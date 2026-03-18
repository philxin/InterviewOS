# API 接口设计 - V2

## 📋 概述

V2 接口设计围绕以下目标展开：

1. 引入用户鉴权与数据隔离
2. 降低用户进入训练的门槛
3. 提升题目真实性与训练反馈可信度
4. 为后续高频训练闭环预留接口扩展点

所有 API 继续遵循统一响应包结构：

**成功响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

**错误响应**

```json
{
  "code": 400,
  "message": "error message",
  "data": null
}
```

**响应语义**

- 成功请求：使用 `2xx`，且 `code = 0`
- 失败请求：使用 `4xx/5xx`，且 `code` 与错误类型一致
- 禁止“业务失败仍返回 200”

---

## 🔗 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8
- **认证方式**: `Authorization: Bearer <token>`

### Token 返回约定

- `tokenType`: 固定返回 `Bearer`
- `expiresIn`: 返回秒级过期时间
- V2 不实现 refresh token，过期后重新登录

### 公开接口

- `POST /api/auth/register`
- `POST /api/auth/login`

### 受保护接口

除上述认证接口外，其他接口默认都需要登录后访问。

---

## 🔄 V2 相比 V1 的主要变化

### 1. 认证与数据边界

- V1 无鉴权，V2 引入用户体系
- 知识点、训练记录、训练结果全部按用户隔离
- 访问不属于当前用户的资源时，应返回 `404` 或 `403`，避免 IDOR

### 2. 训练接口重构

- V1 `POST /training/start` 改为 V2 `POST /training/sessions`
- V1 `POST /training/submit` 改为 V2 `POST /training/sessions/{sessionId}/answers`
- V1 `GET /training/history*` 改为 V2 `GET /training/sessions*`

### 3. 反馈结构重构

- V1 以 `accuracy / depth / clarity / overall` 为主
- V2 结果页结构改为：
  - 档位
  - 主要问题
  - 缺失点
  - 更好回答思路
  - 更自然的参考表达

### 4. 知识输入升级

- 保留单条知识点 CRUD
- 增加批量导入
- 文件导入只保留少量高价值格式，不做复杂内容平台

---

## 🧱 枚举约定

### QuestionType

| 值 | 说明 |
|----|------|
| `FUNDAMENTAL` | 八股 / 基础概念题 |
| `PROJECT` | 项目经历题 |
| `SCENARIO` | 实际场景题 |

### Difficulty

| 值 | 说明 |
|----|------|
| `EASY` | 基础入门 |
| `MEDIUM` | 常规面试强度 |
| `HARD` | 深挖与追问强度 |

### TargetRole

| 值 | 说明 |
|----|------|
| `JAVA_BACKEND` | Java 后端 |
| `FRONTEND` | 前端开发 |
| `FULLSTACK` | 全栈开发 |
| `DEVOPS` | 运维 / 平台 |
| `DATA_ENGINEER` | 数据工程 |

### KnowledgeSourceType

| 值 | 说明 |
|----|------|
| `MANUAL` | 手工创建 |
| `BATCH_IMPORT` | 批量导入 |
| `FILE_IMPORT` | 文件导入 |
| `ROLE_GENERATED` | 基于岗位生成 |

### FeedbackBand

| 值 | 标签 | 说明 |
|----|------|------|
| `UNCLEAR` | 表达不清晰 | 答案结构混乱、难以判断理解程度 |
| `INCOMPLETE` | 回答不完整 | 有基础理解，但关键点缺失 |
| `BASIC` | 基础尚可 | 方向正确，但深度不足 |
| `GOOD` | 回答较完整 | 结构较清晰，关键点基本覆盖 |
| `STRONG` | 回答扎实 | 内容完整、表达自然、深度较好 |

### FeedbackBand 判定规则

`band` 不由 LLM 自由输出，而是由服务端根据最终 `score` 做确定性映射：

| score 区间 | band |
|-----------|------|
| `0-29` | `UNCLEAR` |
| `30-49` | `INCOMPLETE` |
| `50-69` | `BASIC` |
| `70-84` | `GOOD` |
| `85-100` | `STRONG` |

---

## 🔐 认证 API

### 1. 用户注册

```http
POST /api/auth/register
```

