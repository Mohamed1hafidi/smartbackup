import { CloudUpload, Loader2 } from 'lucide-react'
import { useCreateBackup } from '../../hooks/useBackups'
import { getApiErrorMessage } from '../../utils/apiError'
import { Button, type ButtonProps } from '../ui/Button'
import { useToast } from '../ui/Toast'

interface ManualBackupButtonProps {
  fileId: string
  fileName?: string
  variant?: ButtonProps['variant']
  size?: ButtonProps['size']
  showLabel?: boolean
}

export function ManualBackupButton({
  fileId,
  fileName,
  variant = 'subtle',
  size = 'sm',
  showLabel = true,
}: ManualBackupButtonProps) {
  const backup = useCreateBackup()
  const toast = useToast()

  const handleBackup = () => {
    backup.mutate(fileId, {
      onSuccess: () => {
        toast.success(
          fileName
            ? `Sauvegarde démarrée pour « ${fileName} ».`
            : 'Sauvegarde démarrée.',
        )
      },
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  }

  return (
    <Button
      variant={variant}
      size={size}
      onClick={handleBackup}
      disabled={backup.isPending}
      title={fileName ? `Sauvegarder ${fileName}` : 'Sauvegarder maintenant'}
    >
      {backup.isPending ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : (
        <CloudUpload className="h-4 w-4" />
      )}
      {showLabel ? (backup.isPending ? 'Sauvegarde…' : 'Sauvegarder') : null}
    </Button>
  )
}
