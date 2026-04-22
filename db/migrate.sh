#!/bin/bash
# Database migration runner
# Usage: ./migrate.sh [env]
#   env: local (default) | prod

set -e

ENV=${1:-local}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 加载 .env 文件（如果存在）
if [ -f "$SCRIPT_DIR/../.env" ]; then
    set -a
    source "$SCRIPT_DIR/../.env"
    set +a
fi

case "$ENV" in
  local)
    DB_HOST=${DB_HOST:-localhost}
    DB_PORT=${DB_PORT:-3306}
    DB_NAME=${DB_NAME:-blogger_db}
    DB_USER=${DB_USER:-root}
    DB_PASS=${DB_PASS:-}
    ;;
  prod)
    DB_HOST=${DB_HOST:-localhost}
    DB_PORT=${DB_PORT:-3306}
    DB_NAME=${DB_NAME:-blogger_db}
    DB_USER=${DB_USER:-root}
    DB_PASS=${DB_PASS:-}
    ;;
  *)
    echo "Usage: $0 [local|prod]"
    exit 1
    ;;
esac

MYSQL="mysql -h$DB_HOST -P$DB_PORT -u$DB_USER"
[ -n "$DB_PASS" ] && MYSQL="$MYSQL -p$DB_PASS"
MYSQL="$MYSQL $DB_NAME"

# Ensure _schema_version table exists
echo "Ensuring _schema_version table exists..."
$MYSQL -e "
CREATE TABLE IF NOT EXISTS _schema_version (
    version VARCHAR(20) PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    executed_by VARCHAR(50) DEFAULT ''
);" 2>/dev/null || true

# Get executed versions
EXECUTED=$($MYSQL -N -e "SELECT version FROM _schema_version ORDER BY version;" 2>/dev/null || true)

# Run pending migrations
RAN=0
for file in "$SCRIPT_DIR"/migrations/V*.sql; do
    [ -e "$file" ] || continue
    v=$(basename "$file" | grep -oE '^V[0-9]+' || true)
    [ -n "$v" ] || continue

    if echo "$EXECUTED" | grep -qw "$v"; then
        echo "SKIP $v (already executed)"
        continue
    fi

    desc=$(basename "$file" | sed -E 's/^V[0-9]+__//; s/\.sql$//; s/_/ /g')
    echo "EXEC $v ($desc) ..."
    $MYSQL < "$file"
    RAN=$((RAN + 1))
done

if [ "$RAN" -eq 0 ]; then
    echo "No pending migrations."
else
    echo "Done. Executed $RAN migration(s)."
fi
