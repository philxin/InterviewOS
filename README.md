# InterviewOS

AI 驱动的面试认知训练系统。

## 项目概览

InterviewOS 当前以 `V1` 闭环为目标，已经打通知识点管理、训练出题、AI 评分、掌握度更新和训练历史查询。

当前 V1 主流程：

1. 创建知识点
2. 发起训练，生成问题
3. 提交回答，获取评分与建议
4. 更新知识点掌握度
5. 查询训练历史

V1 明确不做：

- 用户系统与权限体系
- Redis 缓存能力落地
- pgvector 语义检索
- 多轮对话
- 复杂统计报表

## 技术栈

### 后端

- Java 21
- Spring Boot 3.3.6
- Spring AI 1.0.0-M5
- Spring Web / JPA / Validation / Security / Redis
- PostgreSQL 14+
- Maven Wrapper

### 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Tailwind CSS
- Axios
- ECharts

## 目录结构

```text
InterviewOS/
  backend/                 Spring Boot 后端
    src/main/java/
    src/main/resources/
    scripts/               本地启动与环境变量加载脚本
    .env.example           后端环境变量模板
  frontend/                Vue 3 + Vite 前端
    src/
    .env.production.example
  docs/                    规划、接口、联调、看板等文档
  docker/                  预留目录
  scripts/                 仓库级辅助脚本
  DEPLOYMENT.md            Linux 服务器部署说明
  README.md
```

## 当前接口边界

V1 后端仅公开以下 API：

- `/api/knowledge/**`
- `/api/training/**`

其他路径默认拒绝访问。

后端每个请求都会返回：

- `X-Request-Id`

日志默认携带 `requestId`，排障时应优先使用该值串联访问日志、异常日志和 LLM 调用日志。

## 环境要求

本地开发建议：

- Java 21
- Node.js 20+
- PostgreSQL 14+
- Redis 6+

## 配置说明

### Spring Profile

当前后端支持以下 profile 组合：

- `dev-postgres`
- `dev-mysql`
- `prod-postgres`
- `prod-mysql`

V1 当前实际建议：

- 本地开发：`dev-postgres`
- 服务器部署：`prod-postgres`

### 后端配置

参考模板：

- [backend/.env.example](D:\InterviewOS\backend\.env.example)

关键项：

- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `SERVER_CONTEXT_PATH`
- `POSTGRES_URL`
- `POSTGRES_USERNAME`
- `POSTGRES_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_CHAT_MODEL`
- `APP_CORS_ALLOWED_ORIGINS`

注意：

- Spring Boot 不会自动读取 `.env`
- 本项目通过 `backend/scripts/load-env.*` 和 `backend/scripts/start-dev.*` 注入环境变量
- 敏感信息必须走环境变量，禁止提交到仓库

### 前端配置

生产环境模板：

- [frontend/.env.production.example](D:\InterviewOS\frontend\.env.production.example)

关键项：

- `VITE_API_BASE_URL`

示例：

```env
VITE_API_BASE_URL=https://your-domain.com/api
```

注意：

- `VITE_*` 变量会打包进浏览器端
- 不可在前端环境变量中放任何 secret

## 本地启动

### Windows PowerShell

后端：

```powershell
cd backend
. .\scripts\load-env.ps1
.\scripts\start-dev.ps1 -Profile dev-postgres
```

如果你已经手动配置好 `JAVA_HOME`，也可以直接：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

### Linux / macOS

后端：

```bash
cd backend
source ./scripts/load-env.sh
./scripts/start-dev.sh --profile dev-postgres
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## 默认访问地址

本地开发默认地址：

- 前端：`http://localhost:5173` 或 Vite 实际输出地址
- 后端：`http://localhost:8080/api`

前端默认请求：

- `http://localhost:8080/api`

如果前端端口变更，请同步更新后端 CORS：

- `APP_CORS_ALLOWED_ORIGINS`

## 测试与构建

### 后端测试

```powershell
cd backend
.\mvnw.cmd test
```

### 前端类型检查

```powershell
cd frontend
npx vue-tsc -b
```

### 前端构建

```powershell
cd frontend
npm run build
```

## 联调验收

已完成 V1 主链路联调回归：

- 知识点 CRUD
- 训练开始
- 训练历史查询
- 删除级联
- 本地 CORS 验证

相关文档：

- [docs/plan/API接口设计.md](D:\InterviewOS\docs\plan\API接口设计.md)
- [docs/plan/V1联调验收记录.md](D:\InterviewOS\docs\plan\V1联调验收记录.md)
- [docs/plan/V1执行看板.md](D:\InterviewOS\docs\plan\V1执行看板.md)

## 服务器部署

部署说明见：

- [DEPLOYMENT.md](D:\InterviewOS\DEPLOYMENT.md)

当前推荐部署形态：

1. Nginx 托管前端静态资源
2. Nginx 反向代理 `/api` 到 Spring Boot
3. 后端使用 `prod-postgres`
4. 数据库当前先使用 PostgreSQL 单库

## 日志与排障

### 关键排障入口

1. 看响应头里的 `X-Request-Id`
2. 用 `requestId` 查后端日志
3. 再判断是请求校验、业务异常还是 LLM 调用异常

### 当前日志策略

- 访问日志：记录方法、路径、状态码、耗时
- 业务日志：记录关键状态变化
- LLM 日志：记录耗时、长度、指纹，不记录原始 prompt/response
- 异常日志：4xx/5xx 分级记录，避免输出敏感正文

## 常见问题

### 1. PowerShell 中文乱码

建议先执行仓库脚本：

- [scripts/fix-powershell-utf8.ps1](D:\InterviewOS\scripts\fix-powershell-utf8.ps1)

### 2. 前端跨域报错

检查：

- 前端实际访问地址
- `APP_CORS_ALLOWED_ORIGINS`
- 后端是否已重启

### 3. 后端启动后接口 401

V1 已使用显式 `SecurityConfig` 放行公开接口；如果出现 401，优先检查：

- 是否启动了正确分支代码
- 是否有本地额外安全配置覆盖
- 是否通过了 `/api/knowledge` 基础探活

### 4. LLM 返回内容异常或乱码

优先检查：

- `OPENAI_BASE_URL`
- `OPENAI_CHAT_MODEL`
- 上游服务兼容性
- 后端日志里的 `requestId` 与 LLM 调用日志

## 文档索引

建议优先阅读：

1. [docs/plan/V1执行看板.md](D:\InterviewOS\docs\plan\V1执行看板.md)
2. [docs/plan/API接口设计.md](D:\InterviewOS\docs\plan\API接口设计.md)
3. [docs/plan/V1联调验收记录.md](D:\InterviewOS\docs\plan\V1联调验收记录.md)
4. [DEPLOYMENT.md](D:\InterviewOS\DEPLOYMENT.md)
