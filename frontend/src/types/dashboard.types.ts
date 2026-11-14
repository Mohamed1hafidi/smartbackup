export interface DashboardStats {
  totalFiles: number
  totalBackups: number
  usedStorageBytes: number
  usedStorageFormatted: string
  lastBackupDate: string | null
  nextScheduledBackup: string | null
  successCount: number
  failedCount: number
  successRate: number
  activeSchedules: number
}
