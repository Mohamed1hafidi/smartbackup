import { HardDrive } from 'lucide-react'
import { formatBytes } from '../../utils/format'
import { cn } from '../../utils/cn'
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card'
import { Progress } from '../ui/Progress'

interface StorageQuotaBarProps {
  usedBytes: number
  quotaBytes: number
}

export function StorageQuotaBar({ usedBytes, quotaBytes }: StorageQuotaBarProps) {
  const percent = quotaBytes > 0 ? (usedBytes / quotaBytes) * 100 : 0
  const tone =
    percent >= 90 ? 'bg-red-500' : percent >= 75 ? 'bg-amber-500' : 'bg-brand-600'

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <HardDrive className="h-4 w-4" />
          </span>
          <div>
            <CardTitle>Stockage</CardTitle>
            <p className="text-xs text-slate-500">Quota de votre espace</p>
          </div>
        </div>
      </CardHeader>
      <CardContent className="pt-4">
        <div className="flex items-end justify-between">
          <p className="text-2xl font-bold tracking-tight text-slate-900">
            {formatBytes(usedBytes)}
          </p>
          <p className="text-xs text-slate-500">sur {formatBytes(quotaBytes)}</p>
        </div>
        <Progress
          value={percent}
          className="mt-3"
          indicatorClassName={cn(tone)}
        />
        <div className="mt-2 flex items-center justify-between text-xs text-slate-500">
          <span>{percent.toFixed(1)} % utilisé</span>
          <span>{formatBytes(Math.max(0, quotaBytes - usedBytes))} restants</span>
        </div>
      </CardContent>
    </Card>
  )
}
