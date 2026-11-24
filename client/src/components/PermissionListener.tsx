import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { onAccessChange, loadAccess } from '../auth/accessStore.ts'
import { firstAllowedPath, matchPage } from '../auth/permissions.ts'

/**
 * Keeps the user's access fresh (reloaded when the tab regains focus and every minute) and reacts when it changed
 * because an administrator edited the role or reassigned the user: shows a notice and, if the current page is no
 * longer allowed, moves to one that is.
 */
export default function PermissionListener() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const pathnameRef = useRef(pathname)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    pathnameRef.current = pathname
  }, [pathname])

  useEffect(
    () =>
      onAccessChange((next, previous) => {
        setNotice(
          next.role !== previous.role
            ? `Your role changed to "${next.role ?? 'none'}".`
            : 'Your permissions were updated.',
        )
        const page = matchPage(pathnameRef.current)
        if (page && !next.pages.has(page)) navigate(firstAllowedPath(next.pages), { replace: true })
      }),
    [navigate],
  )

  useEffect(() => {
    const refresh = () => {
      if (document.visibilityState === 'visible') void loadAccess()
    }
    document.addEventListener('visibilitychange', refresh)
    window.addEventListener('focus', refresh)
    const timer = window.setInterval(refresh, 60_000)
    return () => {
      document.removeEventListener('visibilitychange', refresh)
      window.removeEventListener('focus', refresh)
      window.clearInterval(timer)
    }
  }, [])

  useEffect(() => {
    if (!notice) return
    const timer = setTimeout(() => setNotice(null), 4000)
    return () => clearTimeout(timer)
  }, [notice])

  if (!notice) return null

  return (
    <div
      role="status"
      className="fixed bottom-4 right-4 z-[70] rounded-md bg-slate-900 px-4 py-3 text-sm text-white shadow-lg"
    >
      {notice}
    </div>
  )
}