**请求体**

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "displayName": "philxin"
}
```

**参数校验**

- `email`: 必填，合法邮箱，唯一
- `password`: 必填，长度 `8-64`，至少包含 `大写字母 / 小写字母 / 数字 / 特殊字符` 各一类
- `displayName`: 必填，长度 1-50

**响应示例（HTTP 201）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "displayName": "philxin",
      "targetRole": null
    }
  }
}
```

### 2. 用户登录

```http
POST /api/auth/login
```

**请求体**

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "displayName": "philxin",
      "targetRole": "JAVA_BACKEND"
    }
  }
}
```

### 3. 获取当前用户信息

```http
GET /api/auth/me
```

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "philxin",
    "targetRole": "JAVA_BACKEND"
  }
}
```

---

## 👤 用户训练方向 API

### 1. 更新当前用户训练方向

```http
PATCH /api/users/me/onboarding
```

**请求体**

```json
{
  "targetRole": "JAVA_BACKEND"
}
```

**说明**

- 仅保存训练方向，不做复杂岗位画像
- 不在 V2 实现深度简历解析
- `targetRole` 仅允许使用 `TargetRole` 枚举值

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "targetRole": "JAVA_BACKEND"
  }
}
```

---

## 📚 知识点 API

### KnowledgeResponse

```json
{
  "id": 1,
  "title": "Spring Boot 自动配置",
  "content": "Spring Boot 的自动配置原理...",
  "mastery": 42,
  "tags": ["spring", "backend", "autoconfiguration"],
  "sourceType": "MANUAL",
  "status": "ACTIVE",
  "createdAt": "2026-03-15T10:30:00",
  "updatedAt": "2026-03-15T10:30:00",
  "archivedAt": null
}
```

### 1. 获取知识点列表

```http
GET /api/knowledge
```

**查询参数**

- `page`: 可选，默认 `1`
- `size`: 可选，默认 `20`，最大 `100`
- `keyword`: 可选，按标题/内容模糊匹配
- `tag`: 可选，按单个标签过滤
- `includeArchived`: 可选，默认 `false`

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "title": "Spring Boot 自动配置",
        "content": "Spring Boot 的自动配置原理...",
        "mastery": 42,
        "tags": ["spring", "backend"],
        "sourceType": "BATCH_IMPORT",
        "status": "ACTIVE",
        "createdAt": "2026-03-15T10:30:00",
        "updatedAt": "2026-03-15T10:30:00",
        "archivedAt": null
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "hasNext": false
  }
}
```

### 2. 获取知识点详情

```http
GET /api/knowledge/{id}
```

### 3. 创建单个知识点

```http
POST /api/knowledge
```

**请求体**

```json
{
  "title": "Spring Boot 自动配置",
  "content": "Spring Boot 的自动配置原理...",
  "tags": ["spring", "backend"]
}
```

**标签规范**

- 服务端对 `tags` 执行 `trim + lowercase + de-duplicate`
- 接口返回的标签永远是规范化后的结果

**响应示例（HTTP 201）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "title": "Spring Boot 自动配置",
    "content": "Spring Boot 的自动配置原理...",
    "mastery": 0,
    "tags": ["spring", "backend"],
    "sourceType": "MANUAL",
    "status": "ACTIVE",
    "createdAt": "2026-03-15T10:30:00",
    "updatedAt": "2026-03-15T10:30:00",
    "archivedAt": null
  }
}
```

### 4. 批量导入知识点

```http
POST /api/knowledge/batch-import
```

**请求体**

```json
{
  "items": [
    {
      "title": "Spring Bean 生命周期",
      "content": "Bean 从实例化到销毁的完整过程...",
      "tags": ["spring", "bean"]
    },
    {
      "title": "Redis 持久化",
      "content": "RDB 与 AOF 的差异...",
      "tags": ["redis", "cache"]
    }
  ]
}
```

**响应示例（HTTP 201）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "createdCount": 2,
    "failedCount": 0,
    "failedItems": []
  }
}
```

**`failedItems` 结构**

```json
[
  {
    "index": 1,
    "title": "Redis 持久化",
    "reason": "content 不能为空"
  }
]
```

**返回语义**

- 全部成功：`201`
- 部分成功：`201`
- 全部失败：`422`

