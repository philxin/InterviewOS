import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import type { ApiResult } from '../types'

const apiClient: AxiosInstance = axios.create({
  // 生产环境通过 VITE_API_BASE_URL 覆盖，默认指向本地后端。
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response
  },
  (error) => {
    const status = error.response?.status as number | undefined
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/'
    }
    const message = error.response?.data?.message || error.message || 'Request failed'
    return Promise.reject(new Error(`[${status ?? 'NETWORK'}] ${message}`))
  }
)

function unwrapResult<T>(response: AxiosResponse<ApiResult<T>>): T {
  const payload = response.data
  if (!payload || typeof payload.code !== 'number') {
    throw new Error('Invalid API response format')
  }
  if (payload.code !== 0) {
    throw new Error(`[${payload.code}] ${payload.message || 'Request failed'}`)
  }
  return payload.data
}

const http = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return apiClient.get<ApiResult<T>>(url, config).then(unwrapResult)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return apiClient.post<ApiResult<T>>(url, data, config).then(unwrapResult)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return apiClient.put<ApiResult<T>>(url, data, config).then(unwrapResult)
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return apiClient.delete<ApiResult<T>>(url, config).then(unwrapResult)
  },
}

export default http
