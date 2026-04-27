#!/bin/bash
# 用户端后端一键重启脚本

echo "========================================"
echo "重启用户端后端 (user-backend)"
echo "========================================"

echo ""
echo "[1/2] 停止服务..."
/bin/bash /root/app/gzh/user-service/user-service-stop.sh

echo ""
echo "[2/2] 启动服务..."
/bin/bash /root/app/gzh/user-service/user-service-start.sh

echo ""
echo "========================================"
echo "用户端后端重启完成"
echo "========================================"
