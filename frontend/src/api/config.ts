export const API_BASE_URL = (() => {
  const raw = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim()
  const base = raw && raw.length > 0 ? raw : 'http://localhost:8080'
  const withoutTrailingSlash = base.replace(/\/+$/, '')
  return withoutTrailingSlash.endsWith('/api')
    ? withoutTrailingSlash
    : `${withoutTrailingSlash}/api`
})()

export const APP_NAME = (import.meta.env.VITE_APP_NAME as string | undefined) ?? 'SmartBackup'
export const APP_VERSION = (import.meta.env.VITE_APP_VERSION as string | undefined) ?? '1.0.0'
export const STORAGE_QUOTA_BYTES =
  Number(import.meta.env.VITE_STORAGE_QUOTA_BYTES ?? 1024 * 1024 * 1024) || 1024 * 1024 * 1024
export const NOTIFICATIONS_POLL_INTERVAL_MS = 30_000
