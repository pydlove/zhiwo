#!/bin/bash
# 停止指定服务（支持 user-service / admin-service / all）
# 用法: ./service-stop.sh user-service
#       ./service-stop.sh admin-service
#       ./service-stop.sh all

SERVICE=$1

if [ -z "$SERVICE" ]; then
  echo "用法: $0 <user-service|admin-service|all>"
  exit 1
fi

case "$SERVICE" in
  user-service)
    /bin/bash /root/app/zhiwo/user-service/user-service-stop.sh
    ;;
  admin-service)
    /bin/bash /root/app/zhiwo/admin-service/admin-service-stop.sh
    ;;
  all)
    /bin/bash /root/app/zhiwo/stop-all.sh
    ;;
  *)
    echo "未知服务: $SERVICE"
    echo "支持的参数: user-service, admin-service, all"
    exit 1
    ;;
esac
