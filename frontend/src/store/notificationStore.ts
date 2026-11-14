import { create } from 'zustand'

interface NotificationState {
  unreadCount: number
  setUnreadCount: (count: number) => void
  reset: () => void
}

export const useNotificationStore = create<NotificationState>((set) => ({
  unreadCount: 0,
  setUnreadCount: (count) => set({ unreadCount: Math.max(0, count) }),
  reset: () => set({ unreadCount: 0 }),
}))
