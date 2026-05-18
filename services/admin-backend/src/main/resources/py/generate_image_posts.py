#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 DOCX 文章切分成多张 9:16 贴图（PNG）
用法:
    python generate_image_posts.py <docx_path> <output_dir> <title_id> \
        [--split-mode height|paragraph] \
        [--width 1080] [--height 1920] \
        [--bg-color #ffffff] \
        [--cover-gradient "#4f46e5,#7c3aed"] \
        [--theme morandi-cream|mint-fresh|sunset-blush|midnight|lavender|klein-blue|gradient-ins|newspaper]
输出 JSON:
    {"success": true, "images": ["/uploads/image-posts/.../xxx_0.png", ...]}
"""

import sys
import os
import json
import re
import uuid
import textwrap
import argparse
import subprocess
import urllib.request
import math
from docx import Document
from PIL import Image, ImageDraw, ImageFont

# 自动下载的兜底字体缓存路径
_FONT_CACHE_DIR = os.path.join(os.path.expanduser("~"), ".cache", "image_post_fonts")
_DOWNLOAD_FONT_PATH = os.path.join(_FONT_CACHE_DIR, "NotoSansCJKsc-Regular.otf")
_DOWNLOAD_FONT_URL = (
    "https://github.com/googlefonts/noto-cjk/raw/main/"
    "Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf"
)


# ============================================================
# 小红书风格封面主题配置
# ============================================================
COVER_THEMES = {
    # 1. 莫兰迪奶油 — 高级感，低饱和暖色
    "morandi-cream": {
        "name": "莫兰迪奶油",
        "bg_type": "solid",
        "bg_color": "#f5f0e8",
        "text_color": "#3d3d3d",
        "accent_color": "#c4a882",
        "layout": "center",
        "decorations": ["thin_frame", "corner_dots"],
        "font_size_factor": 1.0,
        "line_height_factor": 1.15,
    },
    # 2. 薄荷清新 — 清爽治愈
    "mint-fresh": {
        "name": "薄荷清新",
        "bg_type": "solid",
        "bg_color": "#e8f5f0",
        "text_color": "#2c5f4a",
        "accent_color": "#7ec4a0",
        "layout": "left",
        "decorations": ["wavy_line", "top_bar"],
        "font_size_factor": 1.0,
        "line_height_factor": 1.2,
    },
    # 3. 落日橘粉 — 温暖治愈
    "sunset-blush": {
        "name": "落日橘粉",
        "bg_type": "solid",
        "bg_color": "#fff0e6",
        "text_color": "#8b4513",
        "accent_color": "#e8927c",
        "layout": "center",
        "decorations": ["sun_rays", "circle_badge"],
        "font_size_factor": 1.05,
        "line_height_factor": 1.15,
    },
    # 4. 深夜高级 — 沉稳大气
    "midnight": {
        "name": "深夜高级",
        "bg_type": "solid",
        "bg_color": "#2d3436",
        "text_color": "#dfe6e9",
        "accent_color": "#fdcb6e",
        "layout": "center",
        "decorations": ["gold_line", "corner_brackets"],
        "font_size_factor": 1.0,
        "line_height_factor": 1.15,
    },
    # 5. 薰衣草紫 — 温柔浪漫
    "lavender": {
        "name": "薰衣草紫",
        "bg_type": "solid",
        "bg_color": "#f3f0ff",
        "text_color": "#5b4b8a",
        "accent_color": "#a29bfe",
        "layout": "center",
        "decorations": ["rounded_frame", "small_dots"],
        "font_size_factor": 1.0,
        "line_height_factor": 1.15,
    },
    # 6. 克莱因蓝 — 艺术前卫
    "klein-blue": {
        "name": "克莱因蓝",
        "bg_type": "solid",
        "bg_color": "#002fa7",
        "text_color": "#ffffff",
        "accent_color": "#ffffff",
        "layout": "center_upper",
        "decorations": ["thick_border", "big_quote"],
        "font_size_factor": 1.1,
        "line_height_factor": 1.1,
    },
    # 7. 渐变ins风 — 时尚梦幻
    "gradient-ins": {
        "name": "渐变ins风",
        "bg_type": "gradient",
        "bg_gradient": ("#ff9a9e", "#fecfef"),
        "text_color": "#ffffff",
        "accent_color": "#ffffff",
        "layout": "center",
        "decorations": ["text_shadow", "soft_circle"],
        "font_size_factor": 1.05,
        "line_height_factor": 1.15,
    },
    # 8. 报纸复古 — 文艺怀旧
    "newspaper": {
        "name": "报纸复古",
        "bg_type": "solid",
        "bg_color": "#f4f1ea",
        "text_color": "#2c2c2c",
        "accent_color": "#8b7355",
        "layout": "left",
        "decorations": ["newspaper_lines", "typewriter_box"],
        "font_size_factor": 0.95,
        "line_height_factor": 1.25,
    },
    # 9. 经典小红书 — 保留原版风格
    "classic-xhs": {
        "name": "经典小红书",
        "bg_type": "solid",
        "bg_color": "#f8f3e0",
        "text_color": "#1a1a1a",
        "accent_color": "#ffb428",
        "layout": "left",
        "decorations": ["quote_marks"],
        "font_size_factor": 1.0,
        "line_height_factor": 1.15,
    },
}


def _load_truetype(path, size):
    """尝试加载字体，兼容 .ttc / .otf / .ttf / .woff / .woff2"""
    try:
        return ImageFont.truetype(path, size, index=0)
    except Exception:
        return ImageFont.truetype(path, size)


def _discover_fonts_via_fclist():
    """通过 fc-list 动态发现系统中的中文字体"""
    try:
        result = subprocess.run(
            ["fc-list", ":lang=zh", "file"],
            capture_output=True, text=True, timeout=5
        )
        if result.returncode != 0:
            return []
        paths = []
        for line in result.stdout.strip().splitlines():
            line = line.strip()
            if line and line.startswith("/"):
                p = line.split(":")[0].strip()
                if p and os.path.exists(p):
                    paths.append(p)
        return paths
    except Exception:
        return []


def _scan_font_dirs():
    """直接扫描常见字体目录，不依赖 fc-list / fontconfig"""
    dirs = [
        "/usr/share/fonts",
        "/usr/local/share/fonts",
        os.path.expanduser("~/.fonts"),
        "/opt/fonts",
    ]
    exts = {".ttf", ".ttc", ".otf", ".woff", ".woff2"}
    found = []
    for d in dirs:
        if not os.path.isdir(d):
            continue
        for root, _dirs, files in os.walk(d):
            for f in files:
                if os.path.splitext(f)[1].lower() in exts:
                    found.append(os.path.join(root, f))
    prioritized = []
    others = []
    for p in found:
        name = os.path.basename(p).lower()
        if any(k in name for k in ("cjk", "noto", "wqy", "zenhei", "hei", "song", "ming")):
            prioritized.append(p)
        else:
            others.append(p)
    return prioritized + others


def _download_fallback_font():
    """自动下载 Noto Sans CJK 作为兜底字体"""
    if os.path.exists(_DOWNLOAD_FONT_PATH):
        return _DOWNLOAD_FONT_PATH
    try:
        os.makedirs(_FONT_CACHE_DIR, exist_ok=True)
        req = urllib.request.Request(
            _DOWNLOAD_FONT_URL,
            headers={"User-Agent": "Mozilla/5.0"}
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            with open(_DOWNLOAD_FONT_PATH, "wb") as f:
                f.write(resp.read())
        return _DOWNLOAD_FONT_PATH
    except Exception:
        return None


# 用户指定的封面字体路径（相对项目根目录，支持 .woff2 / .woff / .ttf / .otf / .ttc）
_CUSTOM_FONT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "..", "..", "..",
    "admin-frontend", "src", "assets", "font"
)

# 全局：通过 --font-family 指定的字体名（文件名或字体名子串）
_FONT_FAMILY = None
# 全局：通过 --body-font-family 指定的正文字体名
_BODY_FONT_FAMILY = None


def _try_load_custom_font(size, purpose="cover"):
    """
    尝试加载用户自定义目录中的字体文件。
    purpose: 'cover' 优先加载粗体/书法/艺术字体；'body' 优先加载常规清晰黑体。
    """
    if not os.path.isdir(_CUSTOM_FONT_DIR):
        return None
    # 排除 woff/woff2：PIL/FreeType 对 Web 字体格式的渲染质量通常不如 ttf/otf
    exts = {".ttc", ".ttf", ".otf"}
    candidates = []
    for root, _dirs, files in os.walk(_CUSTOM_FONT_DIR):
        for f in files:
            if os.path.splitext(f)[1].lower() in exts:
                candidates.append(os.path.join(root, f))
    candidates.sort(key=lambda p: os.path.basename(p))
    if not candidates:
        return None

    # 加载每个字体并获取真实名称，然后按用途打分
    scored = []
    for path in candidates:
        f = os.path.basename(path)
        try:
            font = _load_truetype(path, 40)
            name_tuple = font.getname()
            font_name = " ".join(name_tuple).lower()
            file_name = f.lower()
            combined = font_name + " " + file_name
            s = 0
            is_artistic = any(k in combined for k in ("calligraphy", "brush", "art", "script", "hand", "write"))
            is_calligraphy = any(k in combined for k in ("dao", "li", "mao", "kai", "xingshu", "caoshu", "lishu", "fangyuan"))
            # 如果通过 --font-family / --body-font-family 指定了字体，文件名/字体名/目录名匹配则给最高优先级
            if purpose == "cover" and _FONT_FAMILY:
                target = _FONT_FAMILY.lower()
                dir_name = os.path.basename(os.path.dirname(path)).lower()
                if target in file_name or target in font_name or target in dir_name:
                    s += 1000
            elif purpose == "body" and _BODY_FONT_FAMILY:
                target = _BODY_FONT_FAMILY.lower()
                dir_name = os.path.basename(os.path.dirname(path)).lower()
                if target in file_name or target in font_name or target in dir_name:
                    s += 1000
            # body 未指定正文字体时，回退到封面字体（如果设置了）
            elif purpose == "body" and _FONT_FAMILY and not _BODY_FONT_FAMILY:
                target = _FONT_FAMILY.lower()
                dir_name = os.path.basename(os.path.dirname(path)).lower()
                if target in file_name or target in font_name or target in dir_name:
                    s += 1000

            if purpose == "cover":
                # 封面偏好：书法/艺术体优先，其次是粗黑标题体
                if is_calligraphy:
                    s += 20
                if is_artistic:
                    s += 15
                if any(k in combined for k in ("bold", "heavy", "black", "hei", "simhei", "title")):
                    s += 8
                if any(k in font_name for k in ("noto", "pingfang", "source han", "microsoft yahei")):
                    s += 2  # 系统黑体兜底
                if any(k in combined for k in ("regular", "light", "thin", "mono", "code")) and not is_calligraphy and not is_artistic:
                    s -= 5
            else:  # body
                # 正文偏好：常规清晰黑体，避免书法体
                if is_calligraphy or is_artistic:
                    s -= 10
                if any(k in combined for k in ("regular", "normal", "pingfang", "noto", "sans", "hei", "medium", "book", "light", "thin")):
                    s += 10
                if any(k in combined for k in ("bold", "heavy", "black", "title")):
                    s -= 2
            # 通用：优先 ttf（文件完整性更好），其次 woff
            if file_name.endswith(".ttf"):
                s += 2
            if file_name.endswith(".woff"):
                s += 1
            scored.append((s, f, path, font_name))
        except Exception:
            continue

    scored.sort(key=lambda x: x[0], reverse=True)
    # 打印前3个候选字体供调试
    if scored and (_FONT_FAMILY or _BODY_FONT_FAMILY):
        top3 = scored[:3]
        print(f"[font-debug] purpose={purpose} family={_FONT_FAMILY} body_family={_BODY_FONT_FAMILY} top3: {[(s, f, n) for s, f, _, n in top3]}", file=sys.stderr)
    for _s, _f, path, _name in scored:
        try:
            font = _load_truetype(path, size)
            print(f"[font-debug] selected {purpose} font: {path} (name={_name})", file=sys.stderr)
            return font
        except Exception:
            continue
    print(f"[font-debug] no custom font matched for purpose={purpose}", file=sys.stderr)
    return None


def find_font(size, prefer_bold=False):
    """跨平台查找中文字体，找不到时自动下载兜底字体或抛出明确错误"""
    candidates = []
    if prefer_bold:
        candidates += [
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/PingFangSC-Semibold.otf",
            "/System/Library/Fonts/STHeiti Medium.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc",
            "/usr/share/fonts/google-noto-cjk/NotoSansCJK-Bold.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Bold.otf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        ]
    else:
        candidates += [
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/PingFangSC-Regular.otf",
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/google-noto-cjk/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        ]

    for path in candidates:
        if os.path.exists(path):
            try:
                return _load_truetype(path, size)
            except Exception:
                continue

    for path in _discover_fonts_via_fclist():
        try:
            return _load_truetype(path, size)
        except Exception:
            continue

    for path in _scan_font_dirs():
        try:
            return _load_truetype(path, size)
        except Exception:
            continue

    downloaded = _download_fallback_font()
    if downloaded:
        try:
            return _load_truetype(downloaded, size)
        except Exception:
            pass

    raise RuntimeError(
        "未找到可用的中文字体。请在服务器执行以下命令安装字体：\n"
        "  Ubuntu/Debian: sudo apt-get install -y fonts-noto-cjk\n"
        "  CentOS/RHEL  : sudo yum install -y google-noto-sans-cjk-ttc-fonts\n"
        "  或手动下载   : wget -O /usr/share/fonts/NotoSansCJKsc-Regular.otf "
        "https://github.com/googlefonts/noto-cjk/raw/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf"
    )


def find_cover_font(size):
    """查找封面字体：优先用户自定义粗体/艺术字体，回退系统粗黑体"""
    custom = _try_load_custom_font(size, purpose="cover")
    if custom:
        return custom
    return find_font(size, prefer_bold=True)


def find_body_font(size):
    """查找正文字体：优先用户自定义清晰黑体，回退系统常规黑体"""
    custom = _try_load_custom_font(size, purpose="body")
    if custom:
        return custom
    return find_font(size, prefer_bold=False)


def hex_to_rgb(hex_color):
    hex_color = hex_color.lstrip("#")
    if len(hex_color) == 3:
        hex_color = "".join([c * 2 for c in hex_color])
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))


def draw_gradient_bg(img, draw, width, height, color_start, color_end, direction="vertical"):
    """绘制线性渐变背景到已有图像上"""
    start_rgb = hex_to_rgb(color_start)
    end_rgb = hex_to_rgb(color_end)
    if direction == "vertical":
        for y in range(height):
            ratio = y / max(height - 1, 1)
            r = int(start_rgb[0] + (end_rgb[0] - start_rgb[0]) * ratio)
            g = int(start_rgb[1] + (end_rgb[1] - start_rgb[1]) * ratio)
            b = int(start_rgb[2] + (end_rgb[2] - start_rgb[2]) * ratio)
            draw.line([(0, y), (width, y)], fill=(r, g, b))
    else:
        for x in range(width):
            ratio = x / max(width - 1, 1)
            r = int(start_rgb[0] + (end_rgb[0] - start_rgb[0]) * ratio)
            g = int(start_rgb[1] + (end_rgb[1] - start_rgb[1]) * ratio)
            b = int(start_rgb[2] + (end_rgb[2] - start_rgb[2]) * ratio)
            draw.line([(x, 0), (x, height)], fill=(r, g, b))


def wrap_text(text, font, max_width, draw):
    """按像素宽度自动换行"""
    if not text:
        return []
    lines = []
    for paragraph in text.split("\n"):
        paragraph = paragraph.strip()
        if not paragraph:
            lines.append("")
            continue
        current_line = ""
        for char in paragraph:
            test_line = current_line + char
            bbox = draw.textbbox((0, 0), test_line, font=font)
            if bbox[2] - bbox[0] > max_width:
                if current_line:
                    lines.append(current_line)
                current_line = char
            else:
                current_line = test_line
        if current_line:
            lines.append(current_line)
    return lines


def extract_docx_content(docx_path):
    """提取 DOCX 的标题和段落列表"""
    doc = Document(docx_path)
    paragraphs = []
    for para in doc.paragraphs:
        text = para.text.strip()
        if text:
            paragraphs.append(text)
    title = paragraphs[0] if paragraphs else ""
    body = paragraphs[1:] if len(paragraphs) > 1 else []
    return title, body


# ============================================================
# 装饰绘制函数
# ============================================================

def draw_thin_frame(draw, width, height, margin, color, thickness=2):
    """绘制细线边框"""
    draw.rectangle(
        [(margin, margin), (width - margin, height - margin)],
        outline=color, width=thickness
    )


def draw_corner_dots(draw, width, height, margin, color, radius=6):
    """在四个角绘制圆点装饰"""
    offsets = [
        (margin + 20, margin + 20),
        (width - margin - 20, margin + 20),
        (margin + 20, height - margin - 20),
        (width - margin - 20, height - margin - 20),
    ]
    for x, y in offsets:
        draw.ellipse(
            [(x - radius, y - radius), (x + radius, y + radius)],
            fill=color
        )


def draw_wavy_line(draw, y, width, margin, color, thickness=3, amplitude=8, wavelength=40):
    """绘制波浪线装饰"""
    points = []
    x = margin
    while x <= width - margin:
        px = x
        py = y + int(amplitude * math.sin(2 * math.pi * (x - margin) / wavelength))
        points.append((px, py))
        x += 2
    if len(points) > 1:
        draw.line(points, fill=color, width=thickness)


def draw_top_bar(draw, width, margin, color, height=6):
    """顶部彩色横条"""
    draw.rectangle([(margin, margin), (width - margin, margin + height)], fill=color)


def draw_sun_rays(draw, width, height, color, cx=None, cy=None, radius=120, rays=12):
    """绘制太阳光芒装饰"""
    if cx is None:
        cx = width // 2
    if cy is None:
        cy = height // 4
    for i in range(rays):
        angle = 2 * math.pi * i / rays
        x1 = cx + int(radius * 0.6 * math.cos(angle))
        y1 = cy + int(radius * 0.6 * math.sin(angle))
        x2 = cx + int(radius * math.cos(angle))
        y2 = cy + int(radius * math.sin(angle))
        draw.line([(x1, y1), (x2, y2)], fill=color, width=3)
    draw.ellipse([(cx - 25, cy - 25), (cx + 25, cy + 25)], fill=color)


def draw_circle_badge(draw, x, y, color, text, font):
    """绘制圆形徽章"""
    radius = 35
    draw.ellipse([(x - radius, y - radius), (x + radius, y + radius)], fill=color)
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    draw.text((x - tw // 2, y - th // 2 - 2), text, font=font, fill=(255, 255, 255))


def draw_gold_line(draw, width, height, margin, color, thickness=2):
    """绘制金色装饰线（上下各一条）"""
    y1 = margin + 60
    y2 = height - margin - 60
    draw.line([(margin, y1), (width - margin, y1)], fill=color, width=thickness)
    draw.line([(margin, y2), (width - margin, y2)], fill=color, width=thickness)


def draw_corner_brackets(draw, width, height, margin, color, length=40, thickness=3):
    """绘制四角括号装饰"""
    l = length
    t = thickness
    # 左上
    draw.line([(margin, margin), (margin + l, margin)], fill=color, width=t)
    draw.line([(margin, margin), (margin, margin + l)], fill=color, width=t)
    # 右上
    draw.line([(width - margin - l, margin), (width - margin, margin)], fill=color, width=t)
    draw.line([(width - margin, margin), (width - margin, margin + l)], fill=color, width=t)
    # 左下
    draw.line([(margin, height - margin - l), (margin, height - margin)], fill=color, width=t)
    draw.line([(margin, height - margin), (margin + l, height - margin)], fill=color, width=t)
    # 右下
    draw.line([(width - margin, height - margin - l), (width - margin, height - margin)], fill=color, width=t)
    draw.line([(width - margin - l, height - margin), (width - margin, height - margin)], fill=color, width=t)


def draw_rounded_frame(draw, width, height, margin, color, radius=20, thickness=2):
    """绘制圆角边框"""
    x1, y1 = margin + 10, margin + 10
    x2, y2 = width - margin - 10, height - margin - 10
    draw.rounded_rectangle([(x1, y1), (x2, y2)], radius=radius, outline=color, width=thickness)


def draw_small_dots(draw, width, height, margin, color, spacing=60):
    """在边缘绘制小圆点阵列"""
    for x in range(margin + 30, width - margin, spacing):
        for y in range(margin + 30, height - margin, spacing):
            if (x + y) % (spacing * 2) < spacing:
                draw.ellipse([(x - 3, y - 3), (x + 3, y + 3)], fill=color)


def draw_thick_border(draw, width, height, margin, color, thickness=12):
    """绘制粗边框"""
    draw.rectangle(
        [(margin, margin), (width - margin, height - margin)],
        outline=color, width=thickness
    )


def draw_big_quote(draw, width, height, margin, color, font):
    """绘制大号双引号装饰"""
    quote_text = '"'
    bbox = draw.textbbox((0, 0), quote_text, font=font)
    qw = bbox[2] - bbox[0]
    qh = bbox[3] - bbox[1]
    draw.text((margin, margin + 20), quote_text, font=font, fill=color)
    draw.text((width - margin - qw, height - margin - qh - 20), quote_text, font=font, fill=color)


def draw_text_shadow(draw, text, x, y, font, text_color, shadow_color, offset=3):
    """绘制带阴影的文字"""
    draw.text((x + offset, y + offset), text, font=font, fill=shadow_color)
    draw.text((x, y), text, font=font, fill=text_color)


def draw_soft_circle(draw, width, height, color, cx=None, cy=None, radius=200):
    """绘制柔和的大圆装饰"""
    if cx is None:
        cx = width // 2
    if cy is None:
        cy = height // 3
    # 用半透明效果模拟柔和（PIL 不支持直接 alpha 混合，画实心圆即可）
    draw.ellipse([(cx - radius, cy - radius), (cx + radius, cy + radius)], fill=color)


def draw_newspaper_lines(draw, width, height, margin, color):
    """绘制报纸风格的横线装饰"""
    y_positions = [margin + 40, height - margin - 40]
    for y in y_positions:
        draw.line([(margin, y), (width - margin, y)], fill=color, width=2)
    # 中间分割线
    mid_y = height // 2
    draw.line([(width // 2 - 60, mid_y), (width // 2 + 60, mid_y)], fill=color, width=2)


def draw_typewriter_box(draw, x, y, text, font, text_color, bg_color, padding=16):
    """打字机风格的文字框"""
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    w = tw + padding * 2
    h = th + padding * 2
    draw.rectangle([(x, y), (x + w, y + h)], fill=bg_color)
    draw.text((x + padding, y + padding - 2), text, font=font, fill=text_color)


# ============================================================
# 主题封面渲染
# ============================================================

class ImagePostGenerator:
    def __init__(self, width=1080, height=1920, bg_color="#ffffff",
                 cover_gradient=("#4f46e5", "#7c3aed"), split_mode="height",
                 theme="classic-xhs"):
        self.width = width
        self.height = height
        self.bg_color = bg_color
        self.cover_gradient = cover_gradient
        self.split_mode = split_mode
        self.theme = theme

        # 边距
        self.margin_x = 72
        self.margin_y = 96
        self.content_width = self.width - self.margin_x * 2

        # 字体：封面用艺术/粗体，正文用清晰黑体（仿小红书排版）
        self.font_title = find_cover_font(64)
        self.font_subtitle = find_body_font(32)
        self.font_body = find_body_font(38)
        self.font_page = find_body_font(24)
        self.font_cover_sub = find_body_font(28)
        self.font_cover_title = find_cover_font(88)
        self.font_cover_quote = find_cover_font(140)
        self.font_badge = find_cover_font(24)

        # 行高：小红书风格，正文大行距、段间距明显
        self.line_height_title = 90
        self.line_height_cover_title = 108
        self.line_height_body = 68
        self.paragraph_gap = 56

    def create_cover(self, title, subtitle=""):
        """根据当前主题生成封面"""
        theme_cfg = COVER_THEMES.get(self.theme, COVER_THEMES["classic-xhs"])
        bg_type = theme_cfg.get("bg_type", "solid")

        # 创建画布
        if bg_type == "gradient":
            img = Image.new("RGB", (self.width, self.height), (255, 255, 255))
            draw = ImageDraw.Draw(img)
            grad = theme_cfg.get("bg_gradient", ("#ff9a9e", "#fecfef"))
            draw_gradient_bg(img, draw, self.width, self.height, grad[0], grad[1])
        else:
            bg_color = theme_cfg.get("bg_color", "#f8f3e0")
            bg_rgb = hex_to_rgb(bg_color)
            img = Image.new("RGB", (self.width, self.height), bg_rgb)
            draw = ImageDraw.Draw(img)

        text_color = hex_to_rgb(theme_cfg.get("text_color", "#1a1a1a"))
        accent_color = hex_to_rgb(theme_cfg.get("accent_color", "#ffb428"))
        layout = theme_cfg.get("layout", "left")
        decorations = theme_cfg.get("decorations", [])
        fs_factor = theme_cfg.get("font_size_factor", 1.0)
        lh_factor = theme_cfg.get("line_height_factor", 1.15)

        # 根据主题调整字体大小
        cover_font_size = int(88 * fs_factor)
        cover_line_height = int(108 * lh_factor)
        font_cover = find_cover_font(cover_font_size)

        # 先绘制装饰（在文字下方）
        if "thin_frame" in decorations:
            draw_thin_frame(draw, self.width, self.height, self.margin_x, accent_color, 2)
        if "corner_dots" in decorations:
            draw_corner_dots(draw, self.width, self.height, self.margin_x, accent_color, 6)
        if "top_bar" in decorations:
            draw_top_bar(draw, self.width, self.margin_x, accent_color, 6)
        if "sun_rays" in decorations:
            draw_sun_rays(draw, self.width, self.height, accent_color)
        if "gold_line" in decorations:
            draw_gold_line(draw, self.width, self.height, self.margin_x, accent_color, 2)
        if "corner_brackets" in decorations:
            draw_corner_brackets(draw, self.width, self.height, self.margin_x, accent_color, 40, 3)
        if "rounded_frame" in decorations:
            draw_rounded_frame(draw, self.width, self.height, self.margin_x, accent_color, 20, 2)
        if "small_dots" in decorations:
            draw_small_dots(draw, self.width, self.height, self.margin_x, accent_color, 60)
        if "thick_border" in decorations:
            draw_thick_border(draw, self.width, self.height, self.margin_x, accent_color, 12)
        if "soft_circle" in decorations:
            # 柔和大圆用半透明效果，这里简化为浅色圆
            soft_color = tuple(min(255, c + 40) for c in accent_color)
            draw_soft_circle(draw, self.width, self.height, soft_color)
        if "newspaper_lines" in decorations:
            draw_newspaper_lines(draw, self.width, self.height, self.margin_x, accent_color)
        if "wavy_line" in decorations:
            # 在标题下方绘制波浪线
            pass  # 等计算完标题位置后再画

        # 标题自动换行
        title_lines = wrap_text(title, font_cover, self.content_width, draw)

        # 最多显示行数
        max_lines = 6 if layout != "center_upper" else 4
        title_lines = title_lines[:max_lines]

        # 计算标题总高度
        total_title_height = len(title_lines) * cover_line_height

        # 计算起始 Y 位置
        if layout == "center":
            start_y = (self.height - total_title_height) // 2
        elif layout == "center_upper":
            start_y = self.height // 3 - total_title_height // 2
        elif layout == "left":
            start_y = self.height // 2 - total_title_height // 2
        else:
            start_y = (self.height - total_title_height) // 2

        # 绘制大引号装饰（在文字前面）
        if "big_quote" in decorations or "quote_marks" in decorations:
            quote_font = find_cover_font(120 if self.theme == "klein-blue" else 160)
            draw_big_quote(draw, self.width, self.height, self.margin_x, accent_color, quote_font)

        # 绘制标题文字
        y = start_y
        for i, line in enumerate(title_lines):
            if layout in ("center", "center_upper"):
                bbox = draw.textbbox((0, 0), line, font=font_cover)
                line_w = bbox[2] - bbox[0]
                x = (self.width - line_w) // 2
            else:
                x = self.margin_x

            # 渐变ins风添加文字阴影
            if "text_shadow" in decorations:
                shadow_color = tuple(max(0, c - 60) for c in text_color)
                draw_text_shadow(draw, line, x, y, font_cover, text_color, shadow_color, 3)
            else:
                draw.text((x, y), line, font=font_cover, fill=text_color)

            y += cover_line_height

        # 波浪线在标题下方
        if "wavy_line" in decorations and title_lines:
            wave_y = y + 30
            draw_wavy_line(draw, wave_y, self.width, self.margin_x, accent_color, 3, 8, 40)

        # 圆形徽章
        if "circle_badge" in decorations:
            badge_x = self.width - self.margin_x - 50
            badge_y = self.margin_y + 50
            draw_circle_badge(draw, badge_x, badge_y, accent_color, "HOT", self.font_badge)

        # 打字机风格小标签
        if "typewriter_box" in decorations:
            label = "NOTE"
            draw_typewriter_box(draw, self.margin_x + 10, start_y - 60,
                                label, self.font_badge, text_color,
                                hex_to_rgb(theme_cfg.get("bg_color", "#f4f1ea")), 12)

        return img

    def create_content_page(self, blocks, page_num, total_pages):
        """生成一页内容——仿小红书风格：清晰黑体、大行距、深灰文字"""
        img = Image.new("RGB", (self.width, self.height), hex_to_rgb(self.bg_color))
        draw = ImageDraw.Draw(img)

        # 小红书正文色：深灰而非纯黑，更柔和
        body_text_color = (45, 45, 45)
        # 页码色：浅灰
        page_color = (160, 160, 160)

        y = self.margin_y
        for block in blocks:
            if not block:
                y += self.paragraph_gap
                continue
            lines = wrap_text(block, self.font_body, self.content_width, draw)
            for line in lines:
                draw.text((self.margin_x, y), line, font=self.font_body,
                          fill=body_text_color)
                y += self.line_height_body
            y += self.paragraph_gap

        # 页码
        page_text = f"{page_num}/{total_pages}"
        bbox = draw.textbbox((0, 0), page_text, font=self.font_page)
        text_w = bbox[2] - bbox[0]
        draw.text(((self.width - text_w) // 2, self.height - 80),
                  page_text, font=self.font_page, fill=page_color)

        return img

    def generate(self, docx_path, output_dir, title_id, title="", subtitle=""):
        """主入口：生成全部贴图"""
        docx_title, body = extract_docx_content(docx_path)
        if not title:
            title = docx_title if docx_title else "文章"

        os.makedirs(output_dir, exist_ok=True)

        for f in os.listdir(output_dir):
            if f.endswith(".png"):
                os.remove(os.path.join(output_dir, f))

        cover = self.create_cover(title, subtitle)
        pages = [cover]

        if self.split_mode == "paragraph":
            pages += self._split_by_paragraph(body)
        else:
            pages += self._split_by_height(body)

        total = len(pages)
        image_paths = []
        for idx, page_img in enumerate(pages):
            filename = f"{idx + 1}-{title_id}.png"
            path = os.path.join(output_dir, filename)
            page_img.save(path, "PNG")
            rel_path = "/uploads/image-posts/" + os.path.basename(output_dir) + "/" + filename
            image_paths.append(rel_path)

        return image_paths

    def _split_by_height(self, body):
        """按累积高度切分"""
        pages = []
        current_blocks = []
        current_height = 0
        max_content_height = self.height - self.margin_y * 2 - 100

        test_img = Image.new("RGB", (1, 1))
        test_draw = ImageDraw.Draw(test_img)
        for block in body:
            lines = wrap_text(block, self.font_body, self.content_width, test_draw)
            block_height = len(lines) * self.line_height_body + self.paragraph_gap
            if current_height + block_height > max_content_height and current_blocks:
                pages.append(current_blocks)
                current_blocks = [block]
                current_height = block_height
            else:
                current_blocks.append(block)
                current_height += block_height

        if current_blocks:
            pages.append(current_blocks)

        total = len(pages) + 1
        return [self.create_content_page(blocks, i + 1, total)
                for i, blocks in enumerate(pages)]

    def _split_by_paragraph(self, body):
        """按段落切分，不截断段落"""
        pages = []
        current_blocks = []
        current_height = 0
        max_content_height = self.height - self.margin_y * 2 - 100

        test_img = Image.new("RGB", (1, 1))
        test_draw = ImageDraw.Draw(test_img)

        for block in body:
            lines = wrap_text(block, self.font_body, self.content_width, test_draw)
            block_height = len(lines) * self.line_height_body + self.paragraph_gap

            if block_height > max_content_height:
                if current_blocks:
                    pages.append(current_blocks)
                    current_blocks = []
                    current_height = 0
                line_batches = []
                batch = []
                batch_height = 0
                for line in lines:
                    if batch_height + self.line_height_body > max_content_height:
                        line_batches.append("\n".join(batch))
                        batch = [line]
                        batch_height = self.line_height_body
                    else:
                        batch.append(line)
                        batch_height += self.line_height_body
                if batch:
                    line_batches.append("\n".join(batch))
                for lb in line_batches:
                    pages.append([lb])
                continue

            if current_height + block_height > max_content_height and current_blocks:
                pages.append(current_blocks)
                current_blocks = [block]
                current_height = block_height
            else:
                current_blocks.append(block)
                current_height += block_height

        if current_blocks:
            pages.append(current_blocks)

        total = len(pages) + 1
        return [self.create_content_page(blocks, i + 1, total)
                for i, blocks in enumerate(pages)]


def main():
    parser = argparse.ArgumentParser(description="Generate 9:16 image posts from DOCX")
    parser.add_argument("docx_path", help="Path to input DOCX file")
    parser.add_argument("output_dir", help="Directory to save PNG files")
    parser.add_argument("title_id", help="Title ID for file naming")
    parser.add_argument("--split-mode", default="height", choices=["height", "paragraph"])
    parser.add_argument("--width", type=int, default=1080)
    parser.add_argument("--height", type=int, default=1920)
    parser.add_argument("--bg-color", default="#ffffff")
    parser.add_argument("--cover-gradient", default="#f8f3e0")
    parser.add_argument("--subtitle", default="")
    parser.add_argument("--title", default="", help="封面标题（优先使用，不传入则从 DOCX 提取）")
    parser.add_argument("--theme", default="classic-xhs",
                        choices=list(COVER_THEMES.keys()),
                        help="封面主题风格")
    parser.add_argument("--font-dir", default="",
                        help="自定义字体目录绝对路径（覆盖默认相对路径）")
    parser.add_argument("--font-family", default="",
                        help="指定封面/标题字体名（文件名或字体名子串，如 'NotoSansSC-Bold' 或 '阿里妈妈方圆体'）")
    parser.add_argument("--body-font-family", default="",
                        help="指定正文字体名（文件名或字体名子串，如 'NotoSansSC-Regular'）")

    args = parser.parse_args()

    # 如果传入自定义字体目录，覆盖默认值（解决 JAR 部署时 __file__ 为临时路径的问题）
    if args.font_dir:
        global _CUSTOM_FONT_DIR
        _CUSTOM_FONT_DIR = args.font_dir

    # 设置全局字体偏好
    if args.font_family:
        global _FONT_FAMILY
        _FONT_FAMILY = args.font_family
    if args.body_font_family:
        global _BODY_FONT_FAMILY
        _BODY_FONT_FAMILY = args.body_font_family

    if not os.path.exists(args.docx_path):
        print(json.dumps({"success": False, "error": "DOCX file not found"}))
        sys.exit(1)

    try:
        gradient = tuple(args.cover_gradient.split(","))
        if len(gradient) == 0:
            gradient = ("#f8f3e0",)

        gen = ImagePostGenerator(
            width=args.width,
            height=args.height,
            bg_color=args.bg_color,
            cover_gradient=gradient,
            split_mode=args.split_mode,
            theme=args.theme,
        )
        images = gen.generate(args.docx_path, args.output_dir, args.title_id,
                              title=args.title, subtitle=args.subtitle)
        print(json.dumps({"success": True, "images": images}, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({"success": False, "error": str(e)}, ensure_ascii=False))
        sys.exit(1)


if __name__ == "__main__":
    main()
