import asyncio
import json
from datetime import datetime
import pandas as pd
from playwright.async_api import async_playwright

# ============================================================
# 新榜 (NewRank) 爬虫
# 说明：新榜的微信公众号榜单目前已改为图片形式展示，
# 免费页面不再提供结构化账号列表。但抖音榜单仍提供结构化 JSON API。
# 本脚本同时实现：
#   1) 抖音分类榜单结构化数据采集 -> Excel
#   2) 微信公众号榜单元数据采集（榜单名称、图片URL） -> Excel
# ============================================================

BASE_URL = "https://www.newrank.cn"


def save_excel(data, filename_prefix):
    df = pd.DataFrame(data)
    timestamp = datetime.now().strftime("%m%d_%H%M")
    path = f"/Users/panyong/aio_project/小程序/{filename_prefix}_{timestamp}.xlsx"
    if len(data) > 0:
        df.to_excel(path, index=False)
        print(f"已保存 {len(data)} 条数据到: {path}")
    else:
        print(f"无数据，未生成 {path}")
    return path


async def fetch_douyin_date(page):
    res = await page.evaluate(
        """
        async () => {
            const res = await fetch('https://gw.newrank.cn/api/xd/xdnphb/nr/cloud/douyin/new/rank/getRankDateList', {
                method: 'POST',
                headers: {'Content-Type': 'application/json;charset=UTF-8', 'N-Token': '9116298d52d64bbfb2bafa92267f74f2'},
                body: JSON.stringify({key: 'ACCOUNT_ALL_RANK'})
            });
            return await res.json();
        }
        """
    )
    if res and res.get("code") == 2000 and res.get("data") and res["data"].get("dayList"):
        return res["data"]["dayList"][0]["value"]
    return datetime.now().strftime("%Y-%m-%d")


async def fetch_douyin_ranking(page, date, category, start=1, size=50):
    payload = {
        "size": size,
        "start": start,
        "firstType": category,
        "date_type": "days",
        "date": date,
    }
    res = await page.evaluate(
        f"""
        async () => {{
            const res = await fetch('https://gw.newrank.cn/api/xd/xdnphb/nr/cloud/douyin/rank/mainHotAccountAllRankList', {{
                method: 'POST',
                headers: {{'Content-Type': 'application/json;charset=UTF-8', 'N-Token': '9116298d52d64bbfb2bafa92267f74f2'}},
                body: JSON.stringify({json.dumps(payload, ensure_ascii=False)})
            }});
            return await res.json();
        }}
        """
    )
    if res and res.get("code") == 2000 and res.get("data"):
        return res["data"].get("list", []), res["data"].get("count", 0)
    return [], 0


async def scrape_douyin_rankings(page):
    """采集抖音日榜全部分类结构化数据"""
    print("\n=== 开始采集抖音日榜 ===")
    latest_date = await fetch_douyin_date(page)
    print(f"最新榜单日期: {latest_date}")

    results = []

    # 先采全部榜单，提取真实分类名
    all_list, all_count = await fetch_douyin_ranking(page, latest_date, "", size=200)
    print(f"[全部] 获取到 {len(all_list)} 条，总榜人数约 {all_count}")

    categories = sorted({item.get("account_classify_first", "") for item in all_list if item.get("account_classify_first")})
    print(f"从总榜提取到 {len(categories)} 个分类: {', '.join(categories)}")

    # 将空字符串（全部）也加入采集
    categories_to_scrape = [("全部", "")] + [(c, c) for c in categories]

    for cat_name, cat_val in categories_to_scrape:
        accounts, count = await fetch_douyin_ranking(page, latest_date, cat_val, size=50)
        print(f"[{cat_name}] 获取到 {len(accounts)} 条，总榜人数约 {count}")
        for i, item in enumerate(accounts, 1):
            results.append(
                {
                    "排名": i,
                    "博主名/昵称": item.get("nickname", ""),
                    "用户名": item.get("username", ""),
                    "分类": cat_name,
                    "二级分类": item.get("account_classify_second", ""),
                    "日期": latest_date,
                    "平台": "抖音",
                    "新榜指数": item.get("newRankIndex", ""),
                    "点赞数": item.get("digg_count", ""),
                    "评论数": item.get("comment_count", ""),
                    "分享数": item.get("share_count", ""),
                    "粉丝数": item.get("follower_count", ""),
                    "涨粉数": item.get("add_follower_count", ""),
                    "作品数": item.get("aweme_count", ""),
                    "城市": item.get("city", ""),
                    "认证信息": item.get("custom_verify", ""),
                    "头像URL": item.get("avatarUrl", ""),
                }
            )

    return results


