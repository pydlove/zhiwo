#!/bin/bash
# AI 文本检测服务启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Python 解释器（优先使用 python3）
PYTHON=${PYTHON:-python3}

echo "=== AI 检测服务启动脚本 ==="

# 检查 Python
if ! command -v $PYTHON &> /dev/null; then
    echo "错误: 未找到 $PYTHON，请先安装 Python 3.8+"
    exit 1
fi

PYTHON_VERSION=$($PYTHON --version 2>&1 | awk '{print $2}')
echo "Python 版本: $PYTHON_VERSION"

# 创建虚拟环境（如果不存在）
if [ ! -d "venv" ]; then
    echo "创建虚拟环境..."
    $PYTHON -m venv venv
fi

source venv/bin/activate

# 安装依赖
echo "安装依赖..."
pip install -q --upgrade pip
pip install -q -r requirements.txt

# 预下载模型（如果不存在）
if [ ! -d "models" ]; then
    echo "首次运行，预下载模型..."
    $PYTHON download_model.py
fi

# 启动服务
echo "启动检测服务..."
echo "访问地址: http://127.0.0.1:5000"
echo "健康检查: curl http://127.0.0.1:5000/health"

# 生产环境建议用 nohup 或 systemd 托管
# nohup python detect_service.py > detect.log 2>&1 &
exec $PYTHON detect_service.py "$@"
