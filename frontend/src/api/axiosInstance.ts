import axios, { type InternalAxiosRequestConfig } from 'axios'
import { API_BASE_URL } from './config'
import type { ApiResponse } from '../types/common.types'
import type { AuthResponse } from '../types/auth.types'
import { useAuthStore } from '../store/authStore'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  headers: { Accept: 'application/json' },
})

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20_000,
  headers: { Accept: 'application/json' },
})

let refreshPromise: Promise<string | null> | null = null

const isAuthEndpoint = (url?: string) => Boolean(url?.includes('/auth/'))

async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken, setTokens, setUser, clearSession } = useAuthStore.getState()
  if (!refreshToken) {
    clearSession()
    return null
  }

  try {
    const { data } = await refreshClient.post<ApiResponse<AuthResponse>>('/auth/refresh', {
      refreshToken,
    })
    setTokens(data.data.accessToken, data.data.refreshToken)
    setUser(data.data.user)
    return data.data.accessToken
  } catch {
    clearSession()
    return null
  }
}

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const status = error.response?.status

    if (status === 401 && original && !original._retry && !isAuthEndpoint(original.url)) {
      original._retry = true
      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null
      })
      const token = await refreshPromise
      if (token) {
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      }
    }

    return Promise.reject(error)
  },
)
