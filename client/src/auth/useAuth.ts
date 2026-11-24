import { useContext } from 'react'
import { AuthContext, type AuthContextValue } from './authContext.ts'

/** The signed-in user, `isAuthenticated`, and `login` / `logout`. Must be used under <AuthProvider>. */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
