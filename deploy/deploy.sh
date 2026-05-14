#!/bin/bash
set -e

# ===================================================================
# 公众号创作助手 - 一键部署脚本
# 运行环境：macOS / Linux（本地机器）
# 目标服务器：Linux（CentOS/Ubuntu 等）
# ===================================================================

# ============ 配置区（请按实际情况修改） ============
SERVER_IP="101.126.15.58"           # 例如: 123.45.67.89
SERVER_USER="root"                 # SSH 用户名
SERVER_PASSWORD="Pydlove520smy@1"   # SSH 密码（或使用 SSH_KEY_PATH）
SSH_KEY_PATH="~/.ssh/id_rsa"                    # 如使用密钥登录，填密钥路径，例如: ~/.ssh/id_rsa
SERVER_DOMAIN="www.mmshuo.tech" # 你的域名（用于 nginx server_name）
NGINX_SSL_CERT="/root/ssl/fullchain.pem"      # SSL 证书路径
NGINX_SSL_KEY="/root/ssl/domain.key"          # SSL 私钥路径
# ===================================================

# 项目根目录（脚本位于 deploy/ 下）
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$DEPLOY_DIR")"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function log_info() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

function log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

function log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# SSH 连接参数
if [ -n "$SSH_KEY_PATH" ] && [ -f "$SSH_KEY_PATH" ]; then
  SSH_CMD="ssh -i $SSH_KEY_PATH -o StrictHostKeyChecking=no"
  SCP_CMD="scp -i $SSH_KEY_PATH -o StrictHostKeyChecking=no"
else
  # 检查 sshpass
  if ! command -v sshpass &> /dev/null; then
    log_error "未安装 sshpass，请执行: brew install sshpass (macOS) 或 apt-get install sshpass (Linux)"
    log_error "或者配置 SSH_KEY_PATH 使用密钥登录"
    exit 1
  fi
  SSH_CMD="sshpass -p '$SERVER_PASSWORD' ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -o ServerAliveInterval=30"
  SCP_CMD="sshpass -p '$SERVER_PASSWORD' scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 -C -v"
fi

REMOTE_HOST="$SERVER_USER@$SERVER_IP"

# 检查本地环境变量文件
if [ ! -f "$DEPLOY_DIR/.env" ]; then
  log_error "未找到 $DEPLOY_DIR/.env 文件"
  log_error "请创建该文件并填写 JASYPT_ENCRYPTOR_PASSWORD 等环境变量"
  exit 1
fi

# ============ 步骤1: 本地构建 ============
log_info "开始本地构建..."

# 构建用户端前端
cd "$PROJECT_DIR/services/user-frontend"
if [ ! -d "node_modules" ]; then
  log_info "安装用户端前端依赖..."
  npm install
fi
log_info "构建用户端前端..."
npm run build

# 构建管理端前端
cd "$PROJECT_DIR/services/admin-frontend"
if [ ! -d "node_modules" ]; then
  log_info "安装管理端前端依赖..."
  npm install
fi
log_info "构建管理端前端..."
npm run build

# 构建用户端后端
cd "$PROJECT_DIR/services/user-backend"
log_info "构建用户端后端..."
mvn clean package -DskipTests

# 构建管理端后端
cd "$PROJECT_DIR/services/admin-backend"
log_info "构建管理端后端..."
mvn clean package -DskipTests

log_info "本地构建完成"

# ============ 步骤2: 在服务器创建目录结构 ============
log_info "准备服务器目录..."
eval "$SSH_CMD $REMOTE_HOST 'mkdir -p /root/app/web/gzh /root/app/web/gzh-admin /root/app/gzh/user-service /root/app/gzh/admin-service /root/app/gzh/scripts /root/app/gzh/db/migrations'"

# ============ 步骤3: 上传前端静态文件 ============
log_info "上传用户端前端到 /root/app/web/gzh/ ..."
eval "$SCP_CMD -r $PROJECT_DIR/services/user-frontend/dist/* $REMOTE_HOST:/root/app/web/gzh/"

log_info "上传管理端前端到 /root/app/web/gzh-admin/ ..."
eval "$SCP_CMD -r $PROJECT_DIR/services/admin-frontend/dist/* $REMOTE_HOST:/root/app/web/gzh-admin/"

# ============ 步骤4: 上传后端 JAR 包和生产配置 ============
# 先压缩 JAR（25MB -> ~8MB，上传快 3 倍）
log_info "压缩 JAR 包..."
cd "$PROJECT_DIR/services/user-backend/target"
zip -q user-backend-1.0.0.jar.zip user-backend-1.0.0.jar
cd "$PROJECT_DIR/services/admin-backend/target"
zip -q blogger-backend-1.0.0.jar.zip blogger-backend-1.0.0.jar

