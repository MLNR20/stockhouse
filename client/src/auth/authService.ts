import { api } from '../api.ts'
import { tokenResponseSchema } from '../schemas/auth.ts'
import { clearTokens, setTokens } from './authStore.ts'

/** Signs in and stores the tokens. Rejects (axios error) on wrong credentials. */
export async function login(email: string, password: string): Promise<void> {
  const { data } = await api.post('/api/auth/login', { email, password })
  setTokens(tokenResponseSchema.parse(data))
}

/**
 * Tells the server to end the session (invalidates the refresh token), then forgets the tokens.
 * The local sign-out happens even if the server call fails (offline, token already expired).
 */
export async function logout(): Promise<void> {
  try {
    await api.post('/api/auth/logout')
  } catch {
    // Nothing useful to do: the user wants out either way.
  } finally {
    clearTokens()
  }
}
