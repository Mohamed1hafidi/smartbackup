import { useEffect, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import { CloudUpload, Loader2, UploadCloud } from 'lucide-react'
import { useUploadFile } from '../../hooks/useFiles'
import { getApiErrorMessage } from '../../utils/apiError'
import { MAX_FILE_SIZE_BYTES } from '../../utils/constants'
import { cn } from '../../utils/cn'
import { Button } from '../ui/Button'
import { Progress } from '../ui/Progress'
import { useToast } from '../ui/Toast'

interface FileUploaderProps {
  onUploaded?: () => void
  compact?: boolean
}

export function FileUploader({ onUploaded, compact }: FileUploaderProps) {
  const upload = useUploadFile()
  const toast = useToast()
  const [progress, setProgress] = useState(0)
  const [currentName, setCurrentName] = useState<string | null>(null)

  useEffect(() => {
    if (upload.isPending) {
      return
    }
    const timeout = window.setTimeout(() => {
      setProgress(0)
      setCurrentName(null)
    }, 600)
    return () => window.clearTimeout(timeout)
  }, [upload.isPending])

  const dropzone = useDropzone({
    multiple: true,
    maxSize: MAX_FILE_SIZE_BYTES,
    disabled: upload.isPending,
    onDropRejected: (rejections) => {
      const first = rejections[0]
      if (first?.errors[0]?.code === 'file-too-large') {
        toast.error('Fichier trop volumineux : 50 Mo maximum.')
      } else {
        toast.error(first?.errors[0]?.message ?? 'Fichier refusé.')
      }
    },
    onDropAccepted: async (files) => {
      for (const file of files) {
        setCurrentName(file.name)
        setProgress(0)
        try {
          await upload.mutateAsync({
            file,
            onProgress: (value) => setProgress(value),
          })
          toast.success(`« ${file.name} » a été importé.`)
        } catch (error) {
          toast.error(getApiErrorMessage(error, `Impossible d’importer « ${file.name} ».`))
        }
      }
      setProgress(100)
      onUploaded?.()
    },
  })

  return (
    <div
      {...dropzone.getRootProps()}
      className={cn(
        'relative flex flex-col items-center justify-center rounded-2xl border-2 border-dashed px-6 text-center transition',
        compact ? 'py-8' : 'py-10',
        dropzone.isDragActive
          ? 'border-brand-500 bg-brand-50/70'
          : 'border-slate-300 bg-slate-50/70 hover:border-brand-300 hover:bg-brand-50/40',
        upload.isPending && 'cursor-progress',
      )}
    >
      <input {...dropzone.getInputProps()} />
      <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white text-brand-600 shadow-sm">
        {upload.isPending ? (
          <Loader2 className="h-6 w-6 animate-spin" />
        ) : (
          <UploadCloud className="h-6 w-6" />
        )}
      </span>
      <p className="mt-4 text-sm font-semibold text-slate-900">
        {dropzone.isDragActive ? 'Déposez vos fichiers ici' : 'Glissez-déposez vos fichiers'}
      </p>
      <p className="mt-1 text-xs text-slate-500">
        ou sélectionnez-les depuis votre ordinateur · 50 Mo max par fichier
      </p>
      <Button
        variant="outline"
        size="sm"
        className="mt-4"
        disabled={upload.isPending}
        onClick={(event) => {
          event.stopPropagation()
          dropzone.open()
        }}
      >
        <CloudUpload className="h-4 w-4" />
        Parcourir
      </Button>
      {upload.isPending ? (
        <div className="mt-4 w-full max-w-sm">
          <div className="mb-1 flex items-center justify-between text-xs text-slate-500">
            <span className="truncate">{currentName}</span>
            <span>{progress} %</span>
          </div>
          <Progress value={progress} />
        </div>
      ) : null}
    </div>
  )
}
