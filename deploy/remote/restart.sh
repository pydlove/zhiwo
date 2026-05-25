#!/bin/bash
set -e

# ===================================================================
# 公众号创作助手 - 快速重启脚本（不重新构建，只重启服务和刷新前端）
# ===================================================================

# ============ 配置区（请按实际情况修改） ============
SERVER_IP="你的服务器IP"
SERVER_USER="root"
SERVER_PASSWORD="你的服务器密码"
SSH_KEY_PATH=""
# ===================================================

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$DEPLOY_DIR/../.." && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

function log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
function log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

if [ -n "$SSH_KEY_PATH" ] && [ -f "$SSH_KEY_PATH" ]; then
  SSH_CMD="ssh -i $SSH_KEY_PATH -o StrictHostKeyChecking=no"
  SCP_CMD="scp -i $SSH_KEY_PATH -o StrictHostKeyChecking=no"
else
  if ! command -v sshpass &> /dev/null; then
    echo -e "${RED}[ERROR]${NC} 未安装 sshpass，请执行: brew install sshpass"
    exit 1
  fi
  SSH_CMD="sshpass -p '$SERVER_PASSWORD' ssh -o StrictHostKeyChecking=no"
  SCP_CMD="sshpass -p '$SERVER_PASSWORD' scp -o StrictHostKeyChecking=no"
fi

REMOTE_HOST="$SERVER_USER@$SERVER_IP"

log_info "重新上传前端文件..."
eval "$SCP_CMD -r $PROJECT_DIR/frontend/dist/* $REMOTE_HOST:/root/app/zhiwo/user-frontend/"
eval "$SCP_CMD -r $PROJECT_DIR/admin/dist/* $REMOTE_HOST:/root/app/zhiwo/admin-frontend/"

log_info "停止后端服务..."
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/zhiwo/user-service/user-service-stop.sh || true'"
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/zhiwo/admin-service/admin-service-stop.sh || true'"

sleep 2

log_info "启动后端服务..."
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/zhiwo/user-service/user-service-start.sh'"
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/zhiwo/admin-service/admin-service-start.sh'"

log_info "重载 Nginx..."
eval "$SSH_CMD $REMOTE_HOST 'nginx -s reload'"

log_info "快速重启完成！"
