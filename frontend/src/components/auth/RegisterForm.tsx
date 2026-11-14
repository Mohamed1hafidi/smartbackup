import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff, Loader2, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useRegister } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../utils/apiError'
import { registerSchema, type RegisterValues } from '../../utils/validators'
import { Button } from '../ui/Button'
import { Field } from '../ui/Field'
import { Input } from '../ui/Input'
import { useToast } from '../ui/Toast'

const PASSWORD_HINTS = [
  'Au moins 8 caractères',
  'Une majuscule',
  'Un chiffre',
  'Un caractère spécial',
]

export function RegisterForm() {
  const registerMutation = useRegister()
  const navigate = useNavigate()
  const toast = useToast()
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { fullName: '', email: '', password: '' },
  })

  const onSubmit = handleSubmit((values) => {
    setServerError(null)
    registerMutation.mutate(values, {
      onSuccess: (auth) => {
        toast.success(`Compte créé. Bienvenue ${auth.user.fullName.split(' ')[0]} !`)
        navigate('/dashboard', { replace: true })
      },
      onError: (error) => {
        const message = getApiErrorMessage(error, 'Impossible de créer le compte.')
        setServerError(message)
        toast.error(message)
      },
    })
  })

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Créer un compte</h1>
        <p className="mt-1 text-sm text-slate-500">
          Quelques secondes suffisent pour commencer à sauvegarder vos fichiers.
        </p>
      </div>

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        {serverError ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        ) : null}

        <Field label="Nom complet" htmlFor="fullName" required error={errors.fullName?.message}>
          <div className="relative">
            <UserRound className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              id="fullName"
              autoComplete="name"
              placeholder="Alice Martin"
              className="pl-9"
              invalid={Boolean(errors.fullName)}
              {...register('fullName')}
            />
          </div>
        </Field>

        <Field label="Email" htmlFor="email" required error={errors.email?.message}>
          <div className="relative">
            <Mail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="vous@exemple.com"
              className="pl-9"
              invalid={Boolean(errors.email)}
              {...register('email')}
            />
          </div>
        </Field>

        <Field
          label="Mot de passe"
          htmlFor="password"
          required
          error={errors.password?.message}
          hint="8 caractères min., 1 majuscule, 1 chiffre, 1 caractère spécial."
        >
          <div className="relative">
            <LockKeyhole className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="••••••••"
              className="px-9"
              invalid={Boolean(errors.password)}
              {...register('password')}
            />
            <button
              type="button"
              onClick={() => setShowPassword((value) => !value)}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-slate-400 transition hover:text-slate-700"
              aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
        </Field>

        <ul className="grid grid-cols-2 gap-1.5 rounded-xl bg-slate-50 p-3 text-[11px] text-slate-500">
          {PASSWORD_HINTS.map((hint) => (
            <li key={hint} className="flex items-center gap-1.5">
              <span className="h-1 w-1 rounded-full bg-brand-500" />
              {hint}
            </li>
          ))}
        </ul>

        <Button
          type="submit"
          className="w-full"
          size="lg"
          disabled={registerMutation.isPending}
        >
          {registerMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
          Créer mon compte
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-500">
        Déjà inscrit ?{' '}
        <Link to="/login" className="font-semibold text-brand-700 hover:text-brand-800">
          Se connecter
        </Link>
      </p>
    </div>
  )
}
