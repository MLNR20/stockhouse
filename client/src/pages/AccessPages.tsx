import { useEffect, useMemo, useState } from 'react'
import { api } from '../api.ts'
import { loadAccess } from '../auth/accessStore.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import { useCrud } from '../hooks/useCrud.ts'
import { catalogSchema, roleSchema, type Catalog, type RoleRecord } from '../schemas/access.ts'
import { apiErrorMessage } from '../utils/apiError.ts'

const isAdmin = (role: RoleRecord) => role.system && role.name.toLowerCase() === 'admin'

/** Page assignment: a grid of pages (rows) by roles (columns). Ticks are staged until "Save changes". */
export default function AccessPages() {
  const { can } = usePermissions()
  const canUpdate = can('pages:update')

  const { items: roles, loading, error, fetchAll } = useCrud<RoleRecord>('/api/access/roles', { schema: roleSchema })
  const [catalog, setCatalog] = useState<Catalog | null>(null)
  const [catalogError, setCatalogError] = useState<string | null>(null)

  /** Pages per role id, edited locally. */
  const [draft, setDraft] = useState<Record<string, Set<string>>>({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    api
      .get('/api/access/catalog')
      .then((r) => setCatalog(catalogSchema.parse(r.data)))
      .catch((err: unknown) => setCatalogError(apiErrorMessage(err)))
  }, [])

  // Start from what the server has whenever the list is (re)loaded.
  useEffect(() => {
    setDraft(Object.fromEntries(roles.map((r) => [r.id, new Set(r.pages)])))
  }, [roles])

  const changedRoles = useMemo(
    () =>
      roles.filter((r) => {
        const now = draft[r.id]
        return now && (now.size !== r.pages.length || r.pages.some((p) => !now.has(p)))
      }),
    [roles, draft],
  )

  const sections = useMemo(() => {
    const map = new Map<string, Catalog['pages']>()
    catalog?.pages.forEach((page) => map.set(page.section, [...(map.get(page.section) ?? []), page]))
    return [...map.entries()]
  }, [catalog])

  function toggle(roleId: string, path: string) {
    setNotice(null)
    setDraft((d) => {
      const next = new Set(d[roleId])
      if (next.has(path)) next.delete(path)
      else next.add(path)
      return { ...d, [roleId]: next }
    })
  }

  async function save() {
    setSaving(true)
    setSaveError(null)
    setNotice(null)
    try {
      for (const role of changedRoles) {
        await api.put(`/api/access/roles/${role.id}/pages`, { pages: [...draft[role.id]] })
      }
      await fetchAll()
      void loadAccess() // in case the signed-in user's own role changed
      setNotice('Page assignment saved.')
    } catch (err) {
      setSaveError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Page Assignment</h1>
          <p className="mt-1 text-[1.175rem] text-slate-500">
            Choose which pages each role can open. Pages that are not ticked disappear from that role's menu.
          </p>
        </div>
        {canUpdate && (
          <div className="flex shrink-0 gap-2">
            <button
              type="button"
              onClick={() => setDraft(Object.fromEntries(roles.map((r) => [r.id, new Set(r.pages)])))}
              disabled={saving || changedRoles.length === 0}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
            >
              Reset
            </button>
            <button
              type="button"
              onClick={() => void save()}
              disabled={saving || changedRoles.length === 0}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {saving ? 'Saving…' : 'Save changes'}
            </button>
          </div>
        )}
      </div>

      <div className="space-y-4">
        {(error || catalogError || saveError) && (
          <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {saveError ?? error ?? catalogError}
          </p>
        )}
        {notice && (
          <p className="rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">{notice}</p>
        )}

        {loading && roles.length === 0 ? (
          <p className="text-sm text-slate-500">Loading…</p>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-slate-200">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-medium">Page</th>
                  {roles.map((role) => (
                    <th key={role.id} className="px-4 py-3 text-center font-medium">
                      {role.name}
                    </th>
                  ))}
                </tr>
              </thead>
              {sections.map(([section, pages]) => (
                <tbody key={section} className="divide-y divide-slate-100 border-t border-slate-200">
                  <tr className="bg-slate-50/60">
                    <th
                      colSpan={roles.length + 1}
                      className="px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400"
                    >
                      {section}
                    </th>
                  </tr>
                  {pages.map((page) => (
                    <tr key={page.path}>
                      <td className="px-4 py-2.5 font-medium text-slate-800">
                        {page.label}
                        <span className="ml-2 text-xs font-normal text-slate-400">{page.path}</span>
                      </td>
                      {roles.map((role) => (
                        <td key={role.id} className="px-4 py-2.5 text-center">
                          <input
                            type="checkbox"
                            aria-label={`${role.name}: ${page.label}`}
                            className="h-4 w-4 rounded border-slate-300 accent-blue-600"
                            checked={isAdmin(role) || (draft[role.id]?.has(page.path) ?? false)}
                            disabled={!canUpdate || isAdmin(role) || saving}
                            onChange={() => toggle(role.id, page.path)}
                          />
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              ))}
            </table>
          </div>
        )}
        <p className="text-xs text-slate-400">The admin role always has every page.</p>
      </div>
    </div>
  )
}
