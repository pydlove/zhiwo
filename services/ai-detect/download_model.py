#!/usr/bin/env python3
"""预下载模型，避免服务启动时联网下载"""

import os
import sys

# 使用国内镜像加速下载（如需官方源可注释掉）
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")

MODEL_NAME = "Hello-SimpleAI/chatgpt-detector-roberta-chinese"
CACHE_DIR = os.path.join(os.path.dirname(__file__), "models")


def download():
    print(f"正在下载模型: {MODEL_NAME}")
    print(f"缓存目录: {CACHE_DIR}")

    from transformers import AutoTokenizer, AutoModelForSequenceClassification

    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME, cache_dir=CACHE_DIR)
    model = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME, cache_dir=CACHE_DIR)

    # 验证加载
    test_text = "这是一段测试文本。"
    inputs = tokenizer(test_text, return_tensors="pt", truncation=True, max_length=512)
    outputs = model(**inputs)
    print(f"模型加载成功，输出维度: {outputs.logits.shape}")
    print("预下载完成，可以启动服务了。")


if __name__ == "__main__":
    try:
        download()
    except Exception as e:
        print(f"下载失败: {e}")
        sys.exit(1)
