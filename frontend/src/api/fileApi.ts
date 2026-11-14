import { api } from './axiosInstance'
import type { ApiResponse, PageResponse } from '../types/common.types'
import type { DownloadInfo, FileDetail, FileListParams, FileSummary } from '../types/file.types'

export const fileApi = {
  list: async (params: FileListParams) => {
    const { data } = await api.get<ApiResponse<PageResponse<FileSummary>>>('/files', { params })
    return data.data
  },
  upload: async (file: File, onProgress?: (percent: number) => void) => {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post<ApiResponse<FileDetail>>('/files', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (event) => {
        if (onProgress && event.total) {
          onProgress(Math.round((event.loaded * 100) / event.total))
        }
      },
    })
    return data.data
  },
  get: async (id: string) => {
    const { data } = await api.get<ApiResponse<FileDetail>>(`/files/${id}`)
    return data.data
  },
  download: async (id: string) => {
    const { data } = await api.get<ApiResponse<DownloadInfo>>(`/files/${id}/download`)
    return data.data
  },
  remove: async (id: string) => {
    await api.delete(`/files/${id}`)
  },
}
