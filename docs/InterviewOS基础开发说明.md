# 🧠 InterviewOS — 基础开发说明（V1 Baseline）

* * *

# 一、项目目标（当前阶段）

InterviewOS 是一个 AI 驱动的技术面试训练系统。

V1 目标：

> 实现完整训练闭环：  
> 创建知识点 → AI 提问 → 用户回答 → AI 评分 → 更新掌握度 → 查看历史记录

当前阶段：

* 不做用户系统

* 不做权限控制

* 不做向量数据库

* 不做缓存

* 不做多轮对话

* 不做复杂统计


只做单人本地/线上可运行系统。

* * *

# 二、项目结构

Plain textinterview-os/  
├── interview-os-backend/  
└── interview-os-frontend/

* * *

# 三、后端技术栈

* Java 21

* Spring Boot 3.3.x

* Maven

* Spring Web

* Spring Data JPA

* PostgreSQL

* Lombok

* Jackson

* Spring Validation


AI 接入：

* 外部 LLM HTTP API

* 自定义 LLMService 抽象层


* * *

# 四、基础架构约束（必须遵守）

* * *

## 1️⃣ 统一返回结构

所有接口返回：

JSON{  
"code": 0,  
"message": "success",  
"data": {}  
}

错误时：

JSON{  
"code": 500,  
"message": "error message",  
"data": null  
}

* * *

## 2️⃣ 全局异常处理

必须使用：

* @RestControllerAdvice

* 统一封装错误响应

* 禁止在 Controller 写 try-catch


* * *

## 3️⃣ LLM 抽象层设计

必须使用接口：

Javapublic interface LLMService {

    String generateQuestion(String knowledgeTitle, String knowledgeContent);  
  
    EvaluationResult evaluateAnswer(String question, String userAnswer);  

}

禁止：

* 在 Controller 直接调用 HTTP

* 在 Service 层直接写模型 URL


* * *

## 4️⃣ 日志要求

必须记录：

* prompt 内容

* LLM 原始返回

* 解析后 JSON

* 错误日志


* * *

# 五、数据库设计（V1 固定结构）

* * *

## 表一：knowledge

| 字段 | 类型 |
| --- | --- |
| id | bigint |
| title | varchar |
| content | text |
| mastery | int |
| created_at | timestamp |

* * *

## 表二：training_record

| 字段 | 类型 |
| --- | --- |
| id | bigint |
| knowledge_id | bigint |
| question | text |
| answer | text |
| accuracy | int |
| depth | int |
| clarity | int |
| overall | int |
| created_at | timestamp |

* * *

# 六、核心业务流程

* * *

## 1️⃣ 发起训练

接口：

POST /training/start

输入：

knowledgeId

逻辑：

* 查询知识点

* 调用 LLMService.generateQuestion()

* 返回问题


* * *

## 2️⃣ 提交回答

接口：

POST /training/submit

输入：

knowledgeId  
question  
answer

逻辑：

1. 调用 LLMService.evaluateAnswer()

2. 后端计算 overall 分数

3. 更新 knowledge.mastery


公式：

newMastery = oldMastery * 0.7 + overall * 0.3

4. 保存 training_record

5. 返回完整评分结果


* * *

# 七、AI 评分返回结构（固定协议）

模型必须返回 JSON：

JSON{  
"accuracy": 0,  
"depth": 0,  
"clarity": 0,  
"strengths": "",  
"weaknesses": "",  
"suggestions": [],  
"example_answer": ""  
}

后端负责：

* 校验 JSON

* 限制数值范围 0-100

* 计算 overall


* * *

# 八、前端要求（V1）

使用 Vue 3 + TypeScript。

页面只包含：

1. 知识点列表页

2. 新建知识点页

3. 训练页

4. 评分结果页

5. 训练记录页


禁止：

* 动画复杂化

* 设计系统优化

* 组件抽象过度


* * *

# 九、当前阶段禁止扩展

在 V1 完成前禁止：

* 向量数据库

* Redis

* 用户系统

* JWT

* 多轮追问

* 复杂能力模型

* 报表系统

* 图谱


* * *

# 十、V1 完成标准

满足以下条件即完成：

* 可以创建知识点

* 可以发起训练

* AI 可以评分

* 掌握度更新

* 可查看历史记录


* * *

# 十一、开发优先级顺序

1. 基础项目初始化

2. LLMService 抽象实现

3. 训练提交接口

4. 数据持久化

5. 前端接入


* * *

# 十二、代码风格约束

* Controller 只负责接收参数

* Service 负责业务逻辑

* Repository 只负责数据库

* 禁止业务逻辑写在 Controller

* 禁止魔法字符串

* 禁止写死模型 URL


* * *

# 🎯 当前开发阶段定位

这是一个：

> 单人可控、结构清晰、可扩展的 AI 项目基础版

目标不是复杂，而是：

> 稳定、清晰、可演进

* * *