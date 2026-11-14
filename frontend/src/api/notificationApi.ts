import { api } from './axiosInstance'
import type { ApiResponse } from '../types/common.types'
import type {
  MarkAllReadResponse,
  NotificationList,
  NotificationListParams,
} from '../types/notification.types'

export const notificationApi = {
  list: async (params: NotificationListParams = {}) => {
    const query = {
      page: params.page ?? 0,
      size: params.size ?? 10,
      ...(params.isRead === undefined ? {} : { is_read: params.isRead }),
    }
    const { data } = await api.get<ApiResponse<NotificationList>>('/notifications', {
      params: query,
    })
    return data.data
  },
  markRead: async (id: string) => {
    const { data } = await api.patch<ApiResponse<{ id: string; isRead: boolean }>>(
      `/notifications/${id}/read`,
    )
    return data
  },
  markAllRead: async () => {
    const { data } = await api.patch<ApiResponse<MarkAllReadResponse>>('/notifications/read-all')
    return data
  },
}
