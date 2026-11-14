import { api } from './axiosInstance'
import type { ApiResponse } from '../types/common.types'
import type {
  AuthResponse,
  ChangePasswordPayload,
  LoginPayload,
  RegisterPayload,
  UpdateProfilePayload,
  UserProfile,
} from '../types/auth.types'

export const authApi = {
  register: async (payload: RegisterPayload) => {
    const { data } = await api.post<ApiResponse<UserProfile>>('/auth/register', payload)
    return data
  },
  login: async (payload: LoginPayload) => {
    const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/login', payload)
    return data.data
  },
  refresh: async (refreshToken: string) => {
    const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken })
    return data.data
  },
  logout: async (refreshToken: string) => {
    await api.post<ApiResponse<void>>('/auth/logout', { refreshToken })
  },
  updateProfile: async (payload: UpdateProfilePayload) => {
    const { data } = await api.put<ApiResponse<UserProfile>>('/users/me', payload)
    return data.data
  },
  changePassword: async (payload: ChangePasswordPayload) => {
    const { data } = await api.put<ApiResponse<void>>('/users/me/password', payload)
    return data
  },
}
