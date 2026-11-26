import axios, { type InternalAxiosRequestConfig } from 'axios'
import {
  accessTokenExpiresInMs,
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setTokens,
} from './auth/authStore.ts'
import { tokenResponseSchema } from './schemas/auth.ts'

// Set VITE_API_URL in client/.env to point at the backend.
export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export const api = axios.create({ baseURL: API_URL })

/** Endpoints that must not carry a token or trigger a refresh. */
const isAuthPath = (url?: string) =>
  !!url && /\/api\/auth\/(login|register|refresh|forgot-password|reset-password)$/.test(url)

let refreshing: Promise<string | null> | null = null

/**
 * Trades the refresh token for a new pair. Concurrent callers share one request.
 * Resolves to the new access token, or null if there is no session or the server rejected it
 * (in which case the session is cleared and the route guard sends the user to /login).
 * A network error or 5xx keeps the session so a brief outage does not sign the user out.
 */
export function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return Promise.resolve(null)

  refreshing ??= axios
    .post(`${API_URL}/api/auth/refresh`, { refreshToken })
    .then(({ data }) => {
      const tokens = tokenResponseSchema.parse(data)
      setTokens(tokens)
      return tokens.accessToken
    })
    .catch((err: unknown) => {
      if (axios.isAxiosError(err) && err.response && err.response.status < 500) clearTokens()
      return null
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

/** The current access token, refreshed first if it is about to expire. */
export async function ensureFreshAccessToken(): Promise<string | null> {
  const msLeft = accessTokenExpiresInMs()
  if (getAccessToken() && msLeft !== null && msLeft > 30_000) return getAccessToken()
  return refreshAccessToken()
}

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && !isAuthPath(config.url)) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

type RetryableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const original = error.config as RetryableConfig | undefined
      if (original && !original._retried && !isAuthPath(original.url)) {
        original._retried = true
        const token = await refreshAccessToken()
        if (token) {
          original.headers.set('Authorization', `Bearer ${token}`)
          return api(original)
        }
      }
    }
    return Promise.reject(error)
  },
)
