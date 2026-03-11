import apiClient from './client'
import type {
  CreateKnowledgeRequest,
  EvaluationResult,
  Knowledge,
  StartTrainingRequest,
  StartTrainingResponse,
  SubmitTrainingRequest,
  TrainingRecord,
  UpdateKnowledgeRequest,
} from '../types'

/**
 * 知识点相关 API（与后端 /knowledge 契约保持一致）。
 */
export const knowledgeAPI = {
  getList: () => apiClient.get<Knowledge[]>('/knowledge'),
  getById: (id: number) => apiClient.get<Knowledge>(`/knowledge/${id}`),
  create: (data: CreateKnowledgeRequest) => apiClient.post<Knowledge>('/knowledge', data),
  update: (id: number, data: UpdateKnowledgeRequest) =>
    apiClient.put<Knowledge>(`/knowledge/${id}`, data),
  delete: (id: number) => apiClient.delete<void>(`/knowledge/${id}`),
}

/**
 * 训练流程 API（start/submit/history）。
 */
export const trainingAPI = {
  start: (payload: StartTrainingRequest) =>
    apiClient.post<StartTrainingResponse>('/training/start', payload),
  submit: (payload: SubmitTrainingRequest) =>
    apiClient.post<EvaluationResult>('/training/submit', payload),
  getHistory: (knowledgeId: number) =>
    apiClient.get<TrainingRecord[]>(`/training/history/${knowledgeId}`),
  getAllHistory: () => apiClient.get<TrainingRecord[]>('/training/history'),
}
