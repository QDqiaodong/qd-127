# 商业步行街公共休息长凳街区分段树形分类管理系统

## 项目简介

本系统面向商业步行街运营后勤，按街区、路段、点位维护公共休息长凳分类体系，支持长凳建档、点位绑定、归属变更和变更台账。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Axios
- 后端：Spring Boot 3.3、JDK 17、MyBatis Plus、Redis
- 数据库：MySQL 8.0
- 部署：Docker Compose

## 端口说明

| 服务 | 地址或端口 |
| --- | --- |
| 前端访问地址 | http://localhost:8227 |
| 后端 API 地址 | http://localhost:8327/api |
| MySQL | 127.0.0.1:3527 |
| Redis | 127.0.0.1:6627 |

端口统一维护在根目录 `.env`，示例配置见 `.env.example`。

## 启动方式

```bash
cd /Users/Admin/Desktop/solo-0601/qd-0601/qd-组1/qd-127
docker compose up -d --build
```

## 单独编译验证

```bash
cd backend
mvn compile -q
```

```bash
cd frontend
npm ci
npm run build
```

## Docker 构建说明

Docker Compose 使用固定端口并绑定 `127.0.0.1`；前后端镜像分层安装依赖，再复制源码构建，便于后续审计区分依赖构建、应用编译和运行链路问题。

## 常见问题

- 后端编译失败时先执行 `mvn -version` 检查 JDK，再检查 Lombok、Maven 编译插件和 `pom.xml` 是否被忽略。
- 前端构建失败时优先按实际报错检查 import 路径、导出名、Vite 代理端口和构建期语法。
- 页面中文乱码时检查源码、SQL 初始化脚本、数据库字符集、连接串编码和已有 Docker volume 数据；初始化 SQL 已增加 `SET NAMES utf8mb4;`。
