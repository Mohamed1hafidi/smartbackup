import { api } from './axiosInstance'
import type { ApiResponse, PageResponse } from '../types/common.types'
import type {
  BackupDetail,
  BackupListParams,
  BackupSummary,
  RestoreJob,
} from '../types/backup.types'
import type { DownloadInfo } from '../types/file.types'

export const backupApi = {
  list: async (params: BackupListParams) => {
    const { data } = await api.get<ApiResponse<PageResponse<BackupSummary>>>('/backups', { params })
    return data.data
  },
  get: async (id: string) => {
    const { data } = await api.get<ApiResponse<BackupDetail>>(`/backups/${id}`)
    return data.data
  },
  createManual: async (fileId: string) => {
    const { data } = await api.post<ApiResponse<BackupDetail>>(`/backups/${fileId}`)
    return data.data
  },
  restore: async (backupId: string) => {
    const { data } = await api.post<ApiResponse<RestoreJob>>(`/backups/${backupId}/restore`)
    return data.data
  },
  restoreStatus: async (jobId: string) => {
    const { data } = await api.get<ApiResponse<RestoreJob>>(`/backups/restores/${jobId}`)
    return data.data
  },
  download: async (backupId: string) => {
    const { data } = await api.get<ApiResponse<DownloadInfo>>(`/backups/${backupId}/download`)
    return data.data
  },
}
