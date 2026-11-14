import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fileApi } from '../api/fileApi'
import { triggerDownload } from '../utils/download'
import type { FileListParams } from '../types/file.types'

export const fileKeys = {
  all: ['files'] as const,
  list: (params: FileListParams) => ['files', 'list', params] as const,
  detail: (id: string) => ['files', 'detail', id] as const,
}

export function useFiles(params: FileListParams) {
  return useQuery({
    queryKey: fileKeys.list(params),
    queryFn: () => fileApi.list(params),
    placeholderData: (previous) => previous,
  })
}

export function useFile(id?: string) {
  return useQuery({
    queryKey: fileKeys.detail(id ?? ''),
    queryFn: () => fileApi.get(id as string),
    enabled: Boolean(id),
  })
}

export function useUploadFile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ file, onProgress }: { file: File; onProgress?: (percent: number) => void }) =>
      fileApi.upload(file, onProgress),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: fileKeys.all })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['backups'] })
    },
  })
}

export function useDeleteFile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => fileApi.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: fileKeys.all })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['backups'] })
      void queryClient.invalidateQueries({ queryKey: ['schedules'] })
    },
  })
}

export function useDownloadFile() {
  return useMutation({
    mutationFn: (id: string) => fileApi.download(id),
    onSuccess: (download) => triggerDownload(download.downloadUrl),
  })
}
