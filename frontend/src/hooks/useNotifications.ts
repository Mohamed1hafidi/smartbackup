import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationApi } from '../api/notificationApi'
import { useNotificationStore } from '../store/notificationStore'
import { NOTIFICATIONS_POLL_INTERVAL_MS } from '../api/config'
import type { NotificationListParams } from '../types/notification.types'

export const notificationKeys = {
  all: ['notifications'] as const,
  list: (params: NotificationListParams) => ['notifications', 'list', params] as const,
}

export function useNotifications(params: NotificationListParams = {}) {
  const setUnreadCount = useNotificationStore((state) => state.setUnreadCount)

  const query = useQuery({
    queryKey: notificationKeys.list(params),
    queryFn: () => notificationApi.list(params),
    refetchInterval: NOTIFICATIONS_POLL_INTERVAL_MS,
    refetchOnWindowFocus: true,
    staleTime: 10_000,
  })

  useEffect(() => {
    if (query.data) {
      setUnreadCount(query.data.unreadCount)
    }
  }, [query.data, setUnreadCount])

  return query
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all })
    },
  })
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all })
    },
  })
}
