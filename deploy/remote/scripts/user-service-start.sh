#!/bin/bash
# 用户端后端启动脚本
# 部署目录: /root/app/zhiwo/user-service/

APP_NAME="user-backend"
JAR_NAME="user-backend-1.0.0.jar"
APP_HOME="/root/app/zhiwo/user-service"
LOG_FILE="$APP_HOME/app.log"
PID_FILE="$APP_HOME/app.pid"
JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8 -Dprocess.name=zhiwo-user"
SPRING_OPTS="--spring.profiles.active=prod"

# 加载环境变量（如果存在）
if [ -f "/root/app/zhiwo/.env" ]; then
  source /root/app/zhiwo/.env
fi

# 检查是否已运行
if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if ps -p "$PID" > /dev/null 2>&1; then
    echo "$APP_NAME 已经在运行 (PID: $PID)"
    exit 0
  fi
fi

# 检查 Jasypt 密钥（如果配置文件使用了 ENC() 格式）
if [ -z "$JASYPT_ENCRYPTOR_PASSWORD" ]; then
  echo "警告: 未设置 JASYPT_ENCRYPTOR_PASSWORD 环境变量"
  echo "如果 application-prod.yml 中使用了 ENC() 加密密码，启动会失败"
  echo "请执行: export JASYPT_ENCRYPTOR_PASSWORD=你的加密密钥"
fi

cd "$APP_HOME" || exit 1

echo "启动 $APP_NAME ..."
nohup java $JAVA_OPTS -jar "$JAR_NAME" $SPRING_OPTS > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
echo "$APP_NAME 已启动 (PID: $!)"
