# API 接口设计 - V1

## 📋 概述

所有 API 遵循统一响应包结构：

**成功响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

**错误响应**：
```json
{
  "code": 400,
  "message": "error message",
  "data": null
}
```

**响应语义冻结（V1）**：
- 成功请求：使用 `2xx`，且 `code = 0`
- 失败请求：使用 `4xx/5xx`，且 `code` 与错误类型一致（如 `400/404/500/502`）
- 禁止“业务失败仍返回 200”

---

## 🔗 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8
- **认证要求**: V1 无鉴权（无需 JWT）

---

## 🔒 V1 契约冻结（不允许随意变更）

固定端点（V1）：

- `GET /api/knowledge`
- `GET /api/knowledge/{id}`
- `POST /api/knowledge`
- `PUT /api/knowledge/{id}`
- `DELETE /api/knowledge/{id}`
- `POST /api/training/start`
- `POST /api/training/submit`
- `GET /api/training/history/{knowledgeId}`
- `GET /api/training/history`

固定字段约束（V1）：

- `Knowledge.createdAt` 使用 ISO-8601 字符串
- `TrainingRecord.suggestions` 在 API 返回中固定为 `string[]`（非 JSON 字符串）
- `EvaluationResult.exampleAnswer` 固定为 camelCase（`exampleAnswer`）

---

## 📚 知识点管理 API

### 1. 获取所有知识点

```
GET /api/knowledge
```

**请求参数**: 无

**响应示例（HTTP 201）**：
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "Spring Boot 自动配置",
      "content": "Spring Boot 的自动配置是其核心功能...",
      "mastery": 75,
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "id": 2,
      "title": "Vue 3 响应式系统",
      "content": "Vue 3 使用 Proxy 实现响应式...",
      "mastery": 45,
      "createdAt": "2024-01-14T15:20:00"
    }
  ]
}
```

---

### 2. 获取单个知识点

```
GET /api/knowledge/{id}
```

**路径参数**:
- `id`: 知识点 ID（Long）

**响应示例（HTTP 200）**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "title": "Spring Boot 自动配置",
    "content": "Spring Boot 的自动配置是其核心功能...",
    "mastery": 75,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**错误响应**：
```json
{
  "code": 404,
  "message": "Knowledge not found with id: 1",
  "data": null
}
```

---

### 3. 创建知识点

```
POST /api/knowledge
```

**请求体**：
```json
{
  "title": "Spring Boot 自动配置",
  "content": "Spring Boot 的自动配置原理..."
}
```

**参数校验**：
- `title`: 必填，长度 1-200
- `content`: 必填

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "title": "Spring Boot 自动配置",
    "content": "Spring Boot 的自动配置原理...",
    "mastery": 0,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

---

### 4. 更新知识点

```
PUT /api/knowledge/{id}
```

**路径参数**:
- `id`: 知识点 ID（Long）

**请求体**：
```json
{
  "title": "更新后的标题",
  "content": "更新后的内容"
}
```

**参数校验**：
- `title`: 必填，长度 1-200
- `content`: 必填

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "title": "更新后的标题",
    "content": "更新后的内容",
    "mastery": 75,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

---

### 5. 删除知识点

```
DELETE /api/knowledge/{id}
```

**路径参数**:
- `id`: 知识点 ID（Long）

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**注意**: 删除知识点会级联删除其所有训练记录。

---

## 🎯 训练流程 API

### 1. 开始训练（生成问题）

```
POST /api/training/start
```

**请求体**：
```json
{
  "knowledgeId": 1
}
```

**参数校验**：
- `knowledgeId`: 必填，知识点必须存在

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "question": "请解释 Spring Boot 的自动配置原理，并说明 @EnableAutoConfiguration 注解的作用。"
  }
}
```

**业务逻辑**：
1. 查询知识点
2. 调用 LLM 生成问题
3. 返回问题文本

---

### 2. 提交回答（AI 评分）

```
POST /api/training/submit
```

**请求体**：
```json
{
  "knowledgeId": 1,
  "question": "请解释 Spring Boot 的自动配置原理...",
  "answer": "Spring Boot 通过 @EnableAutoConfiguration 注解实现自动配置。它会扫描 classpath 下的所有 META-INF/spring.factories 文件..."
}
```

