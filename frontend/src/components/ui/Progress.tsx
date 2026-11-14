import * as ProgressPrimitive from '@radix-ui/react-progress'
import { cn } from '../../utils/cn'

interface ProgressProps {
  value: number
  className?: string
  indicatorClassName?: string
}

export function Progress({ value, className, indicatorClassName }: ProgressProps) {
  const clamped = Math.min(100, Math.max(0, value))
  return (
    <ProgressPrimitive.Root
      value={clamped}
      className={cn('relative h-2.5 w-full overflow-hidden rounded-full bg-slate-100', className)}
    >
      <ProgressPrimitive.Indicator
        className={cn('h-full rounded-full bg-brand-600 transition-all', indicatorClassName)}
        style={{ width: `${clamped}%` }}
      />
    </ProgressPrimitive.Root>
  )
}
