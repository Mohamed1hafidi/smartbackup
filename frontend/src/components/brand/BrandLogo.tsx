import { Link } from 'react-router-dom'
import { cn } from '../../utils/cn'

const SIZES = {
  sm: {
    img: 'h-8 w-auto max-w-[80px]',
    text: 'text-lg',
    tagline: 'text-[10px]',
    gap: 'gap-2.5',
  },
  md: {
    img: 'h-10 w-auto max-w-[100px]',
    text: 'text-xl',
    tagline: 'text-xs',
    gap: 'gap-3',
  },
  lg: {
    img: 'h-12 w-auto max-w-[120px]',
    text: 'text-2xl',
    tagline: 'text-xs',
    gap: 'gap-3.5',
  },
  xl: {
    img: 'h-16 w-auto max-w-[160px]',
    text: 'text-4xl',
    tagline: 'text-sm',
    gap: 'gap-4',
  },
} as const

interface BrandLogoProps {
  size?: keyof typeof SIZES
  tagline?: boolean
  inverted?: boolean
  className?: string
}

export function BrandLogo({ size = 'md', tagline, inverted, className }: BrandLogoProps) {
  const styles = SIZES[size]

  return (
    <Link
      to="/"
      className={cn('group inline-flex min-w-0 items-center', styles.gap, className)}
      aria-label="SmartBackup — accueil"
    >
      <span className="relative shrink-0 flex items-center">
        <img
          src="/logo-app-icon.png"
          alt="SmartBackup Emblem"
          className={cn(
            'object-contain drop-shadow-[0_6px_16px_rgba(37,99,235,0.35)] transition-transform duration-200 group-hover:scale-[1.04]',
            styles.img,
          )}
        />
      </span>
      <span className="min-w-0">
        <span
          className={cn(
            'block truncate font-extrabold tracking-tight',
            styles.text,
            inverted ? 'text-white' : 'text-slate-950',
          )}
        >
          Smart<span className={inverted ? 'text-brand-400' : 'text-brand-600'}>Backup</span>
        </span>
        {tagline ? (
          <span
            className={cn(
              'mt-0.5 hidden font-medium tracking-wide sm:block',
              styles.tagline,
              inverted ? 'text-slate-400' : 'text-slate-500',
            )}
          >
            Sauvegarde cloud sécurisée
          </span>
        ) : null}
      </span>
    </Link>
  )
}
