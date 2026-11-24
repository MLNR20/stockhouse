import { z } from 'zod'
import { api } from '../api.ts'

/** The signed-in user's role, permissions and assigned pages: the `access` part of GET /api/auth/me. */
const meSchema = z.object({
  role: z.string().nullable(),
  permissions: z.array(z.string()),
  pages: z.array(z.string()),
})

export type AccessState = {
  /** False until the first load for this session finishes (the route guards wait for it). */
  loaded: boolean
  role: string | null
  permissions: ReadonlySet<string>
  pages: ReadonlySet<string>
}

const EMPTY: AccessState = { loaded: false, role: null, permissions: new Set(), pages: new Set() }

let state: AccessState = EMPTY
const listeners = new Set<() => void>()
/** Called with the old and new state whenever a reload finds that the role or the allowed pages changed. */
const changeListeners = new Set<(next: AccessState, previous: AccessState) => void>()

export function getAccess(): AccessState {
  return state
}

export function subscribeAccess(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function onAccessChange(listener: (next: AccessState, previous: AccessState) => void): () => void {
  changeListeners.add(listener)
  return () => {
    changeListeners.delete(listener)
  }
}

function sameSet(a: ReadonlySet<string>, b: ReadonlySet<string>) {
  return a.size === b.size && [...a].every((x) => b.has(x))
}

/** Asks the server what the current user may do. A network error keeps whatever was loaded before. */
export async function loadAccess(): Promise<void> {
  try {
    const { data } = await api.get('/api/auth/me')
    const me = meSchema.parse(data)
    const next: AccessState = {
      loaded: true,
      role: me.role,
      permissions: new Set(me.permissions),
      pages: new Set(me.pages),
    }
    const previous = state
    state = next
    listeners.forEach((l) => l())
    const changed =
      previous.loaded &&
      (previous.role !== next.role ||
        !sameSet(previous.permissions, next.permissions) ||
        !sameSet(previous.pages, next.pages))
    if (changed) changeListeners.forEach((l) => l(next, previous))
  } catch {
    // 401s are handled by the axios interceptors (sign out); anything else keeps the previous access.
  }
}

/** Forgets the access (on sign-out), so the next user starts from nothing. */
export function clearAccess() {
  if (state === EMPTY) return
  state = EMPTY
  listeners.forEach((l) => l())
}
