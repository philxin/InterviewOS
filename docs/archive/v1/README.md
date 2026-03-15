# InterviewOS V1 归档

## 1. 归档说明

本文档用于冻结 InterviewOS `V1` 阶段的范围、交付物、验收结果和后续阅读入口。

归档原则：

- 将 `docs/plan` 下的 V1 文档归档复制到 `docs/archive/v1/plan/`
- 保留原始 `docs/plan` 文件，避免打断现有 README、部署说明和历史链接
- 通过归档索引统一收敛 V1 的最终状态
- 后续如进入 `V2`，请在新的规划目录中继续演进，不再直接修改 V1 已冻结结论

归档时间：

- `2026-03-14`

V1 当前状态：

- 已完成
- 已完成文档收口
- 可作为后续版本规划与新会话交接的稳定基线

## 2. V1 范围结论

V1 已完成能力：

1. 知识点 CRUD
2. AI 生成训练问题
3. AI 评估回答
4. 掌握度更新
5. 训练历史查询
6. 前后端闭环联调
7. V1 公开接口安全边界收紧
8. 请求追踪与基础可观测性补齐
9. README / 部署 / 联调文档收口

V1 不包含：

1. 用户系统与权限体系
2. Redis 业务缓存能力
3. pgvector 语义检索
4. 多轮对话
5. 复杂统计分析

## 3. V1 核心归档文档

建议按以下顺序阅读：

1. [V1 交接摘要](D:\InterviewOS\docs\archive\v1\V1交接摘要.md)
2. [V1 版本规划](D:\InterviewOS\docs\archive\v1\plan\V1版本规划.md)
3. [API 接口设计](D:\InterviewOS\docs\archive\v1\plan\API接口设计.md)
4. [V1 执行看板](D:\InterviewOS\docs\archive\v1\plan\V1执行看板.md)
5. [V1 联调验收记录](D:\InterviewOS\docs\archive\v1\plan\V1联调验收记录.md)
6. [数据库设计](D:\InterviewOS\docs\archive\v1\plan\数据库设计.md)
7. [后端实现计划](D:\InterviewOS\docs\archive\v1\plan\后端实现计划.md)
8. [前端实现计划](D:\InterviewOS\docs\archive\v1\plan\前端实现计划.md)
9. [开发任务清单](D:\InterviewOS\docs\archive\v1\plan\开发任务清单.md)

## 4. 代码与运行基线

运行基线：

1. 后端：Java 21 + Spring Boot 3.3.6
2. 前端：Vue 3 + Vite
3. 默认 API Base URL：`http://localhost:8080/api`
4. 当前推荐运行 profile：`dev-postgres` / `prod-postgres`
5. 当前业务数据库策略：V1 先以 PostgreSQL 单库落地

运行与部署入口：

1. [README](D:\InterviewOS\README.md)
2. [DEPLOYMENT.md](D:\InterviewOS\DEPLOYMENT.md)

## 5. 归档后的使用方式

如果要开启新的对话或新阶段工作，建议：

1. 先把 [V1 交接摘要](D:\InterviewOS\docs\archive\v1\V1交接摘要.md) 发给新的助手作为上下文
2. 如果涉及接口变更，再补充 [API 接口设计](D:\InterviewOS\docs\archive\v1\plan\API接口设计.md)
3. 如果涉及版本规划，再补充 [V1 版本规划](D:\InterviewOS\docs\archive\v1\plan\V1版本规划.md)
4. 如果进入 V2，请新建独立规划文档，不要直接覆盖 V1 归档结论