log_info "上传用户端后端 JAR (压缩)..."
eval "$SCP_CMD $PROJECT_DIR/services/user-backend/target/user-backend-1.0.0.jar.zip $REMOTE_HOST:/root/app/gzh/user-service/"
log_info "解压用户端 JAR..."
eval "$SSH_CMD $REMOTE_HOST 'cd /root/app/gzh/user-service/ && unzip -oq user-backend-1.0.0.jar.zip && rm -f user-backend-1.0.0.jar.zip'"

log_info "上传用户端生产配置..."
eval "$SCP_CMD $PROJECT_DIR/services/user-backend/src/main/resources/application-prod.yml $REMOTE_HOST:/root/app/gzh/user-service/"

log_info "上传管理端后端 JAR (压缩)..."
eval "$SCP_CMD $PROJECT_DIR/services/admin-backend/target/blogger-backend-1.0.0.jar.zip $REMOTE_HOST:/root/app/gzh/admin-service/"
log_info "解压管理端 JAR..."
eval "$SSH_CMD $REMOTE_HOST 'cd /root/app/gzh/admin-service/ && unzip -oq blogger-backend-1.0.0.jar.zip && rm -f blogger-backend-1.0.0.jar.zip'"

log_info "上传管理端生产配置..."
eval "$SCP_CMD $PROJECT_DIR/services/admin-backend/src/main/resources/application-prod.yml $REMOTE_HOST:/root/app/gzh/admin-service/"

log_info "上传 Python 脚本..."
eval "$SSH_CMD $REMOTE_HOST 'mkdir -p /root/app/gzh/scripts/py'"
eval "$SCP_CMD -r $PROJECT_DIR/services/admin-backend/src/main/resources/py/* $REMOTE_HOST:/root/app/gzh/scripts/py/"

# 验证 Python 脚本是否成功上传（md5 校验）
LOCAL_SCRIPT_MD5=$(md5 -q "$PROJECT_DIR/services/admin-backend/src/main/resources/py/replace_periods.py" 2>/dev/null || md5sum "$PROJECT_DIR/services/admin-backend/src/main/resources/py/replace_periods.py" | awk '{print $1}')
REMOTE_SCRIPT_MD5=$(eval "$SSH_CMD $REMOTE_HOST 'md5sum /root/app/gzh/scripts/py/replace_periods.py 2>/dev/null || md5 -q /root/app/gzh/scripts/py/replace_periods.py 2>/dev/null'" | awk '{print $1}')
if [ "$LOCAL_SCRIPT_MD5" != "$REMOTE_SCRIPT_MD5" ]; then
  log_error "Python 脚本 md5 校验失败！"
  log_error "本地: $LOCAL_SCRIPT_MD5"
  log_error "远程: $REMOTE_SCRIPT_MD5"
  log_error "文件可能未正确上传，请检查服务器 /root/app/gzh/scripts/py/replace_periods.py"
  exit 1
fi
log_info "Python 脚本 md5 校验通过 ($LOCAL_SCRIPT_MD5)"

log_info "检查并安装 Python 依赖 (python-docx)..."
eval "$SSH_CMD $REMOTE_HOST 'pip3 install python-docx >/dev/null 2>&1 || pip install python-docx >/dev/null 2>&1 || echo \"警告: python-docx 安装失败，请手动安装\"'"

# 清理本地临时压缩包
rm -f "$PROJECT_DIR/services/user-backend/target/user-backend-1.0.0.jar.zip"
rm -f "$PROJECT_DIR/services/admin-backend/target/blogger-backend-1.0.0.jar.zip"

# ============ 步骤5: 上传启停脚本与环境变量 ============
log_info "上传环境变量文件..."
eval "$SCP_CMD $DEPLOY_DIR/.env $REMOTE_HOST:/root/app/gzh/.env"
eval "$SSH_CMD $REMOTE_HOST 'chmod 600 /root/app/gzh/.env'"

log_info "上传启停脚本..."
eval "$SCP_CMD $DEPLOY_DIR/scripts/user-service-start.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/user-service-stop.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/user-service-restart.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/admin-service-start.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/admin-service-stop.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/admin-service-restart.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/start-all.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/stop-all.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/restart-all.sh $REMOTE_HOST:/root/app/gzh/scripts/"
eval "$SCP_CMD $DEPLOY_DIR/scripts/status.sh $REMOTE_HOST:/root/app/gzh/scripts/"

