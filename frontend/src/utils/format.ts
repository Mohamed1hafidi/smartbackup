import { format, formatDistanceToNow, isValid, parseISO } from 'date-fns'
import { fr } from 'date-fns/locale'

const UNITS = ['KB', 'MB', 'GB', 'TB', 'PB']

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) {
    return '0 B'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }

  let value = bytes
  let unitIndex = -1
  do {
    value /= 1024
    unitIndex += 1
  } while (value >= 1024 && unitIndex < UNITS.length - 1)

  return `${value.toFixed(1)} ${UNITS[unitIndex]}`
}

export function formatDate(value?: string | null, pattern = 'dd MMM yyyy, HH:mm'): string {
  if (!value) {
    return '—'
  }
  const date = parseISO(value)
  return isValid(date) ? format(date, pattern, { locale: fr }) : '—'
}

export function formatRelativeDate(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = parseISO(value)
  return isValid(date)
    ? formatDistanceToNow(date, { addSuffix: true, locale: fr })
    : '—'
}

export function formatDuration(durationMs?: number | null): string {
  if (durationMs === null || durationMs === undefined) {
    return '—'
  }
  if (durationMs < 1000) {
    return `${durationMs} ms`
  }
  return `${(durationMs / 1000).toFixed(1)} s`
}

export function getInitials(name?: string | null): string {
  if (!name) {
    return 'SB'
  }
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
}

export function pluralize(count: number, singular: string, plural = `${singular}s`): string {
  return count > 1 ? plural : singular
}
