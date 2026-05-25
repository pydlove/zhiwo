# 知我 (Zhiwo) - 自媒体内容创作平台

知我是一款面向自媒体创作者的一站式内容创作与管理平台，涵盖从标题生成、文章创作、AI 检测、图片贴图到发布追踪的完整工作流。

---

## 项目架构

本项目采用前后端分离的微服务架构，包含以下模块：

| 模块 | 技术栈 | 说明 |
|------|--------|------|
| **admin-backend** | Java 17 + Spring Boot 3.2 + MyBatis + MySQL | 管理端后端服务 (端口: 8080) |
| **user-backend** | Java 17 + Spring Boot 3.2 + MyBatis + MySQL | 用户端后端服务 (端口: 8082) |
| **admin-frontend** | Vue 3 + Vite + Ant Design Vue + Pinia | 管理后台前端 (Dev: 5173) |
| **user-frontend** | Vue 3 + Vite + Ant Design Vue + Pinia | 用户端前端 (Dev: 5174) |
| **ai-detect** | Python + FastAPI + PyTorch + Transformers | AI 内容检测服务 |

---

## 功能概览

### 管理端

- **文章管理** - 文章创建、编辑、审核、发布全流程
- **标题生成** - AI 智能标题生成、标题库管理、标题追踪统计
- **AI 检测** - 文章 AI 痕迹检测与去 AI 风味处理
- **图片库** - 批量贴图生成与管理
- **流程管理** - 创作流程配置与调度
- **任务管理** - 创作任务分配与跟踪
- **Agent 配置** - AI Agent 行为与模型参数配置
- **用户 & 权限** - 管理员、角色、会员、订单管理
- **运营支持** - 活动、公告、帮助中心、客服对话
- **数据追踪** - 内容追踪与数据分析

### 用户端

- **智能创作** - AI 辅助文章创作、标题推荐、草稿管理
- **创作记录** - 历史创作内容查看与管理
- **收益概览** - 创作收益数据统计
- **联盟分销** - 推广联盟与佣金管理
- **成长体系** - 公众号注册指南、开号指导、创作技巧
- **客户服务** - 在线客服对话、帮助中心
- **短链接** - 推广链接生成与管理

---

## 本地开发

### 环境要求

- Java 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Python 3.10+ (AI 检测服务可选)

### 1. 数据库初始化

```bash
# 创建数据库并执行初始化脚本
mysql -u root -p < db/init.sql
```

### 2. 配置本地环境

复制并编辑后端本地配置：

```bash
# 管理端后端
vim services/admin-backend/src/main/resources/application-local.yml

# 用户端后端
vim services/user-backend/src/main/resources/application-local.yml
```

配置本地数据库连接和 Jasypt 加密密钥。

### 3. 一键启动所有服务

```bash
bash deploy/local/scripts/restart.sh
```

启动完成后访问：

| 服务 | 地址 |
|------|------|
| 管理后台 | http://localhost:5173 |
| 用户端 | http://localhost:5174 |
| Admin API | http://localhost:8080 |
| User API | http://localhost:8082 |

### 4. 单独启动服务

**后端：**

```bash
cd services/admin-backend
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=local"

cd services/user-backend
mvn spring-boot:run
```

**前端：**

```bash
cd services/admin-frontend
npm run dev -- --host

cd services/user-frontend
npm run dev -- --host
```

---

## 生产部署

### 服务器目录结构

```
/root/app/zhiwo/
├── user-frontend/          # 用户端前端静态文件 (Nginx 服务)
├── admin-frontend/         # 管理端前端静态文件
├── user-service/           # 用户端后端 JAR + 配置
├── admin-service/          # 管理端后端 JAR + 配置
├── scripts/                # 启停脚本
│   └── py/                 # Python 辅助脚本
├── db/                     # 数据库迁移
└── .env                    # 环境变量 (Jasypt 密钥等)
```

### 一键部署

```bash
cd deploy/remote
bash deploy.sh
```

部署脚本会自动完成：
- 本地构建前后端
- 上传前端静态文件、后端 JAR、配置文件、Python 脚本
- 安装 systemd 服务 (`zhiwo-user`, `zhiwo-admin`)
- 上传 SSL 证书并更新 Nginx 配置
- 启动服务

### Systemd 服务管理

```bash
# 启动
systemctl start zhiwo-user zhiwo-admin

# 停止
systemctl stop zhiwo-user zhiwo-admin

# 重启
systemctl restart zhiwo-user zhiwo-admin

# 查看状态
systemctl status zhiwo-user zhiwo-admin
```

### 快速重启（不重新构建）

```bash
cd deploy/remote
bash restart.sh
```

---

## 项目目录说明

```
zhiwo/
├── services/               # 核心业务服务
│   ├── admin-backend/      # 管理端后端 (Spring Boot)
│   ├── admin-frontend/     # 管理端前端 (Vue 3)
│   ├── user-backend/       # 用户端后端 (Spring Boot)
│   ├── user-frontend/      # 用户端前端 (Vue 3)
│   └── ai-detect/          # AI 检测服务 (Python)
├── deploy/                 # 部署相关
│   ├── local/              # 本地开发脚本
│   │   └── scripts/        # 本地一键启停脚本
│   └── remote/             # 远程生产部署
│       ├── scripts/        # 服务器启停脚本
│       ├── systemd/        # systemd 服务配置
│       ├── www.mmshuo.tech_nginx/  # SSL 证书
│       ├── deploy.sh       # 一键部署脚本
│       ├── restart.sh      # 快速重启脚本
│       └── .env            # 生产环境变量
├── db/                     # 数据库脚本
│   ├── init.sql            # 初始化脚本
│   └── migrations/         # 数据库迁移
├── docs/                   # 项目文档
├── data/                   # 数据文件
└── export/                 # 导出文件
```

---

## 安全说明

- 生产环境数据库密码使用 **Jasypt** 加密存储
- 加密密钥通过 `.env` 文件以环境变量方式注入，不提交到代码仓库
- 密码已统一升级为 **BCrypt** 哈希存储

---

## 技术亮点

- **AI 驱动创作** - 集成大语言模型实现智能标题生成、文章创作、AI 检测与去风味化
- **完整工作流** - 从选题、创作、审核、发布到数据追踪的闭环管理
- **多端协同** - 管理端精细化运营 + 用户端轻量化创作的双端架构
- **自动化部署** - 本地一键构建 + 远程一键部署 + systemd 服务托管
