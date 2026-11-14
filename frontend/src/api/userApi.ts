import { api } from './axiosInstance'
import type { ApiResponse } from '../types/common.types'
import type { UserProfile } from '../types/auth.types'

export const userApi = {
  me: async () => {
    const { data } = await api.get<ApiResponse<UserProfile>>('/users/me')
    return data.data
  },
}
