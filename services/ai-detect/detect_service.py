#!/usr/bin/env python3
"""
AI 文本检测服务 (FastAPI)
模型: Hello-SimpleAI/chatgpt-detector-roberta-chinese
懒加载：服务启动后不立即加载模型，收到请求时加载
"""

import os
import time
import warnings
import threading

warnings.filterwarnings("ignore")

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import torch
import torch.nn.functional as F

# ============ 配置 ============
# 使用本地模型目录的绝对路径，完全离线加载
MODEL_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "models"))
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
MAX_LENGTH = 512

# ============ 模型（懒加载） ============
_model = None
_tokenizer = None
_model_lock = threading.Lock()
_model_loaded = False


def _load_model():
    global _model, _tokenizer, _model_loaded
    with _model_lock:
        if _model_loaded:
            return
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 正在加载模型: {MODEL_DIR}")
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 设备: {DEVICE}")

        from transformers import BertTokenizer, BertForSequenceClassification

        _tokenizer = BertTokenizer.from_pretrained(MODEL_DIR)
        _model = BertForSequenceClassification.from_pretrained(MODEL_DIR)
        _model.to(DEVICE)
        _model.eval()
        _model_loaded = True
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 模型加载完成")


# ============ FastAPI 应用 ============
app = FastAPI(title="AI Text Detect Service", version="1.0")


class DetectRequest(BaseModel):
    content: str = Field(..., min_length=10, description="待检测文本")


class DetectResponse(BaseModel):
    score: int
    level: str
    reasons: list
    char_count: int
    word_count: int
    model: str
    elapsed_ms: int


@app.get("/health")
def health():
    status = "ok" if _model_loaded else "loading"
    return {
        "status": status,
        "model_dir": MODEL_DIR,
        "device": DEVICE,
        "model_loaded": _model_loaded
    }


@app.post("/detect", response_model=DetectResponse)
def detect(req: DetectRequest):
    # 懒加载模型
    _load_model()

    text = req.content.strip()
    if len(text) < 10:
        raise HTTPException(status_code=400, detail="文本不能少于10个字")

    start = time.time()

    # Tokenize
    inputs = _tokenizer(
        text,
        return_tensors="pt",
        truncation=True,
        max_length=MAX_LENGTH,
        padding=True,
    )
    inputs = {k: v.to(DEVICE) for k, v in inputs.items()}

    # 推理
    with torch.no_grad():
        outputs = _model(**inputs)
        probs = F.softmax(outputs.logits, dim=-1)

    # 模型输出: [人类概率, AI概率]
    ai_prob = probs[0][1].item()
    ai_score = int(round(ai_prob * 100))

    # 风险等级
    if ai_score >= 70:
        level = "高风险"
    elif ai_score >= 40:
        level = "中风险"
    else:
        level = "低风险"

    # 判定原因
    reasons = []
    if ai_score >= 70:
        reasons.append("模型判断为 AI 生成概率高")
    elif ai_score >= 40:
        reasons.append("模型判断存在 AI 生成特征")
    else:
        reasons.append("未检测到明显 AI 特征")

    elapsed_ms = int((time.time() - start) * 1000)

    return DetectResponse(
        score=ai_score,
        level=level,
        reasons=reasons,
        char_count=len(text),
        word_count=len(text.split()),
        model="Hello-SimpleAI/chatgpt-detector-roberta-chinese",
        elapsed_ms=elapsed_ms,
    )


if __name__ == "__main__":
    import uvicorn

    host = os.environ.get("DETECT_HOST", "127.0.0.1")
    port = int(os.environ.get("DETECT_PORT", "5000"))
    workers = int(os.environ.get("DETECT_WORKERS", "1"))

    uvicorn.run(
        "detect_service:app",
        host=host,
        port=port,
        workers=workers,
        log_level="info",
    )