import { useEffect, useMemo, useSyncExternalStore, type ReactNode } from 'react'
import { clearAccess, loadAccess } from './accessStore.ts'
import { login, logout } from './authService.ts'
import { AuthContext, type AuthContextValue } from './authContext.ts'
import { getAuthState, subscribeAuth } from './authStore.ts'

/**
 * Provides the signed-in user and login / logout to the whole app (read it with useAuth()).
 *
 * The tokens live in authStore (localStorage), which keeps every tab in sync and is what the axios
 * interceptors read. This provider mirrors that state into React and, once per signed-in user,
 * asks the server whether the saved session is still good. If it is not (user removed, refresh token
 * expired or revoked), the interceptors clear the tokens and the route guard sends the user to /login.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const state = useSyncExternalStore(subscribeAuth, getAuthState)
  const userId = state.user?.id ?? null

  useEffect(() => {
    if (!userId) {
      clearAccess()
      return
    }
    // Also checks the saved session: failures are handled by the interceptors (refresh, then sign out),
    // and a network error changes nothing.
    void loadAccess()
  }, [userId])

  const value = useMemo<AuthContextValue>(
    () => ({ user: state.user, isAuthenticated: state.isAuthenticated, login, logout }),
    [state.user, state.isAuthenticated],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
