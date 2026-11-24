import { Link } from 'react-router-dom'
import { firstAllowedPath } from '../auth/permissions.ts'
import { usePermissions } from '../auth/usePermissions.ts'

export default function Forbidden() {
  const { role, pages } = usePermissions()
  const home = firstAllowedPath(pages)

  return (
    <div className="space-y-2">
      <h1 className="text-2xl font-bold">403 – Access denied</h1>
      <p className="text-slate-600">
        {role ? (
          <>
            Your role (<span className="font-medium">{role}</span>) doesn't have access to this page.
          </>
        ) : (
          "You don't have a role yet, so no pages are available. Ask an administrator to assign one."
        )}
      </p>
      {home !== '/forbidden' && (
        <Link to={home} className="inline-block text-sm font-medium text-blue-600 hover:underline">
          Go to an allowed page
        </Link>
      )}
    </div>
  )
}
