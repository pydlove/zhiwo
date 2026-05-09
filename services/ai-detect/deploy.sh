#!/bin/bash
# AI 检测服务部署脚本
# 用法: ./deploy.sh
# 功能: 安装环境、下载模型、启动服务

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  AI 检测服务部署脚本"
echo "=========================================="

# ---------- 配置 ----------
PYTHON=${PYTHON:-python3}
PIP="$SCRIPT_DIR/venv/bin/pip"
PYTHON_VENV="$SCRIPT_DIR/venv/bin/python"

# 使用国内镜像加速
export HF_ENDPOINT="${HF_ENDPOINT:-https://hf-mirror.com}"
export PIP_INDEX_URL="${PIP_INDEX_URL:-https://pypi.tuna.tsinghua.edu.cn/simple}"

MODEL_NAME="Hello-SimpleAI/chatgpt-detector-roberta-chinese"
MODEL_DIR="$SCRIPT_DIR/models"

# ---------- 检查 Python ----------
echo ""
echo "[1/5] 检查 Python 环境..."
if ! command -v $PYTHON &> /dev/null; then
    echo "错误: 未找到 $PYTHON，请先安装 Python 3.8+"
    echo "  CentOS/RHEL: sudo yum install python3"
    echo "  Ubuntu/Debian: sudo apt install python3 python3-venv"
    exit 1
fi

PYTHON_VERSION=$($PYTHON --version 2>&1 | awk '{print $2}')
echo "  Python 版本: $PYTHON_VERSION"

# 检查版本号 >= 3.8
PY_MAJOR=$(echo "$PYTHON_VERSION" | cut -d. -f1)
PY_MINOR=$(echo "$PYTHON_VERSION" | cut -d. -f2)
if [ "$PY_MAJOR" -lt 3 ] || ([ "$PY_MAJOR" -eq 3 ] && [ "$PY_MINOR" -lt 8 ]); then
    echo "错误: Python 版本需要 >= 3.8"
    exit 1
fi

# ---------- 创建虚拟环境 ----------
echo ""
echo "[2/5] 准备虚拟环境..."
if [ ! -d "$SCRIPT_DIR/venv" ]; then
    echo "  创建虚拟环境..."
    $PYTHON -m venv "$SCRIPT_DIR/venv"
fi
source "$SCRIPT_DIR/venv/bin/activate"

# ---------- 安装依赖 ----------
echo ""
echo "[3/5] 安装 Python 依赖..."
$PIP install -q --upgrade pip
$PIP install -q -r requirements.txt
echo "  依赖安装完成"

# ---------- 下载模型 ----------
echo ""
echo "[4/5] 检查并下载模型..."
echo "  模型: $MODEL_NAME"
echo "  镜像: $HF_ENDPOINT"
echo "  本地目录: $MODEL_DIR"

# 检查模型是否已存在（通过检查 pytorch_model.bin 或 model.safetensors）
MODEL_EXISTS=false
if [ -f "$MODEL_DIR/pytorch_model.bin" ] || [ -f "$MODEL_DIR/model.safetensors" ]; then
    MODEL_EXISTS=true
fi

if [ "$MODEL_EXISTS" = true ]; then
    echo "  模型文件已存在，跳过下载"
else
    echo "  正在下载模型（约 400MB，可能需要几分钟）..."
    $PYTHON_VENV -c "
from huggingface_hub import snapshot_download
import os
os.environ['HF_ENDPOINT'] = '${HF_ENDPOINT}'
snapshot_download(
    repo_id='${MODEL_NAME}',
    local_dir='${MODEL_DIR}',
    local_dir_use_symlinks=False,
)
print('Download complete.')
"
    echo "  模型下载完成"
fi

# ---------- 启动服务 ----------
echo ""
echo "[5/5] 启动检测服务..."

# 如果已经有进程在跑，先杀掉
PID=$(lsof -ti:5000 2>/dev/null || true)
if [ -n "$PID" ]; then
    echo "  端口 5000 已被占用 (PID: $PID)，先停止旧进程..."
    kill "$PID" 2>/dev/null || true
    sleep 1
fi

# 用 nohup 后台启动
LOG_FILE="$SCRIPT_DIR/detect.log"
nohup "$PYTHON_VENV" "$SCRIPT_DIR/detect_service.py" > "$LOG_FILE" 2>&1 &
declare NEW_PID=$!
sleep 2

# 检查是否启动成功
if kill -0 "$NEW_PID" 2>/dev/null; then
    echo ""
    echo "=========================================="
    echo "  部署成功!"
    echo "=========================================="
    echo "  服务 PID: $NEW_PID"
    echo "  访问地址: http://127.0.0.1:5000"
    echo "  日志文件: $LOG_FILE"
    echo ""
    echo "  健康检查:"
    echo "    curl http://127.0.0.1:5000/health"
    echo ""
    echo "  管理命令:"
    echo "    查看日志: tail -f $LOG_FILE"
    echo "    停止服务: kill $NEW_PID"
    echo ""
else
    echo ""
    echo "  服务启动失败，查看日志:"
    echo "    tail -n 20 $LOG_FILE"
    exit 1
fi
