/**
 * 后端统一响应包装：成功 code=0，错误 code=4xx/5xx。
 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/**
 * 知识点实体（对应 /knowledge）。
 */
export interface Knowledge {
  id: number
  title: string
  content: string
  mastery: number
  createdAt: string
}

export interface CreateKnowledgeRequest {
  title: string
  content: string
}

export interface UpdateKnowledgeRequest {
  title: string
  content: string
}

/**
 * 提交评分结果（对应 /training/submit）。
 */
export interface EvaluationResult {
  accuracy: number
  depth: number
  clarity: number
  overall: number
  strengths: string
  weaknesses: string
  suggestions: string[]
  exampleAnswer: string
}

export interface StartTrainingRequest {
  knowledgeId: number
}

export interface StartTrainingResponse {
  question: string
}

export interface SubmitTrainingRequest {
  knowledgeId: number
  question: string
  answer: string
}

/**
 * 训练历史记录（对应 /training/history*）。
 */
export interface TrainingRecord {
  id: number
  knowledgeId: number
  question: string
  answer: string
  accuracy: number
  depth: number
  clarity: number
  overall: number
  strengths: string
  weaknesses: string
  suggestions: string[]
  exampleAnswer: string
  createdAt: string
}
