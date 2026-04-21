#!/bin/bash
# 管理端后端停止脚本

APP_NAME="admin-backend"
APP_HOME="/root/app/gzh/admin-service"
PID_FILE="$APP_HOME/app.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "$APP_NAME 未运行"
  exit 0
fi

PID=$(cat "$PID_FILE")
if ! ps -p "$PID" > /dev/null 2>&1; then
  echo "$APP_NAME 进程已不存在，清理 pid 文件"
  rm -f "$PID_FILE"
  exit 0
fi

echo "停止 $APP_NAME (PID: $PID) ..."
kill "$PID"

# 等待进程退出
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
