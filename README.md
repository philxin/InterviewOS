# InterviewOS

AI驱动的面试认知训练系统

## 项目结构

```
interviewos/
│  
├── backend/            # Spring Boot (Maven) 后端项目
│  
├── frontend/           # Vue 3 + Vite 前端项目
│  
├── docs/               # 项目文档
│  
├── docker/             # Docker 相关配置
│  
├── .gitignore
└── README.md
```

## 技术栈

### 后端
- Java 21
- Spring Boot 3.3.x
- Spring AI 1.0.x
- PostgreSQL 14+ with pgvector
- Redis 6+

### 前端
- Vue 3.4+
- TypeScript 5.6
- Vite 5.4
- Tailwind CSS 4.1
- ECharts 5.x

## 快速开始

### 后端

```bash
cd backend
./mvnw spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

## 文档

详细文档请查看 [docs/](./docs/) 目录。