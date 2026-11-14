import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { BadgeCheck, KeyRound, Loader2, Mail, ShieldCheck, UserRound } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card'
import { Field } from '../components/ui/Field'
import { Input } from '../components/ui/Input'
import { Skeleton } from '../components/ui/Skeleton'
import { Badge } from '../components/ui/Badge'
import { useToast } from '../components/ui/Toast'
import { useChangePassword, useProfile, useUpdateProfile } from '../hooks/useAuth'
import { useAuthStore } from '../store/authStore'
import { getApiErrorMessage } from '../utils/apiError'
import { formatDate, getInitials } from '../utils/format'
import {
  changePasswordSchema,
  profileSchema,
  type ChangePasswordValues,
  type ProfileValues,
} from '../utils/validators'

export function ProfilePage() {
  const toast = useToast()
  const profile = useProfile()
  const sessionUser = useAuthStore((state) => state.user)
  const updateProfile = useUpdateProfile()
  const changePassword = useChangePassword()

  const user = profile.data ?? sessionUser

  const profileForm = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName: user?.fullName ?? '' },
  })

  const passwordForm = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  useEffect(() => {
    if (user?.fullName) {
      profileForm.reset({ fullName: user.fullName })
    }
  }, [user?.fullName, profileForm])

  const onSubmitProfile = profileForm.handleSubmit((values) => {
    updateProfile.mutate(values, {
      onSuccess: () => toast.success('Profil mis à jour.'),
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  })

  const onSubmitPassword = passwordForm.handleSubmit((values) => {
    changePassword.mutate(values, {
      onSuccess: () => {
        toast.success('Mot de passe modifié.')
        passwordForm.reset()
      },
      onError: (error) => toast.error(getApiErrorMessage(error)),
    })
  })

  if (profile.isLoading && !user) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-52" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-brand-600">Compte</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">Mon profil</h1>
        <p className="mt-1 text-sm text-slate-500">
          Gérez vos informations personnelles et la sécurité de votre compte.
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-brand-600 text-xl font-bold text-white shadow-lg shadow-brand-900/20">
            {getInitials(user?.fullName)}
          </span>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="truncate text-lg font-bold text-slate-900">
                {user?.fullName ?? 'Utilisateur'}
              </h2>
              <Badge variant="success" dot>
                Actif
              </Badge>
            </div>
            <p className="mt-0.5 flex items-center gap-1.5 truncate text-sm text-slate-500">
              <Mail className="h-3.5 w-3.5" />
              {user?.email}
            </p>
            <p className="mt-1 text-xs text-slate-400">
              Membre depuis {profile.data ? formatDate(profile.data.createdAt, 'MMMM yyyy') : '—'}
            </p>
          </div>
          <div className="hidden items-center gap-1.5 rounded-xl bg-emerald-50 px-3 py-2 text-xs font-semibold text-emerald-700 sm:flex">
            <BadgeCheck className="h-4 w-4" />
            Compte vérifié
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Informations personnelles</CardTitle>
              <CardDescription>Votre nom affiché dans l’application.</CardDescription>
            </div>
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
              <UserRound className="h-4 w-4" />
            </span>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmitProfile} className="space-y-4" noValidate>
              <Field
                label="Nom complet"
                htmlFor="fullName"
                required
                error={profileForm.formState.errors.fullName?.message}
              >
                <Input
                  id="fullName"
                  autoComplete="name"
                  invalid={Boolean(profileForm.formState.errors.fullName)}
                  {...profileForm.register('fullName')}
                />
              </Field>

              <Field label="Adresse email" htmlFor="email">
                <Input id="email" value={user?.email ?? ''} disabled readOnly />
              </Field>
              <p className="text-xs text-slate-400">
                L’adresse email ne peut pas être modifiée pour le moment.
              </p>

              <div className="flex justify-end">
                <Button
                  type="submit"
                  disabled={updateProfile.isPending || !profileForm.formState.isDirty}
                >
                  {updateProfile.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                  Enregistrer
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Sécurité</CardTitle>
              <CardDescription>Changez régulièrement votre mot de passe.</CardDescription>
            </div>
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
              <ShieldCheck className="h-4 w-4" />
            </span>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmitPassword} className="space-y-4" noValidate>
              <Field
                label="Mot de passe actuel"
                htmlFor="currentPassword"
                required
                error={passwordForm.formState.errors.currentPassword?.message}
              >
                <Input
                  id="currentPassword"
                  type="password"
                  autoComplete="current-password"
                  invalid={Boolean(passwordForm.formState.errors.currentPassword)}
                  {...passwordForm.register('currentPassword')}
                />
              </Field>

              <Field
                label="Nouveau mot de passe"
                htmlFor="newPassword"
                required
                error={passwordForm.formState.errors.newPassword?.message}
                hint="8 caractères minimum, une majuscule, un chiffre et un caractère spécial."
              >
                <Input
                  id="newPassword"
                  type="password"
                  autoComplete="new-password"
                  invalid={Boolean(passwordForm.formState.errors.newPassword)}
                  {...passwordForm.register('newPassword')}
                />
              </Field>

              <Field
                label="Confirmer le mot de passe"
                htmlFor="confirmPassword"
                required
                error={passwordForm.formState.errors.confirmPassword?.message}
              >
                <Input
                  id="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  invalid={Boolean(passwordForm.formState.errors.confirmPassword)}
                  {...passwordForm.register('confirmPassword')}
                />
              </Field>

              <div className="flex justify-end">
                <Button type="submit" variant="outline" disabled={changePassword.isPending}>
                  {changePassword.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <KeyRound className="h-4 w-4" />
                  )}
                  Modifier le mot de passe
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