### 5. 文件导入知识点（P1）

```http
POST /api/knowledge/file-imports
Content-Type: multipart/form-data
```

**表单字段**

- `file`: 必填，支持少量高价值格式
- `defaultTags`: 可选，逗号分隔

**文件约束**

- 支持格式：`txt`、`md`、`pdf`
- 单文件最大大小：`5MB`
- 解析超时：`60s`

**响应示例（HTTP 202）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "importId": "imp_001",
    "status": "PROCESSING"
  }
}
```

### 6. 查询文件导入状态（P1）

```http
GET /api/knowledge/file-imports/{importId}
```

**导入状态语义**

- `PENDING`
- `PROCESSING`
- `SUCCESS`
- `FAILED`

### 7. 更新知识点

```http
PUT /api/knowledge/{id}
```

**请求体**

```json
{
  "title": "更新后的标题",
  "content": "更新后的内容",
  "tags": ["spring", "advanced"]
}
```

### 8. 删除知识点

```http
DELETE /api/knowledge/{id}
```

**说明**

- V2 中 `DELETE /knowledge/{id}` 语义为**归档知识点**，不做物理删除
- 归档后知识点默认不出现在列表页，但历史训练记录仍保留
- 只允许归档当前用户拥有的知识点

### 9. 获取当前用户标签列表

```http
GET /api/knowledge/tags
```

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": ["spring", "redis", "mysql"]
}
```

标签列表接口返回的值均为规范化后的标签。

---

## 🎯 训练 API

### TrainingSessionResponse

```json
{
  "sessionId": "ts_001",
  "questionId": "q_001",
  "knowledgeId": 1,
  "knowledgeTitle": "Spring Boot 自动配置",
  "question": "请结合一个实际项目，说明 Spring Boot 自动配置是如何生效的？",
  "questionType": "PROJECT",
  "difficulty": "MEDIUM",
  "hintAvailable": true,
  "sequence": {
    "current": 1,
    "total": 1
  }
}
```

### 1. 开始一次训练会话

```http
POST /api/training/sessions
```

**请求体**

```json
{
  "knowledgeId": 1,
  "questionType": "PROJECT",
  "difficulty": "MEDIUM",
  "hintEnabled": true
}
```

**参数说明**

- `knowledgeId`: 必填，且必须属于当前用户
- `questionType`: 可选，不传则由系统决定
- `difficulty`: 可选，默认 `MEDIUM`
- `hintEnabled`: 可选，默认 `true`

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sessionId": "ts_001",
    "questionId": "q_001",
    "knowledgeId": 1,
    "knowledgeTitle": "Spring Boot 自动配置",
    "question": "请结合一个实际项目，说明 Spring Boot 自动配置是如何生效的？",
    "questionType": "PROJECT",
    "difficulty": "MEDIUM",
    "hintAvailable": true,
    "sequence": {
      "current": 1,
      "total": 1
    }
  }
}
```

### 2. 获取问题提示（P1）

```http
POST /api/training/sessions/{sessionId}/questions/{questionId}/hint
```

**说明**

- 只允许访问当前用户自己的训练题目
- 提示只给答题方向、关键词和结构建议，不直接给完整答案
- 同一题目重复请求时，服务端返回首次生成的提示内容

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "hint": "可以从自动配置类加载入口、条件注解和项目中的实际使用三个角度回答。"
  }
}
```

### 3. 提交回答并获取反馈

```http
POST /api/training/sessions/{sessionId}/answers
```

**请求体**

