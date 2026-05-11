#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量替换 Word 文档中的句号和分号

功能：
1. 遍历指定目录下的所有 .docx 文件
2. 将句号（。）和分号（；）替换为逗号（，）
3. 例外：如果句号/分号后面紧跟换行符（\n）或位于段落末尾，则保留
"""

import os
import sys
import argparse
from docx import Document


def replace_periods(text):
    """
    将文本中的句号（。）和分号（；）替换为逗号（，）。
    保留后面紧跟换行符（\n）或位于文本末尾的句号/分号。
    """
    result = []
    for i, char in enumerate(text):
        if char == '。' or char == '；':
            # 检查下一个字符是否是换行符，或者是否到达文本末尾
            if i + 1 >= len(text) or text[i + 1] == '\n':
                result.append(char)
            else:
                result.append('，')
        else:
            result.append(char)
    return ''.join(result)


def process_paragraph(paragraph):
    """处理单个段落"""
    original_text = paragraph.text
    if not original_text:
        return

    new_text = replace_periods(original_text)
    if new_text == original_text:
        return

    # 如果段落只有一个 run，直接修改以保持格式
    if len(paragraph.runs) == 1:
        paragraph.runs[0].text = new_text
        return

    # 多个 runs：清空后重建文本
    # 注意：这会丢失 run 级别的格式（如粗体、颜色），但保留段落样式
    paragraph.clear()
    paragraph.add_run(new_text)


def process_document(input_path, output_path):
    """处理单个文档"""
    try:
        doc = Document(input_path)

        # 处理正文段落
        for paragraph in doc.paragraphs:
            process_paragraph(paragraph)

        # 处理表格内的段落
        for table in doc.tables:
            for row in table.rows:
                for cell in row.cells:
                    for paragraph in cell.paragraphs:
                        process_paragraph(paragraph)

        # 处理页眉页脚
        for section in doc.sections:
            for header in [section.header, section.first_page_header, section.even_page_header]:
                if header:
                    for paragraph in header.paragraphs:
                        process_paragraph(paragraph)
            for footer in [section.footer, section.first_page_footer, section.even_page_footer]:
                if footer:
                    for paragraph in footer.paragraphs:
                        process_paragraph(paragraph)

        doc.save(output_path)
        return True

    except Exception as e:
        print(f"  错误: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(
        description='批量替换 Word 文档中的句号（保留换行前的句号）'
    )
    parser.add_argument('directory', help='目标目录路径')
    parser.add_argument(
        '-o', '--output',
        help='输出目录（不指定则覆盖原文件）',
        default=None
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='试运行，只显示会处理的文件而不实际修改'
    )

    args = parser.parse_args()

    target_dir = os.path.abspath(args.directory)
    if not os.path.isdir(target_dir):
        print(f"错误: '{target_dir}' 不是有效的目录")
        sys.exit(1)

    output_dir = None
    if args.output:
        output_dir = os.path.abspath(args.output)
        os.makedirs(output_dir, exist_ok=True)

    # 收集文件
    docx_files = []
    doc_files = []

    for root, dirs, files in os.walk(target_dir):
        for file in files:
            file_path = os.path.join(root, file)
            if file.lower().endswith('.docx'):
                docx_files.append(file_path)
            elif file.lower().endswith('.doc'):
                doc_files.append(file_path)

    # 处理 .docx 文件
    processed = 0
    failed = 0
    skipped = 0

    print(f"\n找到 {len(docx_files)} 个 .docx 文件")
    if doc_files:
        print(f"注意: 跳过 {len(doc_files)} 个 .doc 文件（请先转换为 .docx）")

    for file_path in docx_files:
        rel_path = os.path.relpath(file_path, target_dir)

        if output_dir:
            # 保持目录结构
            rel_dir = os.path.dirname(rel_path)
            out_subdir = os.path.join(output_dir, rel_dir)
            os.makedirs(out_subdir, exist_ok=True)
            out_path = os.path.join(output_dir, rel_path)
        else:
            out_path = file_path

        if args.dry_run:
            print(f"[试运行] 将处理: {rel_path}")
            processed += 1
            continue

        print(f"处理中: {rel_path} ...", end=' ')
        if process_document(file_path, out_path):
            print("完成")
            processed += 1
        else:
            failed += 1

    print(f"\n{'='*50}")
    print(f"处理完成!")
    print(f"  成功: {processed}")
    if failed:
        print(f"  失败: {failed}")
    if doc_files:
        print(f"  跳过 .doc: {len(doc_files)} (请转换为 .docx 后处理)")
    print(f"{'='*50}")


if __name__ == '__main__':
    main()
