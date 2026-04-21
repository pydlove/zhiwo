#!/bin/bash
set -e

# ===================================================================
# 公众号创作助手 - 数据库同步脚本
# 将本地 MySQL 数据导出并导入到远程服务器
# ===================================================================

# ==================== 本地数据库配置 ====================
LOCAL_DB_HOST="localhost"
LOCAL_DB_PORT="3306"
LOCAL_DB_USER="root"                # 本地 MySQL 用户名
LOCAL_DB_PASSWORD="123456"          # 本地 MySQL 密码（空密码则留空）
LOCAL_DB_NAME="blogger_db"          # 本地数据库名

# ==================== 远程服务器配置 ====================
REMOTE_HOST_IP="101.126.15.58"        # 例如: 123.45.67.89
REMOTE_SSH_USER="root"              # SSH 登录用户名
REMOTE_SSH_PASSWORD="Pydlove520smy@1"   # SSH 密码（使用密钥则留空）
REMOTE_SSH_KEY=""                   # SSH 私钥路径，例如: ~/.ssh/id_rsa

# ==================== 远程数据库配置 ====================
REMOTE_DB_HOST="101.126.15.58"          # 远程 MySQL 地址（通常localhost）
REMOTE_DB_PORT="3306"
REMOTE_DB_USER="root"               # 远程 MySQL 用户名
REMOTE_DB_PASSWORD="assetQaz#612"  # 远程 MySQL 密码
REMOTE_DB_NAME="blogger_db"         # 远程数据库名（通常和本地一致）

# ==================== 可选配置 ====================
# 是否只导出表结构（不导出数据），yes/no
STRUCTURE_ONLY="no"

# 要排除的表（空格分隔），例如: "log operation_log"
EXCLUDE_TABLES=""

# 导出文件临时存放路径
DUMP_FILE="/tmp/blogger_db_sync_$(date +%Y%m%d_%H%M%S).sql"

# ===================================================
# 以下代码一般不需要修改
# ===================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

function log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
function log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
function log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查必要命令
function check_command() {
  if ! command -v "$1" &> /dev/null; then
    log_error "缺少命令: $1，请先安装"
    exit 1
  fi
}

check_command mysqldump
check_command mysql

# 构建 SSH/SCP 命令
if [ -n "$REMOTE_SSH_KEY" ] && [ -f "$REMOTE_SSH_KEY" ]; then
  SSH_CMD="ssh -i $REMOTE_SSH_KEY -o StrictHostKeyChecking=no"
  SCP_CMD="scp -i $REMOTE_SSH_KEY -o StrictHostKeyChecking=no"
else
  if ! command -v sshpass &> /dev/null; then
    log_error "未安装 sshpass，请执行: brew install sshpass (macOS) 或 apt-get install sshpass (Linux)"
    log_error "或者配置 REMOTE_SSH_KEY 使用密钥登录"
    exit 1
  fi
  SSH_CMD="sshpass -p '$REMOTE_SSH_PASSWORD' ssh -o StrictHostKeyChecking=no"
  SCP_CMD="sshpass -p '$REMOTE_SSH_PASSWORD' scp -o StrictHostKeyChecking=no"
fi

REMOTE_HOST="$REMOTE_SSH_USER@$REMOTE_HOST_IP"

# 构建本地 MySQL 连接参数（密码通过环境变量传递，避免命令行警告和引号问题）
LOCAL_MYSQL_OPTS="-h$LOCAL_DB_HOST -P$LOCAL_DB_PORT -u$LOCAL_DB_USER"
LOCAL_MYSQLDUMP_OPTS="$LOCAL_MYSQL_OPTS"

if [ -n "$LOCAL_DB_PASSWORD" ]; then
  export MYSQL_PWD="$LOCAL_DB_PASSWORD"
fi

# 构建 mysqldump 命令
DUMP_OPTS="--single-transaction --routines --triggers"

