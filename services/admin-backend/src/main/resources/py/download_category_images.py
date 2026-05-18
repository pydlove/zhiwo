#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量下载 16:9 分类配图脚本

用法示例:
    python download_category_images.py ./article_images --count 10
    python download_category_images.py ./article_images --count 10 --source baidu

支持来源:
    picsum   - 稳定随机图 (默认)
    unsplash - 按主题关键词获取 (可能受网络限制)
    mixed    - 先尝试 unsplash，失败则回退 picsum
    bing     - 必应每日壁纸 (国内CDN快，1920x1080高清)
    baidu    - 百度图片搜索 (中文内容，国内速度快)
"""

import os
import sys
import json
import argparse
import time
import requests
import urllib3
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Tuple, Dict, Optional

# 关闭 SSL 警告 (部分国内站点证书环境特殊)
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# ========================= 配置 =========================

# 分类与对应搜索关键词 (中文更贴合国内图库)
CATEGORIES = {
    "情感故事": ["情感 爱情 唯美", "浪漫 情侣 夕阳", "温暖 治愈 故事"],
    "养身健康": ["养生 健康 运动", "瑜伽 健身 自然", "健康饮食 绿色"],
    "职场干货": ["职场 办公 商务", "办公室 工作 会议", "城市 白领 职业"],
    "历史文化": ["中国古建筑 历史", "传统文化 书法", "故宫 博物馆 文物"],
    "旅行攻略": ["中国旅游 风景", "山水风景 自然", "古镇 旅行 美景"],
    "家居生活": ["家居 温馨 室内", "北欧风 客厅", "厨房 生活 装修"],
    "心理健康": ["心理健康 治愈", "冥想 放松 自然", "心理咨询 平衡"],
    "娱乐八卦": ["娱乐 明星 红毯", "电影 影院 银幕", "演唱会 舞台 灯光"],
    "科技数码": ["科技 数码 中国", "智能手机 电脑", "人工智能 未来科技"],
}

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    ),
    "Referer": "https://image.baidu.com/",
    "Accept": "application/json, text/plain, */*",
}

# ========================= 下载核心 =========================

def fetch_image(url: str, save_path: str, timeout: int = 40, verify: bool = True) -> Tuple[bool, str]:
    """下载单张图片，返回 (是否成功, 错误信息)"""
    try:
        resp = requests.get(url, headers=HEADERS, timeout=timeout, allow_redirects=True, verify=verify)
        if resp.status_code == 200:
            content_type = resp.headers.get("Content-Type", "")
            if "image" not in content_type:
                return False, f"bad content-type: {content_type}"
            with open(save_path, "wb") as f:
                f.write(resp.content)
            return True, "ok"
        return False, f"http {resp.status_code}"
    except requests.exceptions.Timeout:
        return False, "timeout"
    except Exception as e:
        return False, str(e)


def get_picsum_url(seed: str, width: int = 1920, height: int = 1080) -> str:
    return f"https://picsum.photos/seed/{seed}/{width}/{height}"


def get_unsplash_url(keywords: str) -> str:
    return f"https://source.unsplash.com/1920x1080/?{keywords}"


def fetch_bing_pool(max_images: int = 200) -> List[str]:
    """
    从必应国内 API 抓取每日壁纸 URL 列表。
    返回: [url1, url2, ...]
    """
    pool = []
    batch = 8
    idx = 0
    while len(pool) < max_images:
        api_url = (
            "https://cn.bing.com/HPImageArchive.aspx"
            f"?format=js&idx={idx}&n={batch}"
        )
        try:
            resp = requests.get(api_url, headers=HEADERS, timeout=20)
            data = resp.json()
            images = data.get("images", [])
            if not images:
                break
            for img in images:
                urlbase = img.get("urlbase")
                if urlbase:
                    full_url = f"https://cn.bing.com{urlbase}_1920x1080.jpg"
                    pool.append(full_url)
            idx += batch
        except Exception as e:
            print(f"   必应接口获取失败 (idx={idx}): {e}")
            break
    return pool


def fetch_baidu_images(keyword: str, need_count: int = 30) -> List[Tuple[str, int, int]]:
    """
    从百度图片搜索接口抓取图片。
    返回: [(url, width, height), ...]
    优先保留宽高比接近 16:9 的图片。
    """
    results = []
    pn = 0
    rn = 30  # 每页数量
    max_pages = 5  # 最多翻 5 页，避免请求过多

    while len(results) < need_count and max_pages > 0:
        max_pages -= 1
        api_url = "https://image.baidu.com/search/acjson"
        params = {
            "tn": "resultjson_com",
            "ipn": "rj",
            "word": keyword,
            "pn": pn,
            "rn": rn,
        }
        try:
            resp = requests.get(
                api_url, params=params, headers=HEADERS, timeout=20, verify=False
            )
            # 百度返回的 JSON 偶尔包含非法控制字符，需要宽容解析
            data = json.loads(resp.text, strict=False)
            items = data.get("data", [])
            if not items:
                break

            for item in items:
                if not item:
                    continue
                img_url = item.get("middleURL") or item.get("thumbURL")
                width = int(item.get("width", 0) or 0)
                height = int(item.get("height", 0) or 0)

                if not img_url or width <= 0 or height <= 0:
                    continue

                ratio = width / height
                # 筛选接近 16:9 (1.777) 的图片，允许一定浮动
                if 1.4 <= ratio <= 2.4:
                    results.append((img_url, width, height))

            pn += rn
            time.sleep(0.3)  # 轻微延迟，礼貌爬取
        except Exception as e:
            print(f"   百度图片接口异常 (keyword={keyword}, pn={pn}): {e}")
            break

    return results


def get_start_index(cat_dir: str, source: str) -> int:
    """扫描目录，返回该来源下已有图片的最大序号，没有则返回 0。"""
    if not os.path.exists(cat_dir):
        return 0
    prefix = f"{source}-"
    max_idx = 0
    for name in os.listdir(cat_dir):
        if name.startswith(prefix) and name.endswith(".jpg"):
            try:
                num = int(name[len(prefix):-4])
                max_idx = max(max_idx, num)
            except ValueError:
                continue
    return max_idx


def download_one(
    category: str,
    idx: int,
    keywords_list: List[str],
    output_dir: str,
    source: str,
    bing_url: Optional[str] = None,
    baidu_url: Optional[str] = None,
) -> Tuple[str, int, bool, str]:
    """
    下载单张图片。
    返回: (分类, 序号, 是否成功, 来源/错误)
    """
    cat_dir = os.path.join(output_dir, category)
    os.makedirs(cat_dir, exist_ok=True)
    # 文件名格式: 来源-序号.jpg  例如 baidu-001.jpg
    filename = f"{source}-{idx:03d}.jpg"
    save_path = os.path.join(cat_dir, filename)

    if os.path.exists(save_path) and os.path.getsize(save_path) > 1024:
        return category, idx, True, "skipped"

    # 用 (idx-1) 取模关键词，因为 idx 是从 1 开始递增的
    keywords = keywords_list[(idx - 1) % len(keywords_list)]

    # 1) 尝试 baidu (国内中文内容)
    if source == "baidu" and baidu_url:
        # 百度图片的 CDN 偶尔也需要 verify=False
        ok, info = fetch_image(baidu_url, save_path, verify=False)
        if ok:
            return category, idx, True, "baidu"
        # 失败则继续 fallback

    # 2) 尝试 bing (如果模式为 bing 且提供了 URL)
    if source == "bing" and bing_url:
        ok, info = fetch_image(bing_url, save_path)
        if ok:
            return category, idx, True, "bing"

    # 3) 尝试 unsplash (如果模式允许)
    if source in ("unsplash", "mixed"):
        url = get_unsplash_url(keywords)
        ok, info = fetch_image(url, save_path)
        if ok:
            return category, idx, True, "unsplash"
        if source == "unsplash":
            return category, idx, False, info

    # 4) 回退到 picsum
    seed = f"{category[0:2]}_{keywords.replace(',', '').replace(' ', '')}_{idx}_{int(time.time()) % 10000}"
    url = get_picsum_url(seed)
    ok, info = fetch_image(url, save_path)
    if ok:
        return category, idx, True, "picsum"
    return category, idx, False, info


# ========================= 主流程 =========================

def main():
    parser = argparse.ArgumentParser(
        description="批量下载 16:9 公众号分类配图",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python download_category_images.py ./my_images
  python download_category_images.py ./my_images --count 10 --source baidu
  python download_category_images.py ./my_images --count 15 --source mixed
  python download_category_images.py ./my_images --count 10 --source bing
        """,
    )
    parser.add_argument("output_dir", help="图片保存根目录")
    parser.add_argument(
        "--count", type=int, default=9, help="每个分类下载多少张 (默认 9)"
    )
    parser.add_argument(
        "--source",
        choices=["picsum", "unsplash", "mixed", "bing", "baidu"],
        default="picsum",
        help="图片来源: picsum(稳定随机), unsplash(按主题), mixed(先unsplash后picsum兜底), bing(必应壁纸), baidu(百度图片,中文内容). 默认 picsum",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=6,
        help="并发下载数 (默认 6，baidu/bing 建议不超过 6)",
    )
    parser.add_argument(
        "--category",
        type=str,
        default=None,
        help="指定单个分类名称（用于目录名），传入后将只下载该分类",
    )
    parser.add_argument(
        "--keyword",
        type=str,
        default=None,
        help="指定单个分类的搜索关键词，配合 --category 使用",
    )
    args = parser.parse_args()

    output_dir = os.path.abspath(args.output_dir)
    os.makedirs(output_dir, exist_ok=True)

    # 确定要处理的分类列表
    if args.category and args.keyword:
        categories_to_process = {args.category: [args.keyword]}
        is_single_mode = True
    else:
        categories_to_process = CATEGORIES
        is_single_mode = False

    print("=" * 50)
    print(f"目标目录 : {output_dir}")
    print(f"下载数量 : 每类 {args.count} 张")
    print(f"图片来源 : {args.source}")
    print(f"并发数   : {args.workers}")
    if is_single_mode:
        print(f"指定分类 : {args.category}")
        print(f"搜索关键词: {args.keyword}")
    print("=" * 50)

    # 准备 bing 壁纸池
    bing_pool = []
    if args.source == "bing":
        total_needed = len(categories_to_process) * args.count
        print("正在从必应国内接口获取壁纸列表...")
        bing_pool = fetch_bing_pool(max_images=total_needed + 10)
        print(f"获取到 {len(bing_pool)} 张必应壁纸")
        if len(bing_pool) < total_needed:
            print(f"壁纸数量不足，缺额将自动回退到 picsum")
        print("-" * 50)

    # 准备 baidu 图片池 (按分类抓取)
    baidu_pool_map: Dict[str, List[str]] = {}
    if args.source == "baidu":
        print("正在从百度图片接口按分类获取图片列表...")
        for category, keywords_list in categories_to_process.items():
            # 用每个分类的第一个关键词去搜
            keyword = keywords_list[0]
            print(f"   搜索 [{category}] -> '{keyword}' ...")
            images = fetch_baidu_images(keyword, need_count=args.count + 5)
            baidu_pool_map[category] = [url for url, w, h in images]
            print(f"   获取到 {len(baidu_pool_map[category])} 张合适尺寸的图片")
            time.sleep(0.5)
        print("-" * 50)

    # 准备任务列表
    tasks = []
    bing_idx = 0
    for category, keywords in categories_to_process.items():
        cat_dir = os.path.join(output_dir, category)
        # 计算该分类已有图片的起始序号，保证多次运行不重复
        start_idx = get_start_index(cat_dir, args.source)
        baidu_pool = baidu_pool_map.get(category, [])
        for i in range(1, args.count + 1):
            idx = start_idx + i
            bing_url = bing_pool[bing_idx] if bing_idx < len(bing_pool) else None
            baidu_url = baidu_pool[i - 1] if (i - 1) < len(baidu_pool) else None
            tasks.append((category, idx, keywords, output_dir, args.source, bing_url, baidu_url))
            bing_idx += 1

    success_map = {cat: 0 for cat in categories_to_process}
    fail_map = {cat: 0 for cat in categories_to_process}
    source_stats = {"picsum": 0, "unsplash": 0, "bing": 0, "baidu": 0, "skipped": 0}

    # 并发下载
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        future_to_task = {
            executor.submit(download_one, *task): task for task in tasks
        }
        completed = 0
        total = len(tasks)

        for future in as_completed(future_to_task):
            completed += 1
            category, idx, ok, info = future.result()
            if ok:
                success_map[category] += 1
                source_stats[info] = source_stats.get(info, 0) + 1
            else:
                fail_map[category] += 1
                print(f"   ⚠️  [{category}] {args.source}-{idx:03d}.jpg 失败: {info}")

            if completed % max(1, total // 10) == 0 or completed == total:
                print(f"   进度 {completed}/{total} ...")

    # ========================= 汇总 =========================
    print("\n" + "=" * 50)
    print("下载汇总")
    print("-" * 50)
    for category in categories_to_process:
        s = success_map[category]
        f = fail_map[category]
        status = "✅" if f == 0 else "⚠️"
        print(f"{status} {category:10s} | 成功 {s:2d} | 失败 {f:2d}")

    print("-" * 50)
    total_success = sum(success_map.values())
    total_fail = sum(fail_map.values())
    print(f"总计: 成功 {total_success} 张, 失败 {total_fail} 张")
    stats_str = ", ".join(
        f"{k}={v}" for k, v in source_stats.items() if v > 0
    )
    print(f"来源分布: {stats_str}")
    print("=" * 50)

    if total_fail > 0:
        print("\n提示: 如有失败，可重新运行相同命令，已成功的会自动跳过。")
        sys.exit(1)


if __name__ == "__main__":
    main()
