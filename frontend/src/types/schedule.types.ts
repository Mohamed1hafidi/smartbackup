export type ScheduleFrequency = 'DAILY' | 'WEEKLY'

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface Schedule {
  id: string
  fileId: string
  fileName: string
  frequency: ScheduleFrequency
  scheduledTime: string
  dayOfWeek: DayOfWeek | null
  timezone: string
  isActive: boolean
  lastRunAt: string | null
  nextRunAt: string | null
  createdAt: string
  updatedAt: string
}

export type ScheduleExecutionStatus = 'SUCCESS' | 'FAILED'

export interface ScheduleExecution {
  id: string
  scheduleId: string
  backupId: string | null
  status: ScheduleExecutionStatus
  errorMessage: string | null
  durationMs: number | null
  startedAt: string
  completedAt: string | null
  createdAt: string
}

export interface CreateSchedulePayload {
  fileId: string
  frequency: ScheduleFrequency
  scheduledTime: string
  dayOfWeek?: DayOfWeek | null
  timezone: string
}

export type UpdateSchedulePayload = Omit<CreateSchedulePayload, 'fileId'>

export interface ScheduleListResult {
  data: Schedule[]
  totalElements: number
}
