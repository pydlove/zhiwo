#!/bin/bash
# 公众号创作助手 - 一键启动所有服务

# 加载环境变量（如果存在）
if [ -f "/root/app/gzh/.env" ]; then
  source /root/app/gzh/.env
fi

echo "========================================"
echo "启动公众号创作助手服务"
echo "========================================"

echo ""
echo "[1/2] 启动用户端后端 (port: 8082)..."
/bin/bash /root/app/gzh/user-service/user-service-start.sh

echo ""
echo "[2/2] 启动管理端后端 (port: 8080)..."
/bin/bash /root/app/gzh/admin-service/admin-service-start.sh

echo ""
echo "========================================"
echo "所有服务启动完成"
echo "========================================"
echo "用户端后端: http://localhost:8082"
echo "管理端后端: http://localhost:8080"
echo ""
echo "前端访问:"
echo "  用户端: https://你的域名 (或 /root/app/web/gzh)"
echo "  管理端: http://服务器IP:38080"
echo "========================================"
