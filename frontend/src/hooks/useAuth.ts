import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api/authApi'
import { userApi } from '../api/userApi'
import { useAuthStore } from '../store/authStore'
import { useNotificationStore } from '../store/notificationStore'
import type {
  ChangePasswordPayload,
  LoginPayload,
  RegisterPayload,
  UpdateProfilePayload,
} from '../types/auth.types'

export const authKeys = {
  me: ['auth', 'me'] as const,
}

export function useProfile() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const setUser = useAuthStore((state) => state.setUser)

  const query = useQuery({
    queryKey: authKeys.me,
    queryFn: userApi.me,
    enabled: Boolean(accessToken),
    staleTime: 5 * 60_000,
  })

  useEffect(() => {
    if (query.data) {
      setUser({
        id: query.data.id,
        email: query.data.email,
        fullName: query.data.fullName,
      })
    }
  }, [query.data, setUser])

  return query
}

export function useLogin() {
  const setSession = useAuthStore((state) => state.setSession)
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: LoginPayload) => authApi.login(payload),
    onSuccess: (auth) => {
      setSession(auth)
      queryClient.clear()
    },
  })
}

export function useRegister() {
  const setSession = useAuthStore((state) => state.setSession)
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (payload: RegisterPayload) => {
      await authApi.register(payload)
      return authApi.login({ email: payload.email, password: payload.password })
    },
    onSuccess: (auth) => {
      setSession(auth)
      queryClient.clear()
    },
  })
}

export function useLogout() {
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const clearSession = useAuthStore((state) => state.clearSession)
  const resetNotifications = useNotificationStore((state) => state.reset)
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async () => {
      if (refreshToken) {
        try {
          await authApi.logout(refreshToken)
        } catch {
          // The local session is cleared even if the token was already revoked.
        }
      }
    },
    onSettled: () => {
      clearSession()
      resetNotifications()
      queryClient.clear()
    },
  })
}

export function useUpdateProfile() {
  const setUser = useAuthStore((state) => state.setUser)
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) => authApi.updateProfile(payload),
    onSuccess: (profile) => {
      setUser({ id: profile.id, email: profile.email, fullName: profile.fullName })
      void queryClient.invalidateQueries({ queryKey: authKeys.me })
    },
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (payload: ChangePasswordPayload) => authApi.changePassword(payload),
  })
}
