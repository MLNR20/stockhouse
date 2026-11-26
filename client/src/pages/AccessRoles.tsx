import { useEffect, useMemo, useState } from 'react'
import { api } from '../api.ts'
import { loadAccess } from '../auth/accessStore.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import { useCrud } from '../hooks/useCrud.ts'
import { catalogSchema, roleSchema, type Catalog, type RoleRecord } from '../schemas/access.ts'
import { apiErrorMessage } from '../utils/apiError.ts'
import { titleCase } from '../utils/format.ts'
import { RolesSkeleton } from '../components/Skeleton.tsx'

/** A role being edited (or a blank one being added), kept apart from the saved record until Save. */
type Draft = { id: string | null; name: string; description: string; permissions: Set<string> }

const NEW = 'new'

const draftOf = (role: RoleRecord): Draft => ({
  id: role.id,
  name: role.name,
  description: role.description ?? '',
  permissions: new Set(role.permissions),
})

const blankDraft = (): Draft => ({ id: null, name: '', description: '', permissions: new Set() })

export default function AccessRoles() {
  const { can } = usePermissions()
  const canCreate = can('roles:create')
  const canUpdate = can('roles:update')
  const canDelete = can('roles:delete')

  const { items: roles, loading, error, create, update, remove } = useCrud<RoleRecord, Record<string, unknown>>(
    '/api/access/roles',
    { schema: roleSchema },
  )
  const [catalog, setCatalog] = useState<Catalog | null>(null)
  const [catalogError, setCatalogError] = useState<string | null>(null)

  const [selected, setSelected] = useState<string | null>(null)
  const [draft, setDraft] = useState<Draft | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    api
      .get('/api/access/catalog')
      .then((r) => setCatalog(catalogSchema.parse(r.data)))
      .catch((err: unknown) => setCatalogError(apiErrorMessage(err)))
  }, [])

  // Show the first role once the list has loaded.
  useEffect(() => {
    if (selected === null && roles.length > 0) {
      setSelected(roles[0].id)
      setDraft(draftOf(roles[0]))
    }
  }, [roles, selected])

  const current = useMemo(() => roles.find((r) => r.id === selected) ?? null, [roles, selected])
  const isAdminRole = !!current?.system && current.name.toLowerCase() === 'admin'
  const adding = selected === NEW
  const editable = adding ? canCreate : canUpdate && !isAdminRole

  function select(role: RoleRecord) {
    setSelected(role.id)
    setDraft(draftOf(role))
    setFormError(null)
    setNotice(null)
  }

  function startAdding() {
    setSelected(NEW)
    setDraft(blankDraft())
    setFormError(null)
    setNotice(null)
  }

  function toggle(permission: string) {
    setDraft((d) => {
      if (!d) return d
      const next = new Set(d.permissions)
      if (next.has(permission)) next.delete(permission)
      else next.add(permission)
      return { ...d, permissions: next }
    })
  }

  function toggleRow(resource: string, on: boolean) {
    if (!catalog) return
    setDraft((d) => {
      if (!d) return d
      const next = new Set(d.permissions)
      catalog.actions.forEach((a) => (on ? next.add(`${resource}:${a}`) : next.delete(`${resource}:${a}`)))
      return { ...d, permissions: next }
    })
  }

  async function save() {
    if (!draft) return
    if (!draft.name.trim()) {
      setFormError('Name is required')
      return
    }
    setSaving(true)
    setFormError(null)
    setNotice(null)
    const body = {
      name: draft.name.trim(),
      description: draft.description.trim() || null,
      permissions: [...draft.permissions],
      // A new role starts with no pages; assign them under Page Assignment.
      ...(adding ? { pages: [] } : {}),
    }
    try {
      if (adding) {
        const created = await create(body)
        setSelected(created.id)
        setDraft(draftOf(created))
        setNotice('Role created. Assign its pages under Page Assignment.')
      } else if (draft.id) {
        const updated = await update(draft.id, body)
        setDraft(draftOf(updated))
        setNotice('Role saved.')
        void loadAccess() // in case it is the signed-in user's own role
      }
    } catch (err) {
      setFormError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  async function deleteRole() {
    if (!current || !window.confirm(`Delete the role "${current.name}"?`)) return
    setFormError(null)
    setNotice(null)
    try {
      await remove(current.id)
      const first = roles.find((r) => r.id !== current.id)
      setSelected(first?.id ?? null)
      setDraft(first ? draftOf(first) : null)
    } catch (err) {
      setFormError(apiErrorMessage(err))
    }
  }

  const inputClass =
    'w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 disabled:bg-slate-50 disabled:text-slate-500'

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Roles &amp; Permissions</h1>
          <p className="mt-1 text-[1.175rem] text-slate-500">
            What each role may do on every area of the app. Which pages it can open is set under Page Assignment.
          </p>
        </div>
        {canCreate && (
          <button
            type="button"
            onClick={startAdding}
            className="shrink-0 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            Add role
          </button>
        )}
      </div>

      {(error || catalogError) && (
        <p className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error ?? catalogError}
        </p>
      )}

      {loading && roles.length === 0 ? (
        <RolesSkeleton />
      ) : (
        <div className="space-y-6">
          {/* One tab per role; the New role tab appears while a role is being added. */}
          <div role="tablist" aria-label="Roles" className="mb-2 flex gap-1 overflow-x-auto border-b border-slate-200">
            {roles.map((role) => {
              const active = selected === role.id
              return (
                <button
                  key={role.id}
                  type="button"
                  role="tab"
                  aria-selected={active}
                  onClick={() => select(role)}
                  className={`flex shrink-0 items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium ${
                    active
                      ? 'border-blue-600 text-blue-700'
                      : 'border-transparent text-slate-600 hover:border-slate-300 hover:text-slate-900'
                  }`}
                >
                  <span className="capitalize">{role.name}</span>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs ${
                      active ? 'bg-blue-50 text-blue-700' : 'bg-slate-100 text-slate-500'
                    }`}
                  >
                    {role.userCount}
                  </span>
                </button>
              )
            })}
            {adding && (
              <span
                role="tab"
                aria-selected
                className="shrink-0 border-b-2 border-blue-600 px-4 py-2.5 text-sm font-medium text-blue-700"
              >
                New role
              </span>
            )}
          </div>

          {draft && catalog ? (
            <div className="space-y-5">
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="role-name" className="mb-1 block text-sm font-medium text-slate-700">
                    Name
                  </label>
                  <input
                    id="role-name"
                    className={inputClass}
                    value={draft.name}
                    maxLength={50}
                    disabled={!editable || (!adding && !!current?.system)}
                    onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                  />
                </div>
                <div>
                  <label htmlFor="role-description" className="mb-1 block text-sm font-medium text-slate-700">
                    Description (optional)
                  </label>
                  <input
                    id="role-description"
                    className={inputClass}
                    value={draft.description}
                    maxLength={255}
                    disabled={!editable}
                    onChange={(e) => setDraft({ ...draft, description: e.target.value })}
                  />
                </div>
              </div>

              {isAdminRole && (
                <p className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                  The admin role always has every permission and page, so nobody can lock themselves out.
                </p>
              )}

              <div className="overflow-x-auto rounded-lg border border-slate-200">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-medium">Area</th>
                      {catalog.actions.map((a) => (
                        <th key={a} className="px-4 py-3 text-center font-medium">
                          {a}
                        </th>
                      ))}
                      <th className="px-4 py-3 text-center font-medium">All</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {catalog.resources.map((resource) => {
                      const all = catalog.actions.every((a) => draft.permissions.has(`${resource}:${a}`))
                      return (
                        <tr key={resource}>
                          <td className="px-4 py-2.5 font-medium text-slate-800">{titleCase(resource)}</td>
                          {catalog.actions.map((action) => {
                            const permission = `${resource}:${action}`
                            return (
                              <td key={action} className="px-4 py-2.5 text-center">
                                <input
                                  type="checkbox"
                                  aria-label={`${titleCase(resource)} ${action}`}
                                  className="h-4 w-4 rounded border-slate-300 accent-blue-600"
                                  checked={draft.permissions.has(permission)}
                                  disabled={!editable}
                                  onChange={() => toggle(permission)}
                                />
                              </td>
                            )
                          })}
                          <td className="px-4 py-2.5 text-center">
                            <input
                              type="checkbox"
                              aria-label={`${titleCase(resource)} all`}
                              className="h-4 w-4 rounded border-slate-300 accent-blue-600"
                              checked={all}
                              disabled={!editable}
                              onChange={(e) => toggleRow(resource, e.target.checked)}
                            />
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>

              {formError && (
                <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{formError}</p>
              )}
              {notice && (
                <p className="rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
                  {notice}
                </p>
              )}

              <div className="flex items-center justify-between gap-2">
                <div>
                  {!adding && current && !current.system && canDelete && (
                    <button
                      type="button"
                      onClick={() => void deleteRole()}
                      className="text-sm font-medium text-red-600 hover:underline"
                    >
                      Delete role
                    </button>
                  )}
                </div>
                {editable && (
                  <button
                    type="button"
                    onClick={() => void save()}
                    disabled={saving}
                    className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                  >
                    {saving ? 'Saving…' : adding ? 'Create role' : 'Save changes'}
                  </button>
                )}
              </div>
            </div>
          ) : (
            !loading && <p className="text-sm text-slate-500">Select a role to see its permissions.</p>
          )}
        </div>
      )}
    </div>
  )
}
