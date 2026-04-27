#!/bin/bash
# 管理端后端停止脚本

APP_NAME="admin-backend"
APP_HOME="/root/app/gzh/admin-service"
PID_FILE="$APP_HOME/app.pid"
PORT=8080

function kill_by_port() {
  local pids
  pids=$(lsof -t -i:$PORT 2>/dev/null)
  if [ -n "$pids" ]; then
    echo "通过端口 $PORT 找到残留进程: $pids，正在终止 ..."
    kill $pids 2>/dev/null
    for i in {1..30}; do
      pids=$(lsof -t -i:$PORT 2>/dev/null)
      [ -z "$pids" ] && break
      sleep 1
    done
    pids=$(lsof -t -i:$PORT 2>/dev/null)
    if [ -n "$pids" ]; then
      echo "强制终止残留进程 ..."
      kill -9 $pids 2>/dev/null
    fi
  fi
}

# 1. 先尝试用 PID 文件优雅停止
if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if ps -p "$PID" > /dev/null 2>&1; then
    echo "停止 $APP_NAME (PID: $PID) ..."
    kill "$PID"
    for i in {1..30}; do
      if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "$APP_NAME 已停止"
        rm -f "$PID_FILE"
        exit 0
      fi
      sleep 1
    done
    echo "强制终止 $APP_NAME ..."
    kill -9 "$PID"
    rm -f "$PID_FILE"
    echo "$APP_NAME 已强制停止"
    exit 0
  else
    echo "$APP_NAME 进程已不存在，清理 pid 文件"
    rm -f "$PID_FILE"
  fi
fi

# 2. PID 文件不存在或进程已死，按端口查杀残留
kill_by_port

echo "$APP_NAME 已停止"
