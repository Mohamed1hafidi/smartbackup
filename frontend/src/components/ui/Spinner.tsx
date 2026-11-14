import { Loader2 } from 'lucide-react'
import { cn } from '../../utils/cn'

export function Spinner({ className }: { className?: string }) {
  return <Loader2 className={cn('h-5 w-5 animate-spin text-brand-600', className)} aria-hidden />
}

export function LoadingState({ label = 'Chargement…' }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12 text-sm text-slate-500">
      <Spinner className="h-6 w-6" />
      <span>{label}</span>
    </div>
  )
}
