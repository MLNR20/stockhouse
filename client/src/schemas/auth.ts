import { z } from 'zod'

/** Response of POST /api/auth/login, /register and /refresh. */
export const tokenResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  tokenType: z.string(),
  expiresInSeconds: z.number(),
})

export type TokenResponse = z.infer<typeof tokenResponseSchema>

/** Login form validation. */
export const loginInputSchema = z.object({
  email: z.string().trim().pipe(z.email('Enter a valid email address')),
  password: z.string().min(1, 'Password is required'),
})

export type LoginInput = z.input<typeof loginInputSchema>
