export type BackupStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED'
export type BackupType = 'MANUAL' | 'SCHEDULED'
export type RestoreStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED'

export interface BackupSummary {
  id: string
  fileId: string
  fileName: string
  backupType: BackupType
  status: BackupStatus
  fileSize: number
  versionNumber: number
  createdAt: string
  completedAt: string | null
}

export interface BackupDetail extends BackupSummary {
  r2ObjectKey: string
  r2Bucket: string
  sha256Hash: string
  errorMessage: string | null
}

export interface RestoreJob {
  restoreJobId: string
  backupId: string
  fileId: string
  status: RestoreStatus
  initiatedAt: string
  completedAt: string | null
  errorMessage: string | null
}

export interface BackupListParams {
  page?: number
  size?: number
  status?: BackupStatus
  fileId?: string
  dateFrom?: string
  dateTo?: string
}
