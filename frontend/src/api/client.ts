import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import type { ApiResult } from '../types'
import { getStoredToken, resetAuthSession } from '../utils/auth'

export class ApiRequestError extends Error {
  status?: number
  payload?: unknown

  constructor(message: string, status?: number, payload?: unknown) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.payload = payload
  }
}

const defaultBaseURL = import.meta.env.DEV ? 'http://localhost:8080/api' : '/api'

const apiClient: AxiosInstance = axios.create({
  // 开发环境默认直连本地后端；生产环境默认走同域反向代理的 /api。
  baseURL: import.meta.env.VITE_API_BASE_URL || defaultBaseURL,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use(
  (config) => {
    const token = getStoredToken()
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
    const requestUrl = String(error.config?.url ?? '')
    const isAuthEntry = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register')

    if (status === 401) {
      resetAuthSession()
      if (!isAuthEntry && typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(`${window.location.pathname}${window.location.search}`)
        window.location.assign(`/login?redirect=${redirect}`)
      }
    }
    const message = error.response?.data?.message || error.message || 'Request failed'
    return Promise.reject(new ApiRequestError(
      `[${status ?? 'NETWORK'}] ${message}`,
      status,
      error.response?.data?.data
    ))
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
  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return apiClient.patch<ApiResult<T>>(url, data, config).then(unwrapResult)
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return apiClient.delete<ApiResult<T>>(url, config).then(unwrapResult)
  },
}

export default http
