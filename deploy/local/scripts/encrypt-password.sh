#!/bin/bash
# Jasypt 密码加密工具
# 用法: ./encrypt-password.sh

echo "========================================"
echo "Jasypt 密码加密工具"
echo "========================================"
echo ""

read -s -p "请输入加密密钥 (JASYPT_ENCRYPTOR_PASSWORD): " SECRET
echo ""
read -s -p "请输入要加密的明文密码: " PLAIN
echo ""

echo ""
echo "正在加密..."

# 使用 Maven 运行 Jasypt 加密
# 需要先构建项目获取依赖
cd "$(dirname "$0")/../services/user-backend" || exit 1

# 通过 Spring Boot 的加密端点来加密（需要项目已编译）
# 或者直接用 Java 运行 jasypt 的 CLI

# 方案: 用 Maven exec 运行加密
ENCRYPTED=$(mvn -q exec:java \
  -Dexec.mainClass="org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI" \
  -Dexec.args="input=$PLAIN password=$SECRET algorithm=PBEWithHMACSHA512AndAES_256" \
  2>/dev/null | grep "OUTPUT" | sed 's/.*OUTPUT: //')

if [ -z "$ENCRYPTED" ]; then
  echo "加密失败，尝试备用方案..."
  echo ""
  echo "请手动执行以下命令（需要先下载 jasypt jar）:"
  echo ""
  echo "java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \\"
  echo "  input=\"$PLAIN\" \\"
  echo "  password=\"$SECRET\" \\"
  echo "  algorithm=PBEWithHMACSHA512AndAES_256"
  echo ""
  echo "或者使用在线工具生成后填入 ENC(...)"
  exit 1
fi

echo ""
echo "========================================"
echo "加密结果:"
echo "========================================"
echo ""
echo "ENC($ENCRYPTED)"
echo ""
echo "请将以上结果填入 application-prod.yml 的 password 字段"
echo "并在服务器上设置环境变量:"
echo "  export JASYPT_ENCRYPTOR_PASSWORD=$SECRET"
echo "========================================"
