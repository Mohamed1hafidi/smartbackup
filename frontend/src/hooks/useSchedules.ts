import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { scheduleApi } from '../api/scheduleApi'
import type { CreateSchedulePayload, UpdateSchedulePayload } from '../types/schedule.types'

export const scheduleKeys = {
  all: ['schedules'] as const,
  list: ['schedules', 'list'] as const,
  executions: (id: string, page: number) => ['schedules', 'executions', id, page] as const,
}

export function useSchedules() {
  return useQuery({
    queryKey: scheduleKeys.list,
    queryFn: scheduleApi.list,
  })
}

export function useScheduleExecutions(id?: string, page = 0) {
  return useQuery({
    queryKey: scheduleKeys.executions(id ?? '', page),
    queryFn: () => scheduleApi.executions(id as string, page),
    enabled: Boolean(id),
    placeholderData: (previous) => previous,
  })
}

function useInvalidateSchedules() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: scheduleKeys.all })
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    void queryClient.invalidateQueries({ queryKey: ['notifications'] })
  }
}

export function useCreateSchedule() {
  const invalidate = useInvalidateSchedules()
  return useMutation({
    mutationFn: (payload: CreateSchedulePayload) => scheduleApi.create(payload),
    onSuccess: invalidate,
  })
}

export function useUpdateSchedule() {
  const invalidate = useInvalidateSchedules()
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateSchedulePayload }) =>
      scheduleApi.update(id, payload),
    onSuccess: invalidate,
  })
}

export function useToggleSchedule() {
  const invalidate = useInvalidateSchedules()
  return useMutation({
    mutationFn: (id: string) => scheduleApi.toggle(id),
    onSuccess: invalidate,
  })
}

export function useDeleteSchedule() {
  const invalidate = useInvalidateSchedules()
  return useMutation({
    mutationFn: (id: string) => scheduleApi.remove(id),
    onSuccess: invalidate,
  })
}
