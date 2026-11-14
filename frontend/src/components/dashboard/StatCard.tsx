import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '../../utils/cn'
import { Card } from '../ui/Card'

interface StatCardProps {
  icon: LucideIcon
  label: string
  value: ReactNode
  hint?: string
  tone?: 'brand' | 'accent' | 'emerald' | 'amber'
}

const TONES = {
  brand: 'bg-brand-50 text-brand-600',
  accent: 'bg-accent-50 text-accent-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
}

export function StatCard({ icon: Icon, label, value, hint, tone = 'brand' }: StatCardProps) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
          <p className="mt-2 text-2xl font-bold tracking-tight text-slate-900">{value}</p>
          {hint ? <p className="mt-1 text-xs text-slate-500">{hint}</p> : null}
        </div>
        <span className={cn('flex h-10 w-10 items-center justify-center rounded-xl', TONES[tone])}>
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </Card>
  )
}
