#!/bin/bash
# 公众号创作助手 - 查看服务状态（systemctl）

echo "========================================"
echo "服务状态检查"
echo "========================================"

echo ""
echo "[systemctl 状态]"
systemctl status gzh-user --no-pager

echo ""
echo "----------------------------------------"

echo ""
systemctl status gzh-admin --no-pager

echo ""
echo "========================================"
