import { createContext } from 'react'
import type { AuthUser } from './authStore.ts'

export type AuthContextValue = {
  /** The signed-in user (read from the access token's claims), or null when signed out. */
  user: AuthUser | null
  /** True while a session exists; false after sign-out or when the server rejects the session. */
  isAuthenticated: boolean
  /** Signs in and stores the tokens. Rejects (axios error) on wrong credentials. */
  login: (email: string, password: string) => Promise<void>
  /** Ends the session on the server, then locally. Always signs out locally, even if the call fails. */
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
