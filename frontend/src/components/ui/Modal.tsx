import * as Dialog from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '../../utils/cn'

interface ModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  children: ReactNode
  footer?: ReactNode
  className?: string
}

export function Modal({
  open,
  onOpenChange,
  title,
  description,
  children,
  footer,
  className,
}: ModalProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-slate-950/50 backdrop-blur-sm data-[state=open]:animate-in" />
        <Dialog.Content
          className={cn(
            'fixed left-1/2 top-1/2 z-50 max-h-[90vh] w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl focus:outline-none',
            className,
          )}
        >
          <div className="pr-8">
            <Dialog.Title className="text-lg font-semibold text-slate-900">{title}</Dialog.Title>
            {description ? (
              <Dialog.Description className="mt-1 text-sm text-slate-500">
                {description}
              </Dialog.Description>
            ) : null}
          </div>
          <Dialog.Close asChild>
            <button
              type="button"
              className="absolute right-4 top-4 rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
              aria-label="Fermer"
            >
              <X className="h-4 w-4" />
            </button>
          </Dialog.Close>
          <div className="mt-5">{children}</div>
          {footer ? <div className="mt-6 flex justify-end gap-2">{footer}</div> : null}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
