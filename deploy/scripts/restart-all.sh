#!/bin/bash
# 公众号创作助手 - 一键重启所有服务

echo "========================================"
echo "重启公众号创作助手所有服务"
echo "========================================"

echo ""
echo "[1/4] 停止用户端后端..."
/bin/bash /root/app/gzh/user-service/user-service-stop.sh || true

echo ""
echo "[2/4] 停止管理端后端..."
/bin/bash /root/app/gzh/admin-service/admin-service-stop.sh || true

echo ""
echo "等待端口释放..."
sleep 2

# 强制清理残留进程
lsof -t -i:8080 >/dev/null 2>&1 && { echo "清理 admin 残留进程..."; kill -9 $(lsof -t -i:8080) 2>/dev/null; } || true
lsof -t -i:8082 >/dev/null 2>&1 && { echo "清理 user 残留进程..."; kill -9 $(lsof -t -i:8082) 2>/dev/null; } || true

sleep 1

echo ""
echo "[3/4] 启动用户端后端 (port: 8082)..."
/bin/bash /root/app/gzh/user-service/user-service-start.sh

echo ""
echo "[4/4] 启动管理端后端 (port: 8080)..."
/bin/bash /root/app/gzh/admin-service/admin-service-start.sh

echo ""
echo "========================================"
echo "所有服务重启完成"
echo "========================================"
echo "用户端后端: http://localhost:8082"
echo "管理端后端: http://localhost:8080"
echo ""
echo "查看状态: /bin/bash /root/app/gzh/status.sh"
echo "========================================"
