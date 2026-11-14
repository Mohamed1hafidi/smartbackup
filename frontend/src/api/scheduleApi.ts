import { api } from './axiosInstance'
import type { ApiResponse, PageResponse } from '../types/common.types'
import type {
  CreateSchedulePayload,
  Schedule,
  ScheduleExecution,
  ScheduleListResult,
  UpdateSchedulePayload,
} from '../types/schedule.types'

export const scheduleApi = {
  list: async () => {
    const { data } = await api.get<ScheduleListResult>('/schedules')
    return data
  },
  get: async (id: string) => {
    const { data } = await api.get<ApiResponse<Schedule>>(`/schedules/${id}`)
    return data.data
  },
  create: async (payload: CreateSchedulePayload) => {
    const { data } = await api.post<ApiResponse<Schedule>>('/schedules', payload)
    return data.data
  },
  update: async (id: string, payload: UpdateSchedulePayload) => {
    const { data } = await api.put<ApiResponse<Schedule>>(`/schedules/${id}`, payload)
    return data.data
  },
  toggle: async (id: string) => {
    const { data } = await api.patch<ApiResponse<Schedule>>(`/schedules/${id}/toggle`)
    return data.data
  },
  remove: async (id: string) => {
    await api.delete(`/schedules/${id}`)
  },
  executions: async (id: string, page = 0, size = 20) => {
    const { data } = await api.get<ApiResponse<PageResponse<ScheduleExecution>>>(
      `/schedules/${id}/executions`,
      { params: { page, size } },
    )
    return data.data
  },
}
