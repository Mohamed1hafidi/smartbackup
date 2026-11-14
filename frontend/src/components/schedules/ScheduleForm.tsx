import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import { useCreateSchedule, useUpdateSchedule } from '../../hooks/useSchedules'
import { useToast } from '../ui/Toast'
import type { FileSummary } from '../../types/file.types'
import type { Schedule } from '../../types/schedule.types'
import { getApiErrorMessage } from '../../utils/apiError'
import { DAYS_OF_WEEK, DEFAULT_TIMEZONE, TIMEZONES } from '../../utils/constants'
import { scheduleSchema, type ScheduleValues } from '../../utils/validators'
import { Button } from '../ui/Button'
import { Field } from '../ui/Field'
import { Input, Select } from '../ui/Input'
import { Modal } from '../ui/Modal'

interface ScheduleFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  files: FileSummary[]
  schedule?: Schedule | null
}

export function ScheduleForm({ open, onOpenChange, files, schedule }: ScheduleFormProps) {
  const create = useCreateSchedule()
  const update = useUpdateSchedule()
  const toast = useToast()
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setError,
    formState: { errors },
  } = useForm<ScheduleValues>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      fileId: '',
      frequency: 'DAILY',
      scheduledTime: '02:00',
      dayOfWeek: 'MONDAY',
      timezone: DEFAULT_TIMEZONE,
    },
  })

  const frequency = watch('frequency')
  const timezone = watch('timezone')
  const timezones = TIMEZONES.includes(timezone) ? TIMEZONES : [timezone, ...TIMEZONES]

  useEffect(() => {
    if (!open) return
    setServerError(null)
    reset({
      fileId: schedule?.fileId ?? '',
      frequency: schedule?.frequency ?? 'DAILY',
      scheduledTime: schedule?.scheduledTime ?? '02:00',
      dayOfWeek: schedule?.dayOfWeek ?? 'MONDAY',
      timezone: schedule?.timezone ?? DEFAULT_TIMEZONE,
    })
  }, [open, schedule, reset])

  const isPending = create.isPending || update.isPending

  const onSubmit = handleSubmit((values) => {
    setServerError(null)
    if (!schedule && !values.fileId) {
      setError('fileId', { message: 'Choisissez un fichier.' })
      return
    }

    const payload = {
      frequency: values.frequency,
      scheduledTime: values.scheduledTime,
      dayOfWeek: values.frequency === 'WEEKLY' ? values.dayOfWeek : null,
      timezone: values.timezone,
    }

    const onSuccess = () => {
      toast.success(schedule ? 'Planification mise à jour.' : 'Planification créée.')
      onOpenChange(false)
    }
    const onError = (error: unknown) => {
      const message = getApiErrorMessage(error)
      setServerError(message)
      toast.error(message)
    }

    if (schedule) {
      update.mutate({ id: schedule.id, payload }, { onSuccess, onError })
    } else {
      create.mutate({ ...payload, fileId: values.fileId as string }, { onSuccess, onError })
    }
  })

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={schedule ? 'Modifier la planification' : 'Nouvelle planification'}
      description={
        schedule
          ? `Fichier : ${schedule.fileName}`
          : 'Choisissez la fréquence et l’heure de vos sauvegardes automatiques.'
      }
    >
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        {serverError ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        ) : null}

        <Field label="Fichier" htmlFor="fileId" required error={errors.fileId?.message}>
          <Select
            id="fileId"
            disabled={Boolean(schedule)}
            invalid={Boolean(errors.fileId)}
            {...register('fileId')}
          >
            <option value="">Sélectionner un fichier…</option>
            {files.map((file) => (
              <option key={file.id} value={file.id}>
                {file.originalName}
              </option>
            ))}
          </Select>
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Fréquence" htmlFor="frequency" required error={errors.frequency?.message}>
            <Select id="frequency" {...register('frequency')}>
              <option value="DAILY">Quotidienne</option>
              <option value="WEEKLY">Hebdomadaire</option>
            </Select>
          </Field>

          <Field
            label="Heure"
            htmlFor="scheduledTime"
            required
            error={errors.scheduledTime?.message}
          >
            <Input
              id="scheduledTime"
              type="time"
              invalid={Boolean(errors.scheduledTime)}
              {...register('scheduledTime')}
            />
          </Field>
        </div>

        {frequency === 'WEEKLY' ? (
          <Field
            label="Jour de la semaine"
            htmlFor="dayOfWeek"
            required
            error={errors.dayOfWeek?.message}
          >
            <Select id="dayOfWeek" invalid={Boolean(errors.dayOfWeek)} {...register('dayOfWeek')}>
              {DAYS_OF_WEEK.map((day) => (
                <option key={day.value} value={day.value}>
                  {day.label}
                </option>
              ))}
            </Select>
          </Field>
        ) : null}

        <Field label="Fuseau horaire" htmlFor="timezone" required error={errors.timezone?.message}>
          <Select id="timezone" {...register('timezone')}>
            {timezones.map((zone) => (
              <option key={zone} value={zone}>
                {zone}
              </option>
            ))}
          </Select>
        </Field>

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
            Annuler
          </Button>
          <Button type="submit" disabled={isPending}>
            {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            {schedule ? 'Enregistrer' : 'Créer la planification'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
