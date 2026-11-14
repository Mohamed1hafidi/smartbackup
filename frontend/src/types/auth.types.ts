export interface UserSummary {
  id: string
  email: string
  fullName: string
}

export interface UserProfile extends UserSummary {
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  refreshToken: string
  user: UserSummary
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  email: string
  password: string
  fullName: string
}

export interface UpdateProfilePayload {
  fullName: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}
