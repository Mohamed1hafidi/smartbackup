import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, UserSummary } from '../types/auth.types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserSummary | null
  setSession: (auth: AuthResponse) => void
  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (user: UserSummary) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setSession: (auth) =>
        set({
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
          user: auth.user,
        }),
      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
      setUser: (user) => set({ user }),
      clearSession: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    {
      name: 'smartbackup-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    },
  ),
)
