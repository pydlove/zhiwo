#!/bin/bash
# 公众号创作助手 - 一键停止所有服务

echo "========================================"
echo "停止公众号创作助手服务"
echo "========================================"

echo ""
echo "[1/2] 停止用户端后端..."
/bin/bash /root/app/gzh/user-service/user-service-stop.sh || true

echo ""
echo "[2/2] 停止管理端后端..."
/bin/bash /root/app/gzh/admin-service/admin-service-stop.sh || true

echo ""
echo "========================================"
echo "所有服务已停止"
echo "========================================"
