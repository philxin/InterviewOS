import apiClient from './client'
import type {
  AuthResponse,
  AuthUser,
  BatchImportKnowledgeRequest,
  BatchImportKnowledgeResponse,
  CreateKnowledgeRequest,
  DashboardOverview,
  PagedResult,
  EvaluationResult,
  Knowledge,
  LoginRequest,
  RegisterRequest,
  StartTrainingSessionRequest,
  SubmitSessionAnswerRequest,
  TrainingHintResponse,
  StartTrainingRequest,
  StartTrainingResponse,
  SubmitTrainingRequest,
  TrainingFeedback,
  TrainingSessionDetail,
  TrainingSessionStartResponse,
  TrainingSessionSummary,
  TrainingRecord,
  UpdateOnboardingRequest,
  UpdateKnowledgeRequest,
  UserOnboardingResponse,
} from '../types'

export const authAPI = {
  register: (data: RegisterRequest) => apiClient.post<AuthResponse>('/auth/register', data),
  login: (data: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', data),
  me: () => apiClient.get<AuthUser>('/auth/me'),
}

export const userAPI = {
  updateOnboarding: (data: UpdateOnboardingRequest) =>
    apiClient.patch<UserOnboardingResponse>('/users/me/onboarding', data),
}

export const dashboardAPI = {
  getOverview: () => apiClient.get<DashboardOverview>('/dashboard/overview'),
}

/**
 * 知识点相关 API（与后端 /knowledge 契约保持一致）。
 */
export const knowledgeAPI = {
  getList: () => apiClient.get<Knowledge[]>('/knowledge'),
  getById: (id: number) => apiClient.get<Knowledge>(`/knowledge/${id}`),
  getTags: () => apiClient.get<string[]>('/knowledge/tags'),
  create: (data: CreateKnowledgeRequest) => apiClient.post<Knowledge>('/knowledge', data),
  update: (id: number, data: UpdateKnowledgeRequest) =>
    apiClient.put<Knowledge>(`/knowledge/${id}`, data),
  batchImport: (data: BatchImportKnowledgeRequest) =>
    apiClient.post<BatchImportKnowledgeResponse>('/knowledge/batch-import', data),
  delete: (id: number) => apiClient.delete<void>(`/knowledge/${id}`),
}

/**
 * 训练流程 API（start/submit/history）。
 */
export const trainingAPI = {
  startSession: (payload: StartTrainingSessionRequest) =>
    apiClient.post<TrainingSessionStartResponse>('/training/sessions', payload),
  getHint: (sessionId: string, questionId: string) =>
    apiClient.post<TrainingHintResponse>(`/training/sessions/${sessionId}/questions/${questionId}/hint`),
  submitAnswer: (sessionId: string, payload: SubmitSessionAnswerRequest) =>
    apiClient.post<TrainingFeedback>(`/training/sessions/${sessionId}/answers`, payload),
  getSessions: (params?: { knowledgeId?: number; page?: number; size?: number }) =>
    apiClient.get<PagedResult<TrainingSessionSummary>>('/training/sessions', { params }),
  getSessionDetail: (sessionId: string) =>
    apiClient.get<TrainingSessionDetail>(`/training/sessions/${sessionId}`),

  start: (payload: StartTrainingRequest) =>
    apiClient.post<StartTrainingResponse>('/training/start', payload),
  submit: (payload: SubmitTrainingRequest) =>
    apiClient.post<EvaluationResult>('/training/submit', payload),
  getHistory: (knowledgeId: number) =>
    apiClient.get<TrainingRecord[]>(`/training/history/${knowledgeId}`),
  getAllHistory: () => apiClient.get<TrainingRecord[]>('/training/history'),
}
