import { Outlet, useLocation } from 'react-router-dom'
import { usePermissions } from '../auth/usePermissions.ts'
import Forbidden from '../pages/Forbidden.tsx'
import { PageSkeleton } from './Skeleton.tsx'

/** Route guard: renders the nested routes only if the page has been assigned to the user's role. */
export default function RequirePage() {
  const { canOpen, loaded } = usePermissions()
  const { pathname } = useLocation()

  if (!loaded) return <PageSkeleton />
  return canOpen(pathname) ? <Outlet /> : <Forbidden />
}
