#!/bin/bash
# 公众号创作助手 - 一键停止所有服务（systemctl）

echo "========================================"
echo "停止公众号创作助手服务"
echo "========================================"

echo ""
echo "[1/2] 停止用户端后端 (zhiwo-user)..."
systemctl stop zhiwo-user

echo ""
echo "[2/2] 停止管理端后端 (zhiwo-admin)..."
systemctl stop zhiwo-admin

echo ""
echo "========================================"
echo "所有服务已停止"
echo "========================================"
