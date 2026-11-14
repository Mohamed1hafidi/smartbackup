export type NotificationType =
  | 'BACKUP_SUCCESS'
  | 'BACKUP_FAILED'
  | 'RESTORE_SUCCESS'
  | 'RESTORE_FAILED'
  | 'SCHEDULE_ACTIVATED'
  | 'SCHEDULE_DEACTIVATED'

export interface Notification {
  id: string
  backupId: string | null
  type: NotificationType
  message: string
  isRead: boolean
  createdAt: string
}

export interface NotificationList {
  content: Notification[]
  totalElements: number
  totalPages: number
  unreadCount: number
}

export interface NotificationListParams {
  page?: number
  size?: number
  isRead?: boolean
}

export interface MarkAllReadResponse {
  updatedCount: number
}
