import type { DayOfWeek } from '../types/schedule.types'

export const DAYS_OF_WEEK: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Lundi' },
  { value: 'TUESDAY', label: 'Mardi' },
  { value: 'WEDNESDAY', label: 'Mercredi' },
  { value: 'THURSDAY', label: 'Jeudi' },
  { value: 'FRIDAY', label: 'Vendredi' },
  { value: 'SATURDAY', label: 'Samedi' },
  { value: 'SUNDAY', label: 'Dimanche' },
]

export const TIMEZONES = [
  'Europe/Paris',
  'Europe/London',
  'Europe/Berlin',
  'Europe/Madrid',
  'Africa/Casablanca',
  'Africa/Dakar',
  'America/New_York',
  'America/Chicago',
  'America/Los_Angeles',
  'Asia/Tokyo',
  'Asia/Singapore',
  'Australia/Sydney',
  'UTC',
]

export const DEFAULT_TIMEZONE =
  Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Paris'

export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024
