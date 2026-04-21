#!/bin/bash
# 公众号创作助手 - 查看服务状态

echo "========================================"
echo "服务状态检查"
echo "========================================"

check_service() {
  local name=$1
  local pid_file=$2
  local port=$3

  echo ""
  echo "[$name]"

  # 检查 PID 文件
  if [ -f "$pid_file" ]; then
    local pid=$(cat "$pid_file")
    if ps -p "$pid" > /dev/null 2>&1; then
      echo "  进程状态: 运行中 (PID: $pid)"
    else
      echo "  进程状态: 已停止 (PID 文件残留: $pid)"
    fi
  else
    echo "  进程状态: 未启动 (无 PID 文件)"
  fi

  # 检查端口监听
  if command -v ss > /dev/null 2>&1; then
    local port_listen=$(ss -tlnp 2>/dev/null | grep ":$port " | head -1)
    if [ -n "$port_listen" ]; then
      echo "  端口监听: $port 正常"
    else
      echo "  端口监听: $port 未监听"
    fi
  elif command -v netstat > /dev/null 2>&1; then
    local port_listen=$(netstat -tlnp 2>/dev/null | grep ":$port " | head -1)
    if [ -n "$port_listen" ]; then
      echo "  端口监听: $port 正常"
    else
      echo "  端口监听: $port 未监听"
    fi
  else
    echo "  端口监听: 无法检查 (ss/netstat 均不可用)"
  fi
}

check_service "用户端后端" "/root/app/gzh/user-service/app.pid" "8082"
check_service "管理端后端" "/root/app/gzh/admin-service/app.pid" "8080"

echo ""
echo "========================================"
