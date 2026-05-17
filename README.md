# RAG 知识库问答系统

基于 Spring AI 和 Vue.js 的企业级 RAG（检索增强生成）知识库问答系统。

## 📋 项目简介

本系统采用微服务架构，支持文档上传、向量化存储、智能问答等功能，适用于企业知识管理、智能客服等场景。

### 核心功能

- 🔐 **用户认证**：JWT 无状态认证，支持注册/登录
- 📚 **知识库管理**：创建、查询、删除知识库
- 📄 **文档处理**：支持 PDF、Word、Excel 等多格式文档上传
- 🤖 **智能问答**：基于 RAG 的上下文感知问答
- 📖 **小说生成**：AI 辅助创作功能
- 🎬 **视频处理**：视频内容分析（扩展功能）

## 🏗️ 技术架构

### 后端技术栈

- **框架**：Spring Boot 3.x + Spring Cloud
- **服务治理**：Nacos（服务注册与发现）
- **网关**：Spring Cloud Gateway
- **数据库**：PostgreSQL + pgvector（向量存储）
- **AI 框架**：Spring AI + Ollama
- **认证**：Spring Security + JWT
- **API 文档**：SpringDoc OpenAPI 3.0
- **数据库迁移**：Flyway

### 前端技术栈

- **框架**：Vue 3 + Vite 6
- **语言**：TypeScript 5
- **UI 库**：Tailwind CSS + DaisyUI
- **HTTP 客户端**：Axios

### 基础设施

- **容器化**：Docker + Docker Compose
- **向量化服务**：Ollama
- **服务注册中心**：Nacos

## 📦 项目结构

```
.
├── rag-backend/          # 后端服务
├── rag-gateway/          # API 网关
├── rag-common/           # 公共模块
├── rag-frontend/         # 前端应用
├── docker-compose.yml    # Docker 编排配置
└── docs/                 # 文档目录
```

## 🚀 快速开始

### 前置要求

- Java 17+
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+

### 1. 启动基础设施

```bash
# 启动 PostgreSQL、Nacos、Ollama
docker-compose up -d

# 等待服务就绪（约 30 秒）
docker-compose ps
```

### 2. 初始化 Ollama 模型

```bash
# 拉取 embedding 模型
docker exec -it rag-ollama ollama pull nomic-embed-text
```

### 3. 启动后端服务

```bash
# 启动后端
cd rag-backend
mvn spring-boot:run

# 启动网关
cd ../rag-gateway
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd rag-frontend
npm install
npm run dev
```

### 5. 访问应用

- **前端应用**：http://localhost:5173
- **API 网关**：http://localhost:8080
- **API 文档**：http://localhost:8084/swagger-ui.html
- **Nacos 控制台**：http://localhost:8848/nacos（账号：nacos/nacos）

## 📖 API 文档

启动后端服务后，访问 Swagger UI 查看完整 API 文档：

- **Swagger UI**：http://localhost:8084/swagger-ui.html
- **OpenAPI JSON**：http://localhost:8084/v3/api-docs

### 主要接口

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/auth/register` | POST | 用户注册 | ❌ |
| `/api/auth/login` | POST | 用户登录 | ❌ |
| `/api/knowledge-bases` | GET | 知识库列表 | ✅ |
| `/api/knowledge-bases` | POST | 创建知识库 | ✅ |
| `/api/documents/upload` | POST | 上传文档 | ✅ |
| `/api/chat` | POST | 聊天问答 | ✅ |

## 🧪 测试

### 运行后端测试

```bash
cd rag-backend
mvn test
```

### 测试覆盖

- ✅ 单元测试：Controller、Service 层
- ✅ 集成测试：API 接口测试
- 📊 覆盖率：50%+（核心功能）

## 🐳 Docker 部署

### 构建镜像

```bash
# 构建后端镜像
cd rag-backend
docker build -t rag-backend:latest .

# 构建网关镜像
cd ../rag-gateway
docker build -t rag-gateway:latest .

# 构建前端镜像
cd ../rag-frontend
docker build -t rag-frontend:latest .
```

### 一键启动

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 📊 性能测试

参考 [performance-test.md](performance-test.md) 进行性能测试。

## 🔧 配置说明

### 环境变量

**后端服务** (`rag-backend/src/main/resources/application.yml`)：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_db
    username: postgres
    password: postgres123
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

**前端应用** (`.env`)：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 📝 开发指南

详细开发文档请参考：
- [部署文档](docs/DEPLOYMENT.md)
- [API 开发指南](docs/API_GUIDE.md)
- [微服务架构说明](MICROSERVICES.md)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👥 联系方式

- 项目维护者：RAG Team
- 邮箱：support@rag.com
