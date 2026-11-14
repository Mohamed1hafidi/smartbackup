import { z } from 'zod'

const passwordRule = z
  .string()
  .min(8, 'Le mot de passe doit contenir au moins 8 caractères.')
  .max(100, 'Le mot de passe ne doit pas dépasser 100 caractères.')
  .regex(/[A-Z]/, 'Ajoutez au moins une majuscule.')
  .regex(/\d/, 'Ajoutez au moins un chiffre.')
  .regex(/[^A-Za-z0-9]/, 'Ajoutez au moins un caractère spécial.')

export const loginSchema = z.object({
  email: z.string().min(1, "L'email est requis.").email('Format d’email invalide.'),
  password: z.string().min(1, 'Le mot de passe est requis.'),
})

export const registerSchema = z.object({
  fullName: z
    .string()
    .min(2, 'Le nom doit contenir au moins 2 caractères.')
    .max(100, 'Le nom ne doit pas dépasser 100 caractères.'),
  email: z.string().min(1, "L'email est requis.").email('Format d’email invalide.'),
  password: passwordRule,
})

export const profileSchema = z.object({
  fullName: z
    .string()
    .min(2, 'Le nom doit contenir au moins 2 caractères.')
    .max(100, 'Le nom ne doit pas dépasser 100 caractères.'),
})

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Le mot de passe actuel est requis.'),
    newPassword: passwordRule,
    confirmPassword: z.string().min(1, 'Confirmez le nouveau mot de passe.'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: 'Les deux mots de passe ne correspondent pas.',
    path: ['confirmPassword'],
  })
  .refine((values) => values.newPassword !== values.currentPassword, {
    message: 'Le nouveau mot de passe doit être différent de l’actuel.',
    path: ['newPassword'],
  })

export const scheduleSchema = z
  .object({
    fileId: z.string().optional(),
    frequency: z.enum(['DAILY', 'WEEKLY']),
    scheduledTime: z
      .string()
      .regex(/^([01]\d|2[0-3]):[0-5]\d$/, 'Utilisez le format HH:mm.'),
    dayOfWeek: z
      .enum(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'])
      .optional(),
    timezone: z.string().min(1, 'Le fuseau horaire est requis.'),
  })
  .superRefine((values, ctx) => {
    if (values.frequency === 'WEEKLY' && !values.dayOfWeek) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Choisissez un jour de la semaine.',
        path: ['dayOfWeek'],
      })
    }
  })

export type LoginValues = z.infer<typeof loginSchema>
export type RegisterValues = z.infer<typeof registerSchema>
export type ProfileValues = z.infer<typeof profileSchema>
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>
export type ScheduleValues = z.infer<typeof scheduleSchema>