```json
{
  "questionId": "q_001",
  "answer": "在实际项目里，我们会先引入 starter，然后 Spring Boot 会根据 classpath 自动装配 Bean..."
}
```

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "score": 58,
    "band": {
      "code": "BASIC",
      "label": "基础尚可",
      "description": "方向正确，但深度不足。"
    },
    "majorIssue": "关键点缺失，表达尚可，但缺少自动配置生效条件的说明。",
    "missingPoints": [
      "没有说明 @EnableAutoConfiguration 的入口作用",
      "没有解释条件注解如何决定配置是否生效"
    ],
    "betterAnswerApproach": [
      "先讲自动配置入口，再讲候选配置类加载，再讲条件装配",
      "最后补一个项目中的实际例子"
    ],
    "naturalExampleAnswer": "我一般会从两个层面回答。第一，Spring Boot 启动时会通过自动配置入口去加载候选配置类；第二，这些配置类并不会全部生效，而是要看条件注解是否满足。实际项目里，我们经常只是引入 starter，但真正生效的 Bean 还是取决于当前依赖和配置。",
    "weakTags": ["spring", "autoconfiguration"],
    "masteryBefore": 42,
    "masteryAfter": 47
  }
}
```

### 4. 获取训练历史列表

```http
GET /api/training/sessions
```

**查询参数**

- `page`: 可选，默认 `1`
- `size`: 可选，默认 `20`，最大 `100`
- `knowledgeId`: 可选，仅查询某个知识点的训练历史

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "sessionId": "ts_001",
        "knowledgeId": 1,
        "knowledgeTitle": "Spring Boot 自动配置",
        "questionCount": 1,
        "answeredCount": 1,
        "sessionScore": 58,
        "band": {
          "code": "BASIC",
          "label": "基础尚可"
        },
        "majorIssueSummary": "关键点缺失，表达尚可。",
        "startedAt": "2026-03-15T11:00:00",
        "completedAt": "2026-03-15T11:03:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "hasNext": false
  }
}
```

**聚合规则**

- `questionCount`：该 session 的总题数
- `answeredCount`：已提交回答的题数
- `sessionScore`：所有已回答题目的 `score` 平均值，四舍五入取整
- `band`：服务端基于 `sessionScore` 按 `FeedbackBand` 规则映射
- `majorIssueSummary`：
  - P0 单题模式下，等于该题 `majorIssue`
  - 多题模式下，由服务端汇总为 session 级摘要

### 5. 获取单次训练详情

```http
GET /api/training/sessions/{sessionId}
```

**说明**

- 返回问题、用户回答、完整反馈结果、训练时间
- 只允许访问当前用户自己的训练记录

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sessionId": "ts_001",
    "knowledgeId": 1,
    "knowledgeTitle": "Spring Boot 自动配置",
    "questionCount": 1,
    "answeredCount": 1,
    "sessionScore": 58,
    "band": {
      "code": "BASIC",
      "label": "基础尚可",
      "description": "方向正确，但深度不足。"
    },
    "majorIssueSummary": "关键点缺失，表达尚可。",
    "startedAt": "2026-03-15T11:00:00",
    "completedAt": "2026-03-15T11:03:00",
    "questions": [
      {
        "questionId": "q_001",
        "orderNo": 1,
        "parentQuestionId": null,
        "questionType": "PROJECT",
        "difficulty": "MEDIUM",
        "question": "请结合一个实际项目，说明 Spring Boot 自动配置是如何生效的？",
        "hintAvailable": false,
        "hintText": "可以先讲自动配置入口，再讲条件装配，最后补一个项目中的实际例子。",
        "hintUsed": true,
        "answer": "在实际项目里，我们会先引入 starter...",
        "feedback": {
          "score": 58,
          "band": {
            "code": "BASIC",
            "label": "基础尚可",
            "description": "方向正确，但深度不足。"
          },
          "majorIssue": "关键点缺失，表达尚可，但缺少自动配置生效条件的说明。",
          "missingPoints": [
            "没有说明 @EnableAutoConfiguration 的入口作用",
            "没有解释条件注解如何决定配置是否生效"
          ],
          "betterAnswerApproach": [
            "先讲自动配置入口，再讲候选配置类加载，再讲条件装配",
            "最后补一个项目中的实际例子"
          ],
          "naturalExampleAnswer": "我一般会从两个层面回答...",
          "weakTags": ["spring", "autoconfiguration"],
          "masteryBefore": 42,
          "masteryAfter": 47
        }
      }
    ]
  }
}
```

---

## 📊 Dashboard / Progress API

### 1. 获取首页概览（P1）

```http
GET /api/dashboard/overview
```

**响应目标**

- 最近训练概览
- 当前薄弱知识点 Top N
- 最近掌握度变化摘要

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "weakKnowledgeItems": [
      {
        "knowledgeId": 1,
        "title": "Spring Boot 自动配置",
        "mastery": 42,
        "tags": ["spring", "backend"]
      }
    ],
    "recentTrainings": [
      {
        "sessionId": "ts_001",
        "knowledgeId": 1,
        "knowledgeTitle": "Spring Boot 自动配置",
        "sessionScore": 58,
        "band": {
          "code": "BASIC",
          "label": "基础尚可"
        },
        "completedAt": "2026-03-15T11:03:00"
      }
    ],
    "progressSummary": {
      "trainedCountLast7Days": 6,
      "averageScoreLast7Days": 64,
      "improvedKnowledgeCount": 2
    }
  }
}
```