# 远程设置脚本权限并复制到服务目录
log_info "设置脚本权限..."
eval "$SSH_CMD $REMOTE_HOST 'chmod +x /root/app/gzh/scripts/*.sh && cp /root/app/gzh/scripts/user-service-*.sh /root/app/gzh/user-service/ && cp /root/app/gzh/scripts/admin-service-*.sh /root/app/gzh/admin-service/ && cp /root/app/gzh/scripts/start-all.sh /root/app/gzh/start-all.sh && cp /root/app/gzh/scripts/stop-all.sh /root/app/gzh/stop-all.sh && cp /root/app/gzh/scripts/restart-all.sh /root/app/gzh/restart-all.sh && cp /root/app/gzh/scripts/status.sh /root/app/gzh/status.sh && chmod +x /root/app/gzh/start-all.sh /root/app/gzh/stop-all.sh /root/app/gzh/restart-all.sh /root/app/gzh/status.sh'"

# ============ 步骤6: 数据库迁移 ============
log_info "上传数据库迁移脚本..."
eval "$SCP_CMD $PROJECT_DIR/db/migrate.sh $REMOTE_HOST:/root/app/gzh/db/"
eval "$SCP_CMD -r $PROJECT_DIR/db/migrations/* $REMOTE_HOST:/root/app/gzh/db/migrations/"

log_info "执行数据库迁移..."
eval "$SSH_CMD $REMOTE_HOST 'chmod +x /root/app/gzh/db/migrate.sh && cd /root/app/gzh/db && bash migrate.sh prod'"

# ============ 步骤7: 停止旧服务 ============
log_info "停止旧服务..."
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/gzh/user-service/user-service-stop.sh'"
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/gzh/admin-service/admin-service-stop.sh'"

# 等待端口释放
sleep 2

# 再次检查端口是否还有残留，如果有则强制清理
log_info "检查端口释放状态..."
eval "$SSH_CMD $REMOTE_HOST 'lsof -t -i:8080 >/dev/null 2>&1 && { echo \"清理 admin 残留进程...\"; kill -9 \$(lsof -t -i:8080) 2>/dev/null; } || true'"
eval "$SSH_CMD $REMOTE_HOST 'lsof -t -i:8082 >/dev/null 2>&1 && { echo \"清理 user 残留进程...\"; kill -9 \$(lsof -t -i:8082) 2>/dev/null; } || true'"

sleep 1

# ============ 步骤7: 启动新服务 ============
log_info "启动用户端后端..."
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/gzh/user-service/user-service-start.sh'"

log_info "启动管理端后端..."
eval "$SSH_CMD $REMOTE_HOST '/bin/bash /root/app/gzh/admin-service/admin-service-start.sh'"

# ============ 步骤8: 上传并更新 Nginx 配置 ============
log_info "上传 Nginx 配置..."

# 替换 nginx 中的域名和证书路径为实际配置
TMP_NGINX="/tmp/nginx_gzh_deploy.conf"
sed -e "s/server_name gzh.yourdomain.com/server_name $SERVER_DOMAIN/g" \
    -e "s|ssl_certificate /root/ssl/fullchain.pem|ssl_certificate $NGINX_SSL_CERT|g" \
    -e "s|ssl_certificate_key /root/ssl/domain.key|ssl_certificate_key $NGINX_SSL_KEY|g" \
    "$DEPLOY_DIR/nginx.conf" > "$TMP_NGINX"

eval "$SCP_CMD $TMP_NGINX $REMOTE_HOST:/tmp/nginx_gzh_deploy.conf"

# 远程替换 nginx 配置并检查语法
log_info "更新 Nginx 配置..."
eval "$SSH_CMD $REMOTE_HOST 'cp /tmp/nginx_gzh_deploy.conf /etc/nginx/nginx.conf && nginx -t'"

log_info "重载 Nginx..."
eval "$SSH_CMD $REMOTE_HOST 'nginx -s reload'"

# 清理临时文件
rm -f "$TMP_NGINX"

# ============ 部署完成 ============
log_info "========================================"
log_info "部署完成！"
log_info "========================================"
log_info "用户端访问: https://$SERVER_DOMAIN"
log_info "管理端访问: http://$SERVER_IP:38080"
log_info "========================================"
log_warn "重要提醒:"
log_warn "1. application-prod.yml 中的数据库密码已改为 ENC() 占位符"
log_warn "2. 在本地运行 JasyptEncryptor.main() 生成加密密码，填入 application-prod.yml 后重新部署"
log_warn "3. Jasypt 加密密钥已通过 .env 文件自动同步到服务器，无需手动设置"
log_warn "4. 现有明文密码会在用户/管理员首次登录时自动升级为 BCrypt"
log_warn "5. 如需批量迁移所有明文密码，在服务器执行:"
log_warn "     java -jar xxx.jar --spring.profiles.active=prod,migrate"
log_info "========================================"
