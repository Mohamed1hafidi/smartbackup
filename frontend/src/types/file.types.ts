export interface FileSummary {
  id: string
  originalName: string
  fileSize: number
  mimeType: string
  createdAt: string
  lastBackupAt: string | null
}

export interface FileDetail extends FileSummary {
  storedKey: string
  sha256Hash: string
  updatedAt: string
  backupCount: number
}

export interface DownloadInfo {
  downloadUrl: string
  expiresAt: string
  fileName: string
  fileSize: number
  mimeType: string
}

export type FileSort = 'createdAt' | 'originalName' | 'fileSize'
export type SortDirection = 'asc' | 'desc'

export interface FileListParams {
  page?: number
  size?: number
  search?: string
  sort?: FileSort
  direction?: SortDirection
}
