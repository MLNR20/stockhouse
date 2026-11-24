import axios from 'axios'
import { z } from 'zod'

/**
 * A message a person can read for any error thrown while calling the API.
 * Prefers the server's own reason (e.g. "Truck still has shipments", "firstName must contain letters only"),
 * then a plain-English line for the common status codes, then the raw error text.
 */
export function apiErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const serverMessage = err.response?.data?.message
    if (typeof serverMessage === 'string' && serverMessage) return serverMessage
    switch (err.response?.status) {
      case 400:
        return 'The server rejected some of the values. Check them and try again.'
      case 403:
        return 'You do not have permission to do that.'
      case 404:
        return 'That record no longer exists.'
      case 409:
        return 'This record is still in use by other records, so it cannot be changed or removed.'
    }
    return err.message
  }
  if (err instanceof z.ZodError) {
    return `Unexpected response from server: ${err.issues[0]?.message ?? 'validation failed'}`
  }
  return err instanceof Error ? err.message : 'Something went wrong'
}
