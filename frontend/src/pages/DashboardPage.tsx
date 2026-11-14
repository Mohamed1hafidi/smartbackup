import {
  ArrowUpRight,
  CalendarClock,
  CheckCircle2,
  FileText,
  HardDrive,
  History,
  RefreshCw,
  TrendingUp,
  XCircle,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { STORAGE_QUOTA_BYTES } from '../api/config'
import { ActivityChart } from '../components/dashboard/ActivityChart'
import { RecentBackups } from '../components/dashboard/RecentBackups'
import { StatCard } from '../components/dashboard/StatCard'
import { StorageQuotaBar } from '../components/dashboard/StorageQuotaBar'
import { Button } from '../components/ui/Button'
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/Card'
import { Skeleton } from '../components/ui/Skeleton'
import { useBackups } from '../hooks/useBackups'
import { useDashboard } from '../hooks/useDashboard'
import { useAuthStore } from '../store/authStore'
import { getApiErrorMessage } from '../utils/apiError'
import { formatDate, formatRelativeDate } from '../utils/format'

export function DashboardPage() {
  const user = useAuthStore((state) => state.user)
  const dashboard = useDashboard()
  const recent = useBackups({ page: 0, size: 8 })
  const stats = dashboard.data

  const refreshing = dashboard.isFetching || recent.isFetching

  const refresh = () => {
    void dashboard.refetch()
    void recent.refetch()
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-brand-600">
            Tableau de bord
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">
            Bonjour {user?.fullName?.split(' ')[0] ?? ''}, voici votre espace
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {stats?.lastBackupDate
              ? `Dernière sauvegarde ${formatRelativeDate(stats.lastBackupDate)}.`
              : 'Lancez votre première sauvegarde pour démarrer.'}
          </p>
        </div>
        <Button variant="outline" onClick={refresh} disabled={refreshing}>
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
          Actualiser
        </Button>
      </div>

      {dashboard.isError ? (
        <div className="flex flex-col items-start justify-between gap-3 rounded-2xl border border-red-200 bg-red-50 p-5 sm:flex-row sm:items-center">
          <div>
            <p className="text-sm font-semibold text-red-800">
              Impossible de charger le tableau de bord
            </p>
            <p className="mt-0.5 text-xs text-red-600">
              {getApiErrorMessage(dashboard.error)}
            </p>
          </div>
          <Button variant="outline" size="sm" onClick={() => void dashboard.refetch()}>
            Réessayer
          </Button>
        </div>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {dashboard.isLoading || !stats ? (
          Array.from({ length: 4 }).map((_, index) => (
            <Card key={index} className="p-5">
              <Skeleton className="h-3 w-20" />
              <Skeleton className="mt-3 h-8 w-24" />
              <Skeleton className="mt-2 h-3 w-28" />
            </Card>
          ))
        ) : (
          <>
            <StatCard
              icon={FileText}
              label="Fichiers"
              value={stats.totalFiles}
              hint="Fichiers actifs"
              tone="brand"
            />
            <StatCard
              icon={History}
              label="Sauvegardes"
              value={stats.totalBackups}
              hint={`${stats.successCount} réussies · ${stats.failedCount} échouées`}
              tone="accent"
            />
            <StatCard
              icon={HardDrive}
              label="Stockage utilisé"
              value={stats.usedStorageFormatted}
              hint={`sur ${(STORAGE_QUOTA_BYTES / 1024 / 1024 / 1024).toFixed(0)} Go`}
              tone="emerald"
            />
            <StatCard
              icon={TrendingUp}
              label="Taux de réussite"
              value={`${stats.successRate.toFixed(1)} %`}
              hint={
                stats.successCount + stats.failedCount > 0
                  ? 'Sur les sauvegardes terminées'
                  : 'Aucune sauvegarde terminée'
              }
              tone="amber"
            />
          </>
        )}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <ActivityChart backups={recent.data?.content ?? []} />
        </div>
        <div className="space-y-4">
          <StorageQuotaBar
            usedBytes={stats?.usedStorageBytes ?? 0}
            quotaBytes={STORAGE_QUOTA_BYTES}
          />
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                  <CalendarClock className="h-4 w-4" />
                </span>
                <div>
                  <CardTitle>Prochaine sauvegarde</CardTitle>
                  <p className="text-xs text-slate-500">Planifications actives</p>
                </div>
              </div>
            </CardHeader>
            <CardContent className="pt-3">
              <p className="text-lg font-semibold text-slate-900">
                {stats?.nextScheduledBackup
                  ? formatDate(stats.nextScheduledBackup, 'dd MMM yyyy, HH:mm')
                  : 'Aucune planification'}
              </p>
              <div className="mt-1 flex items-center justify-between text-xs text-slate-500">
                <span>
                  {stats?.activeSchedules ?? 0} planification
                  {(stats?.activeSchedules ?? 0) > 1 ? 's' : ''} active
                  {(stats?.activeSchedules ?? 0) > 1 ? 's' : ''}
                </span>
                <Link
                  to="/schedules"
                  className="inline-flex items-center gap-1 font-semibold text-brand-700 hover:text-brand-800"
                >
                  Gérer
                  <ArrowUpRight className="h-3.5 w-3.5" />
                </Link>
              </div>
              <div className="mt-3 flex items-center gap-4 rounded-xl bg-slate-50 px-3 py-2 text-xs">
                <span className="inline-flex items-center gap-1.5 text-emerald-700">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  {stats?.successCount ?? 0} réussies
                </span>
                <span className="inline-flex items-center gap-1.5 text-red-600">
                  <XCircle className="h-3.5 w-3.5" />
                  {stats?.failedCount ?? 0} échouées
                </span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <RecentBackups backups={recent.data?.content ?? []} />
    </div>
  )
}
