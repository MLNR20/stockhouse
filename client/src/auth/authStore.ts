import type { TokenResponse } from '../schemas/auth.ts'

const ACCESS_KEY = 'warehouse-access-token'
const REFRESH_KEY = 'warehouse-refresh-token'

export type AuthUser = { id: string; email: string; firstName: string; lastName: string }

export type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  /** Read from the access token's claims, for display only (the server is what enforces access). */
  user: AuthUser | null
  /** A session exists while there is a refresh token, even if the access token has just expired. */
  isAuthenticated: boolean
}

function read(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function decodePayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes)) as Record<string, unknown>
  } catch {
    return null
  }
}

function userFrom(accessToken: string | null): AuthUser | null {
  const claims = accessToken ? decodePayload(accessToken) : null
  if (!claims || typeof claims.sub !== 'string') return null
  return {
    id: claims.sub,
    email: typeof claims.email === 'string' ? claims.email : '',
    firstName: typeof claims.firstName === 'string' ? claims.firstName : '',
    lastName: typeof claims.lastName === 'string' ? claims.lastName : '',
  }
}

function readState(): AuthState {
  const accessToken = read(ACCESS_KEY)
  const refreshToken = read(REFRESH_KEY)
  return { accessToken, refreshToken, user: userFrom(accessToken), isAuthenticated: refreshToken !== null }
}

// useSyncExternalStore needs a stable snapshot: only rebuild it when something changed.
let state = readState()
const listeners = new Set<() => void>()

function emit() {
  state = readState()
  listeners.forEach((listener) => listener())
}

export function getAuthState(): AuthState {
  return state
}

/** Subscribe to sign-in / sign-out / token refresh. Returns an unsubscribe function. */
export function subscribeAuth(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function getAccessToken(): string | null {
  return state.accessToken
}

export function getRefreshToken(): string | null {
  return state.refreshToken
}

export function setTokens(tokens: Pick<TokenResponse, 'accessToken' | 'refreshToken'>) {
  try {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken)
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  } catch {
    // Storage blocked: the session just won't survive a reload.
  }
  emit()
}

export function clearTokens() {
  try {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
  } catch {
    // ignore
  }
  emit()
}

/** Milliseconds until the access token expires (negative if it already has), or null if unknown. */
export function accessTokenExpiresInMs(): number | null {
  const claims = state.accessToken ? decodePayload(state.accessToken) : null
  return claims && typeof claims.exp === 'number' ? claims.exp * 1000 - Date.now() : null
}

// Keep other tabs in sync: signing in or out in one tab fires `storage` in the others.
window.addEventListener('storage', (event) => {
  if (event.key === null || event.key === ACCESS_KEY || event.key === REFRESH_KEY) emit()
})