if [ "$STRUCTURE_ONLY" = "yes" ]; then
  DUMP_OPTS="$DUMP_OPTS --no-data"
  log_info "模式: 只导出表结构"
else
  log_info "模式: 导出结构和数据"
fi

# 处理排除的表
for tbl in $EXCLUDE_TABLES; do
  DUMP_OPTS="$DUMP_OPTS --ignore-table=$LOCAL_DB_NAME.$tbl"
  log_warn "排除表: $tbl"
done

log_info "========================================"
log_info "数据库同步开始"
log_info "========================================"
log_info "本地: $LOCAL_DB_HOST:$LOCAL_DB_PORT / $LOCAL_DB_NAME"
log_info "远程: $REMOTE_HOST_IP:$REMOTE_DB_PORT / $REMOTE_DB_NAME"

# 步骤1: 导出本地数据库
log_info ""
log_info "[1/5] 正在导出本地数据库..."
mysqldump $DUMP_OPTS $LOCAL_MYSQLDUMP_OPTS "$LOCAL_DB_NAME" > "$DUMP_FILE"

if [ $? -ne 0 ] || [ ! -f "$DUMP_FILE" ]; then
  log_error "数据库导出失败"
  exit 1
fi

FILE_SIZE=$(ls -lh "$DUMP_FILE" | awk '{print $5}')
log_info "导出完成: $DUMP_FILE ($FILE_SIZE)"

# 步骤2: 上传 SQL 文件到服务器
log_info ""
log_info "[2/5] 上传 SQL 文件到服务器..."
eval "$SCP_CMD $DUMP_FILE $REMOTE_HOST:/tmp/"

if [ $? -ne 0 ]; then
  log_error "文件上传失败"
  rm -f "$DUMP_FILE"
  exit 1
fi
log_info "上传完成"

# 步骤3: 在远程服务器创建数据库（如果不存在）
log_info ""
log_info "[3/5] 检查远程数据库..."

# 构建远程 mysql 命令（密码通过环境变量传递）
REMOTE_MYSQL="mysql -h$REMOTE_DB_HOST -P$REMOTE_DB_PORT -u$REMOTE_DB_USER"
REMOTE_MYSQL_ENV=""
if [ -n "$REMOTE_DB_PASSWORD" ]; then
  REMOTE_MYSQL_ENV="export MYSQL_PWD='$REMOTE_DB_PASSWORD'; "
fi

# 远程执行：创建数据库
eval "$SSH_CMD $REMOTE_HOST '$REMOTE_MYSQL_ENV $REMOTE_MYSQL -e \"CREATE DATABASE IF NOT EXISTS $REMOTE_DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\"'"

if [ $? -ne 0 ]; then
  log_error "远程数据库创建失败，请检查远程数据库配置"
  rm -f "$DUMP_FILE"
  exit 1
fi
log_info "数据库 $REMOTE_DB_NAME 已就绪"

# 步骤4: 导入数据到远程数据库
log_info ""
log_info "[4/5] 导入数据到远程数据库..."

REMOTE_DUMP_FILE="/tmp/$(basename $DUMP_FILE)"
eval "$SSH_CMD $REMOTE_HOST '$REMOTE_MYSQL_ENV $REMOTE_MYSQL $REMOTE_DB_NAME < $REMOTE_DUMP_FILE'"

if [ $? -ne 0 ]; then
  log_error "数据导入失败"
  rm -f "$DUMP_FILE"
  exit 1
fi
log_info "导入完成"

# 步骤5: 清理临时文件
log_info ""
log_info "[5/5] 清理临时文件..."
rm -f "$DUMP_FILE"
eval "$SSH_CMD $REMOTE_HOST 'rm -f $REMOTE_DUMP_FILE'"
log_info "清理完成"

log_info ""
log_info "========================================"
log_info "数据库同步成功"
log_info "========================================"
log_info "本地数据库: $LOCAL_DB_NAME"
log_info "远程数据库: $REMOTE_DB_NAME@$REMOTE_HOST_IP"
log_info "========================================"
