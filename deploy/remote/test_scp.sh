#!/bin/bash
echo "测试 SCP 上传 JAR..."
sshpass -p 'Pydlove520smy@1' scp -v -o StrictHostKeyChecking=no -o ConnectTimeout=60 /Users/panyong/aio_project/小程序/services/admin-backend/target/blogger-backend-1.0.0.jar root@101.126.15.58:/root/test.jar 2>&1
echo "退出码: $?"