### 2. 获取今日推荐训练包（P2）

```http
GET /api/training/recommendations/today
```

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "packageId": "pkg_today_001",
    "title": "今日推荐练习",
    "items": [
      {
        "knowledgeId": 1,
        "questionType": "SCENARIO",
        "difficulty": "MEDIUM"
      },
      {
        "knowledgeId": 5,
        "questionType": "PROJECT",
        "difficulty": "MEDIUM"
      }
    ]
  }
}
```

### 3. 获取回练提醒（P2）

```http
GET /api/dashboard/review-reminders
```

**说明**

- 服务端根据掌握度、近期训练得分和训练间隔动态计算 `reviewWeight`
- 返回建议的回练题型和难度，供首页一键发起训练

**响应示例（HTTP 200）**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "knowledgeId": 1,
        "knowledgeTitle": "Spring Boot 自动配置",
        "reviewWeight": 82,
        "reason": "掌握度和最近得分都偏低，建议优先回练巩固关键点。",
        "suggestedQuestionType": "FUNDAMENTAL",
        "suggestedDifficulty": "EASY",
        "lastTrainedAt": "2026-03-10T09:00:00",
        "tags": ["spring", "backend"]
      }
    ],
    "generatedAt": "2026-03-18T10:00:00"
  }
}
```

---

## ⚠️ 错误码说明

| Code | 说明 | 示例场景 |
|------|------|----------|
| 0 | 成功 | 请求正常处理 |
| 400 | 参数错误 | 必填参数缺失、字段格式不合法 |
| 401 | 未认证 | 未携带 token 或 token 无效 |
| 403 | 无权限 | 访问不允许的资源 |
| 404 | 资源不存在 | 知识点不存在或不属于当前用户 |
| 409 | 冲突 | 邮箱已注册 |
| 413 | 请求体过大 | 上传文件超出大小限制 |
| 422 | 业务校验失败 | 批量导入存在非法数据 |
| 429 | 请求过快 | LLM 调用限流 |
| 500 | 系统内部错误 | 未预期异常 |
| 502 | 上游服务错误 | LLM 服务异常、响应解析失败 |
| 504 | 上游超时 | 文件解析或 LLM 调用超时 |

---

## 🔒 安全约束

1. 除 `/auth/**` 外，接口全部要求 Bearer Token
2. 用户资源按 owner 隔离，禁止通过 ID 访问他人知识点和训练记录
3. 上传接口限制文件大小、文件类型与解析超时
4. Controller 只做协议与校验，鉴权与 owner 校验应下沉到 Security / Service
5. 日志不得打印密码、token、原始敏感回答内容

---

## 🧪 验收建议

### P0 必测

1. 注册 / 登录成功，并能获取 `me`
2. 用户 A 无法访问用户 B 的知识点与训练记录
3. 知识点批量导入成功，标签字段可正常回显
4. 训练会话可按题型和难度启动
5. 提交回答后，结果页结构符合新契约

### P1 回归

1. 提示接口只给思路，不直接给完整答案
2. Dashboard 可返回薄弱项与最近训练摘要
3. 页面主链路无 401/403 误报与明显卡顿

### P2 预留

1. 今日推荐训练包可基于薄弱项生成
2. 自动组题不与 P0 契约冲突
3. 回练提醒可输出权重和回练入口建议

---

## 📝 Postman 分组建议

- Auth
- User
- Knowledge
- Training
- Dashboard

建议在 Postman 环境中预置：

- `base_url = http://localhost:8080/api`
- `token = <jwt>`

后续如进入实现阶段，本接口文档应优先于现有前端 API 定义与 DTO 命名，前后端契约统一以本文件为准。
