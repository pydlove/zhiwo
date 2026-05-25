#!/bin/bash
# 公众号创作助手 - 一键重启所有服务（systemctl）

echo "========================================"
echo "重启公众号创作助手所有服务"
echo "========================================"

echo ""
echo "[1/2] 重启用户端后端 (gzh-user)..."
systemctl restart gzh-user

echo ""
echo "[2/2] 重启管理端后端 (gzh-admin)..."
systemctl restart gzh-admin

echo ""
echo "========================================"
echo "所有服务重启完成"
echo "========================================"
echo "用户端后端: http://localhost:8082"
echo "管理端后端: http://localhost:8080"
echo ""
echo "查看状态: systemctl status gzh-user gzh-admin"
echo "========================================"
