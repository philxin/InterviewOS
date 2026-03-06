import apiClient from './client'
import type { User, Interview, Training, Dashboard } from '../types'

export const authAPI = {
  login: (email: string, password: string) =>
    apiClient.post('/auth/login', { email, password }),

  register: (name: string, email: string, password: string) =>
    apiClient.post('/auth/register', { name, email, password }),

  logout: () => apiClient.post('/auth/logout'),

  getCurrentUser: () => apiClient.get<User>('/auth/me'),
}

export const interviewAPI = {
  getList: () => apiClient.get<Interview[]>('/interviews'),

  getById: (id: string) => apiClient.get<Interview>(`/interviews/${id}`),

  create: (data: Partial<Interview>) => apiClient.post<Interview>('/interviews', data),

  update: (id: string, data: Partial<Interview>) =>
    apiClient.put<Interview>(`/interviews/${id}`, data),

  delete: (id: string) => apiClient.delete(`/interviews/${id}`),
}

export const trainingAPI = {
  getList: () => apiClient.get<Training[]>('/trainings'),

  getById: (id: string) => apiClient.get<Training>(`/trainings/${id}`),

  create: (data: Partial<Training>) => apiClient.post<Training>('/trainings', data),

  update: (id: string, data: Partial<Training>) =>
    apiClient.put<Training>(`/trainings/${id}`, data),

  delete: (id: string) => apiClient.delete(`/trainings/${id}`),
}

export const dashboardAPI = {
  getData: () => apiClient.get<Dashboard>('/dashboard'),
}