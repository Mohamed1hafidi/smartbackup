import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { backupApi } from '../api/backupApi'
import { triggerDownload } from '../utils/download'
import type { BackupListParams } from '../types/backup.types'

export const backupKeys = {
  all: ['backups'] as const,
  list: (params: BackupListParams) => ['backups', 'list', params] as const,
  detail: (id: string) => ['backups', 'detail', id] as const,
  restore: (jobId: string) => ['backups', 'restore', jobId] as const,
}

export function useBackups(params: BackupListParams) {
  return useQuery({
    queryKey: backupKeys.list(params),
    queryFn: () => backupApi.list(params),
    placeholderData: (previous) => previous,
    refetchInterval: (query) => {
      const data = query.state.data
      const pending = data?.content.some(
        (backup) => backup.status === 'PENDING' || backup.status === 'IN_PROGRESS',
      )
      return pending ? 4000 : false
    },
  })
}

export function useBackup(id?: string) {
  return useQuery({
    queryKey: backupKeys.detail(id ?? ''),
    queryFn: () => backupApi.get(id as string),
    enabled: Boolean(id),
  })
}

export function useCreateBackup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (fileId: string) => backupApi.createManual(fileId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: backupKeys.all })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['files'] })
    },
  })
}

export function useRestoreBackup() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (backupId: string) => backupApi.restore(backupId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: backupKeys.all })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}

export function useRestoreStatus(jobId?: string) {
  return useQuery({
    queryKey: backupKeys.restore(jobId ?? ''),
    queryFn: () => backupApi.restoreStatus(jobId as string),
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'PENDING' || status === 'IN_PROGRESS' ? 2000 : false
    },
  })
}

export function useDownloadBackup() {
  return useMutation({
    mutationFn: (backupId: string) => backupApi.download(backupId),
    onSuccess: (download) => triggerDownload(download.downloadUrl),
  })
}
