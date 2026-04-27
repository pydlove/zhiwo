#!/bin/bash
# 管理端后端一键重启脚本

echo "========================================"
echo "重启管理端后端 (admin-backend)"
echo "========================================"

echo ""
echo "[1/2] 停止服务..."
/bin/bash /root/app/gzh/admin-service/admin-service-stop.sh

echo ""
echo "[2/2] 启动服务..."
/bin/bash /root/app/gzh/admin-service/admin-service-start.sh

echo ""
echo "========================================"
echo "管理端后端重启完成"
echo "========================================"