**参数校验**：
- `knowledgeId`: 必填，知识点必须存在
- `question`: 必填
- `answer`: 必填

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accuracy": 80,
    "depth": 65,
    "clarity": 82,
    "overall": 75,
    "strengths": "回答结构清晰，准确提到了关键概念和注解",
    "weaknesses": "对条件注解的工作机制理解不够深入，缺少具体例子",
    "suggestions": [
      "深入学习 @Conditional 系列注解的工作原理",
      "了解 AutoConfigurationImportSelector 的选择逻辑",
      "结合实际项目中的自动配置案例进行理解"
    ],
    "exampleAnswer": "Spring Boot 的自动配置原理...\n\n关键点：\n1. @EnableAutoConfiguration 导入 AutoConfigurationImportSelector\n2. 从 META-INF/spring.factories 加载候选配置类\n3. @Conditional 注解决定配置类是否生效..."
  }
}
```

**业务逻辑**：
1. 调用 LLM 评估回答
2. 计算 overall 分数
3. 更新知识点掌握度：`newMastery = oldMastery * 0.7 + overall * 0.3`
4. 保存训练记录
5. 返回完整评分结果

---

### 3. 获取训练历史（指定知识点）

```
GET /api/training/history/{knowledgeId}
```

**路径参数**:
- `knowledgeId`: 知识点 ID（Long）

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "knowledgeId": 1,
      "question": "请解释 Spring Boot 的自动配置原理...",
      "answer": "Spring Boot 通过...",
      "accuracy": 80,
      "depth": 65,
      "clarity": 82,
      "overall": 75,
      "strengths": "回答结构清晰",
      "weaknesses": "对条件注解理解不够深入",
      "suggestions": ["深入学习 @Conditional"],
      "exampleAnswer": "示例回答...",
      "createdAt": "2024-01-15T10:35:00"
    },
    {
      "id": 2,
      "knowledgeId": 1,
      "question": "什么是 Spring Boot Starter？",
      "answer": "Starter 是一组依赖集合...",
      "accuracy": 70,
      "depth": 60,
      "clarity": 75,
      "overall": 68,
      "strengths": "概念理解正确",
      "weaknesses": "缺少实际应用场景",
      "suggestions": ["结合实际项目使用"],
      "exampleAnswer": "示例回答...",
      "createdAt": "2024-01-14T16:20:00"
    }
  ]
}
```

---

### 4. 获取所有训练历史

```
GET /api/training/history
```

**请求参数**: 无

**响应示例（HTTP 200）**：
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 5,
      "knowledgeId": 2,
      "question": "Vue 3 的响应式系统有什么改进？",
      "answer": "Vue 3 使用 Proxy 替代了 Vue 2 的 Object.defineProperty...",
      "accuracy": 85,
      "depth": 75,
      "clarity": 90,
      "overall": 83,
      "strengths": "对比清晰，提到了核心改进",
      "weaknesses": "",
      "suggestions": ["可以补充一些使用场景"],
      "exampleAnswer": "示例回答...",
      "createdAt": "2024-01-15T11:00:00"
    },
    {
      "id": 1,
      "knowledgeId": 1,
      "question": "请解释 Spring Boot 的自动配置原理...",
      "answer": "Spring Boot 通过...",
      "accuracy": 80,
      "depth": 65,
      "clarity": 82,
      "overall": 75,
      "strengths": "回答结构清晰",
      "weaknesses": "对条件注解理解不够深入",
      "suggestions": ["深入学习 @Conditional"],
      "exampleAnswer": "示例回答...",
      "createdAt": "2024-01-15T10:35:00"
    }
  ]
}
```

---

## ⚠️ 错误码说明

| Code | 说明 | 示例场景 |
|------|------|----------|
| 0 | 成功 | 请求正常处理 |
| 400 | 参数错误 | 必填参数缺失、参数格式错误 |
| 404 | 资源不存在 | 知识点不存在 |
| 500 | 系统内部错误 | 未预期异常、数据库运行时错误 |
| 502 | 上游服务错误 | LLM 服务异常、响应不可解析 |

---

## 🔒 V1 认证说明

V1 **不实现认证机制**，所有接口无需鉴权即可访问。

这在 V2 版本会通过 JWT 方式改进。

---

## 📝 Postman 集合示例

```json
{
  "info": {
    "name": "InterviewOS V1 API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Knowledge",
      "item": [
        { "name": "Get All", "request": { "method": "GET", "url": "{{base_url}}/knowledge" } },
        { "name": "Get By ID", "request": { "method": "GET", "url": "{{base_url}}/knowledge/1" } },
        { "name": "Create", "request": { "method": "POST", "url": "{{base_url}}/knowledge" } },
        { "name": "Update", "request": { "method": "PUT", "url": "{{base_url}}/knowledge/1" } },
        { "name": "Delete", "request": { "method": "DELETE", "url": "{{base_url}}/knowledge/1" } }
      ]
    },
    {
      "name": "Training",
      "item": [
        { "name": "Start", "request": { "method": "POST", "url": "{{base_url}}/training/start" } },
        { "name": "Submit", "request": { "method": "POST", "url": "{{base_url}}/training/submit" } },
        { "name": "Get History", "request": { "method": "GET", "url": "{{base_url}}/training/history/1" } },
        { "name": "Get All History", "request": { "method": "GET", "url": "{{base_url}}/training/history" } }
      ]
    }
  ],
  "variable": [
    { "key": "base_url", "value": "http://localhost:8080/api" }
  ]
}
```
