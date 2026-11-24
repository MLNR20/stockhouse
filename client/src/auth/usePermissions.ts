import { useCallback, useSyncExternalStore } from 'react'
import { matchPage, type Permission } from './permissions.ts'
import { getAccess, subscribeAccess } from './accessStore.ts'

/** The user's role and what it allows. Re-renders whenever the access is (re)loaded. */
export function usePermissions() {
  const access = useSyncExternalStore(subscribeAccess, getAccess)

  const can = useCallback((permission: Permission) => access.permissions.has(permission), [access])

  /** Whether the page (or a path under it, such as '/orders/123') has been assigned to the role. */
  const canOpen = useCallback(
    (pathname: string) => {
      const page = matchPage(pathname)
      return page === undefined || access.pages.has(page)
    },
    [access],
  )

  return { role: access.role, loaded: access.loaded, pages: access.pages, can, canOpen }
}
