import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff, Loader2, LockKeyhole, Mail } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useLogin } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../utils/apiError'
import { loginSchema, type LoginValues } from '../../utils/validators'
import { Button } from '../ui/Button'
import { Field } from '../ui/Field'
import { Input } from '../ui/Input'
import { useToast } from '../ui/Toast'

export function LoginForm() {
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const onSubmit = handleSubmit((values) => {
    setServerError(null)
    login.mutate(values, {
      onSuccess: () => {
        toast.success('Connexion réussie. Bienvenue !')
        const from = (location.state as { from?: string } | null)?.from
        navigate(from ?? '/dashboard', { replace: true })
      },
      onError: (error) => {
        const message = getApiErrorMessage(error, 'Identifiants incorrects.')
        setServerError(message)
        toast.error(message)
      },
    })
  })

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Bon retour !</h1>
        <p className="mt-1 text-sm text-slate-500">
          Connectez-vous pour retrouver vos fichiers et sauvegardes.
        </p>
      </div>

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        {serverError ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        ) : null}

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

        <Field label="Mot de passe" htmlFor="password" required error={errors.password?.message}>
          <div className="relative">
            <LockKeyhole className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
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

        <Button type="submit" className="w-full" size="lg" disabled={login.isPending}>
          {login.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
          Se connecter
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-500">
        Pas encore de compte ?{' '}
        <Link to="/register" className="font-semibold text-brand-700 hover:text-brand-800">
          Créer un compte
        </Link>
      </p>
    </div>
  )
}
