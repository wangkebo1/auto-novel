# 部署文档

## 📋 环境要求

### 开发环境

- **Java**：JDK 17+
- **Node.js**：20.x+
- **Maven**：3.9+
- **Docker**：20.x+
- **Docker Compose**：2.x+

### 硬件要求

- **最低配置**：4 核 CPU，8GB 内存，20GB 磁盘
- **推荐配置**：8 核 CPU，16GB 内存，50GB 磁盘

## 🚀 本地开发部署

### 步骤 1：克隆项目

```bash
git clone <repository-url>
cd springAI
```

### 步骤 2：启动基础设施

```bash
# 启动 PostgreSQL、Nacos、Ollama
docker-compose up -d

# 检查服务状态
docker-compose ps
```

### 步骤 3：初始化数据库

数据库会通过 Flyway 自动初始化，无需手动操作。

### 步骤 4：配置 Ollama 模型

```bash
# 进入 Ollama 容器
docker exec -it rag-ollama bash

# 拉取 embedding 模型
ollama pull nomic-embed-text

# 退出容器
exit
```

### 步骤 5：配置后端环境变量

编辑 `rag-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_db
    username: postgres
    password: postgres123
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key}
      base-url: https://api.openai.com
```

### 步骤 6：启动后端服务

```bash
# 终端 1：启动后端
cd rag-backend
mvn clean install
mvn spring-boot:run

# 终端 2：启动网关
cd rag-gateway
mvn spring-boot:run
```

### 步骤 7：启动前端

```bash
# 终端 3：启动前端
cd rag-frontend
npm install
npm run dev
```

### 步骤 8：验证部署

- 前端：http://localhost:5173
- API 文档：http://localhost:8084/swagger-ui.html
- Nacos：http://localhost:8848/nacos

## 🐳 Docker 完整部署

### 方式一：使用预构建镜像

```bash
# 构建所有镜像
docker-compose -f docker-compose.full.yml build

# 启动所有服务
docker-compose -f docker-compose.full.yml up -d
```

### 方式二：分步构建

```bash
# 1. 构建后端镜像
cd rag-backend
docker build -t rag-backend:1.0.0 .

# 2. 构建网关镜像
cd ../rag-gateway
docker build -t rag-gateway:1.0.0 .

# 3. 构建前端镜像
cd ../rag-frontend
docker build -t rag-frontend:1.0.0 .

# 4. 启动所有服务
cd ..
docker-compose up -d
```

## 🌐 生产环境部署

### 1. 环境变量配置

创建 `.env.production` 文件：

```env
# 数据库配置
DB_HOST=your-db-host
DB_PORT=5432
DB_NAME=rag_db
DB_USER=postgres
DB_PASSWORD=your-secure-password

# JWT 配置
JWT_SECRET=your-jwt-secret-key-min-256-bits

# OpenAI 配置
OPENAI_API_KEY=your-openai-api-key

# Nacos 配置
NACOS_SERVER=your-nacos-server:8848
```

### 2. 数据库准备

```bash
# 创建数据库
psql -U postgres -c "CREATE DATABASE rag_db;"

# 安装 pgvector 扩展
psql -U postgres -d rag_db -c "CREATE EXTENSION vector;"
```

### 3. 部署建议

#### 使用 Nginx 反向代理

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:5173;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### 使用 Systemd 管理服务

创建 `/etc/systemd/system/rag-backend.service`：

```ini
[Unit]
Description=RAG Backend Service
After=network.target

[Service]
Type=simple
User=rag
WorkingDirectory=/opt/rag-backend
ExecStart=/usr/bin/java -jar app.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl enable rag-backend
sudo systemctl start rag-backend
```

## 🔧 常见问题

### 1. Nacos 启动失败

**问题**：Nacos 容器启动后立即退出

**解决**：
```bash
# 检查日志
docker logs rag-nacos

# 清理数据重新启动
docker-compose down -v
docker-compose up -d
```

### 2. 数据库连接失败

**问题**：后端无法连接 PostgreSQL

**解决**：
```bash
# 检查数据库是否就绪
docker exec -it rag-postgres pg_isready

# 检查连接配置
# 确保 application.yml 中的数据库地址正确
```

### 3. Ollama 模型未加载

**问题**：向量化失败

**解决**：
```bash
# 检查模型是否已下载
docker exec -it rag-ollama ollama list

# 重新拉取模型
docker exec -it rag-ollama ollama pull nomic-embed-text
```

### 4. 前端无法访问后端

**问题**：CORS 错误

**解决**：
检查网关的 CORS 配置（`rag-gateway/src/main/resources/application.yml`）

### 5. JWT Token 验证失败

**问题**：登录后请求返回 401

**解决**：
确保网关和后端使用相同的 JWT Secret

## 📊 性能优化

### 1. JVM 参数优化

```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -jar app.jar
```

### 2. 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### 3. Ollama GPU 加速

```yaml
# docker-compose.yml
ollama:
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: 1
            capabilities: [gpu]
```

## 🔒 安全建议

1. **修改默认密码**：PostgreSQL、Nacos 等服务的默认密码
2. **使用 HTTPS**：生产环境必须启用 SSL/TLS
3. **限制端口访问**：使用防火墙限制不必要的端口暴露
4. **定期备份**：数据库和配置文件定期备份
5. **日志监控**：配置日志收集和监控告警

## 📝 维护指南

### 日志查看

```bash
# Docker 日志
docker-compose logs -f [service-name]

# 应用日志
tail -f rag-backend/logs/application.log
```

### 数据备份

```bash
# 备份数据库
docker exec rag-postgres pg_dump -U postgres rag_db > backup.sql

# 恢复数据库
docker exec -i rag-postgres psql -U postgres rag_db < backup.sql
```

### 服务重启

```bash
# 重启单个服务
docker-compose restart rag-backend

# 重启所有服务
docker-compose restart
```

## 📞 技术支持

如遇到问题，请：
1. 查看日志文件
2. 检查 [常见问题](#常见问题) 章节
3. 提交 Issue 到项目仓库