async def scrape_weixin_categories(page):
    """采集微信公众号榜单分类元数据（榜单名称、最新期数、图片URL）"""
    print("\n=== 开始采集微信公众号榜单分类 ===")
    results = []

    for type_id in [1, 999]:
        res = await page.evaluate(
            f"""
            async () => {{
                const res = await fetch('/xdnphb/weixinrank/item/search', {{
                    method: 'POST',
                    headers: {{'Content-Type': 'application/x-www-form-urlencoded'}},
                    body: 'type={type_id}&page=0&size=200'
                }});
                return await res.json();
            }}
            """
        )
        if res and res.get("success") and res.get("value", {}).get("datas"):
            categories = res["value"]["datas"]
            label = "地区榜" if type_id == 1 else "行业榜"
            print(f"[type={type_id}] 获取到 {len(categories)} 个{label}分类")
            for cat in categories:
                results.append(
                    {
                        "榜单ID": cat.get("id", ""),
                        "榜单名称": cat.get("display_name", "").replace("</br>", " "),
                        "榜单代码": cat.get("name", ""),
                        "分类类型": label,
                        "最新期数": cat.get("last_period", ""),
                        "最新发布日期": cat.get("last_pub_time", ""),
                        "榜单图片URL": cat.get("img_url", ""),
                        "备注": cat.get("memo", ""),
                        "平台": "公众号",
                    }
                )
        else:
            print(f"[type={type_id}] 无数据")

    # 为前 20 个分类抓取最新一期排名图片 URL
    image_results = []
    for r in results[:20]:
        name = r["榜单代码"]
        period_res = await page.evaluate(
            f"""
            async () => {{
                const res = await fetch('/xdnphb/weixinrank/period/searchByName', {{
                    method: 'POST',
                    headers: {{'Content-Type': 'application/x-www-form-urlencoded'}},
                    body: 'name={name}&page=0&size=1'
                }});
                return await res.json();
            }}
            """
        )
        if period_res and period_res.get("success") and period_res.get("value", {}).get("datas"):
            datas = period_res["value"]["datas"]
            if datas:
                img_paths = datas[0].get("img_path", "").split(";")
                image_results.append(
                    {
                        "榜单名称": r["榜单名称"],
                        "榜单代码": name,
                        "期数": datas[0].get("period", ""),
                        "发布日期": datas[0].get("pub_time", ""),
                        "排名图片URL_1": img_paths[0] if len(img_paths) > 0 else "",
                        "排名图片URL_2": img_paths[1] if len(img_paths) > 1 else "",
                        "平台": "公众号",
                    }
                )

    return results, image_results


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context(viewport={"width": 1920, "height": 1080})
        page = await context.new_page()

        # 1. 抖音榜单需要在 /rankdypublic 页面环境下调用 API
        print("正在打开新榜抖音榜单页...")
        await page.goto(f"{BASE_URL}/rankdypublic", wait_until="networkidle")
        await page.wait_for_timeout(5000)
        douyin_data = await scrape_douyin_rankings(page)
        douyin_path = save_excel(douyin_data, "newrank_douyin")

        # 2. 微信公众号榜单分类
        print("\n正在打开新榜公众号榜单页...")
        await page.goto(f"{BASE_URL}/public/info/rank_detail.html?name=mother-to-child", wait_until="networkidle")
        await page.wait_for_timeout(3000)
        weixin_cats, weixin_images = await scrape_weixin_categories(page)
        weixin_cat_path = save_excel(weixin_cats, "newrank_weixin_categories")
        weixin_img_path = save_excel(weixin_images, "newrank_weixin_rank_images")

        await browser.close()

        print("\n========== 采集完成 ==========")
        print(f"抖音榜单数据: {douyin_path} ({len(douyin_data)} 条)")
        print(f"公众号榜单分类: {weixin_cat_path} ({len(weixin_cats)} 条)")
        print(f"公众号榜单图片: {weixin_img_path} ({len(weixin_images)} 条)")
        print("\n注意：新榜的微信公众号榜单目前已转为图片形式展示，")
        print("免费页面不再提供结构化账号列表。若需公众号具体排名数据，")
        print("可考虑：1) OCR 识别榜单图片；2) 使用新榜付费数据服务；3) 寻找其他数据源。")


if __name__ == "__main__":
    asyncio.run(main())
