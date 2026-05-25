#!/bin/bash
# 公众号创作助手 - 一键启动所有服务（systemctl）

echo "========================================"
echo "启动公众号创作助手服务"
echo "========================================"

echo ""
echo "[1/2] 启动用户端后端 (gzh-user)..."
systemctl start gzh-user

echo ""
echo "[2/2] 启动管理端后端 (gzh-admin)..."
systemctl start gzh-admin

echo ""
echo "========================================"
echo "所有服务启动完成"
echo "========================================"
echo "用户端后端: http://localhost:8082"
echo "管理端后端: http://localhost:8080"
echo ""
echo "查看状态: systemctl status gzh-user gzh-admin"
echo "========================================"
