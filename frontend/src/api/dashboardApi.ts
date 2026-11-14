import { api } from './axiosInstance'
import type { ApiResponse } from '../types/common.types'
import type { DashboardStats } from '../types/dashboard.types'

export const dashboardApi = {
  stats: async () => {
    const { data } = await api.get<ApiResponse<DashboardStats>>('/dashboard/stats')
    return data.data
  },
}
