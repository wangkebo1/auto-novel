# RAG 微服务架构

## 架构说明

### 模块结构
- **rag-common**: 公共模块，包含 DTO、异常处理、常量等共享代码
- **rag-gateway**: API 网关，提供统一入口、路由、JWT 鉴权
- **rag-backend**: 后端服务，处理业务逻辑

### 技术栈
- Spring Cloud Gateway: API 网关
- Nacos: 服务注册与发现、配置中心
- Spring Cloud LoadBalancer: 客户端负载均衡
- JWT: 无状态认证

## 服务端口

| 服务 | 端口 | 说明 |
|-----|------|------|
| Nacos | 8848 | 服务注册中心 |
| Gateway | 8080 | API 网关 |
| Backend | 8084 | 后端服务 |
| Frontend | 5173 | 前端服务 |
| PostgreSQL | 5432 | 数据库 |
| Ollama | 11434 | 向量化服务 |

## 启动顺序

### 1. 启动基础设施
```bash
# 启动 PostgreSQL 和 Nacos
docker-compose up -d
```

### 2. 等待 Nacos 就绪
访问 http://localhost:8848/nacos （默认账号: nacos/nacos）

### 3. 启动后端服务
```bash
cd rag-backend
mvn spring-boot:run
```

### 4. 启动网关服务
```bash
cd rag-gateway
mvn spring-boot:run
```

### 5. 启动前端
```bash
cd frontend
npm run dev
```

## API 路由

所有请求通过网关（8080端口）访问:

- `POST /api/auth/login` - 用户登录（无需鉴权）
- `POST /api/auth/register` - 用户注册（无需鉴权）
- `GET /api/knowledge-bases` - 知识库列表（需要 JWT）
- `POST /api/knowledge-bases` - 创建知识库（需要 JWT）
- `POST /api/documents/upload` - 上传文档（需要 JWT）
- `POST /api/chat` - 聊天接口（需要 JWT）
- `GET /api/chat/stream` - 流式聊天（需要 JWT）

## JWT 认证流程

1. 用户通过 `/api/auth/login` 登录获取 JWT Token
2. 前端在请求头中携带 `Authorization: Bearer <token>`
3. 网关 JwtAuthenticationFilter 验证 Token
4. 验证通过后添加 `X-User-Name` 请求头传递给后端服务
5. 后端服务从请求头获取用户信息

## 服务注册

所有微服务启动后会自动注册到 Nacos:
- rag-backend: 后端服务实例
- rag-gateway: 网关服务实例

## 配置说明

### Gateway 配置 (application.yml)
- 路由配置: 定义 URL 路径到后端服务的映射
- CORS 配置: 允许前端跨域访问
- JWT 配置: Token 验证密钥

### Backend 配置
- Nacos 服务发现地址: localhost:8848
- 服务名称: rag-backend
- JWT 密钥: 与 Gateway 保持一致

## 扩展计划

- [ ] 将认证服务独立为 rag-auth 模块
- [ ] 添加 Sentinel 流量控制
- [ ] 添加 Skywalking 链路追踪
- [ ] 使用 Nacos 配置中心统一管理配置
- [ ] 添加服务间 OpenFeign 调用
