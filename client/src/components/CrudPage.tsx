import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import type { Resource } from '../auth/permissions.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import { usePagedCrud } from '../hooks/usePagedCrud.ts'
import { formatDateTime } from '../utils/format.ts'
import DataTable, { type Column } from './DataTable.tsx'
import EntityFormModal, { type FieldConfig } from './EntityFormModal.tsx'
import NoticeModal from './NoticeModal.tsx'

/** "Created" and "Updated" columns, read from the createdAt / updatedAt every API record carries. */
function timestampColumns<T>(): Column<T>[] {
  const read = (row: T, key: 'createdAt' | 'updatedAt') =>
    (row as unknown as Record<string, string | null | undefined>)[key]
  return [
    {
      key: 'createdAt',
      header: 'Created',
      render: (row) => formatDateTime(read(row, 'createdAt')),
      searchValue: (row) => formatDateTime(read(row, 'createdAt')),
    },
    {
      key: 'updatedAt',
      header: 'Updated',
      render: (row) => formatDateTime(read(row, 'updatedAt')),
      searchValue: (row) => formatDateTime(read(row, 'updatedAt')),
    },
  ]
}

type CrudPageProps<T extends { id: string | number }, TValues extends Record<string, string>, TInput> = {
  title: string
  /** Lower-case singular noun, used in "Add supplier" / "Edit supplier". */
  singular: string
  /** REST collection, e.g. '/api/suppliers'. */
  resource: string
  /** Must be a stable reference (module-level constant): changing it refetches the list. */
  schema: z.ZodType<T>
  columns: Column<T>[]
  fields: FieldConfig<TValues>[]
  /** Validates the raw form strings and produces the API payload. */
  inputSchema: z.ZodType<TInput, TValues>
  /** Values for a blank "add" form. */
  emptyValues: TValues
  /** Form values for editing an existing record. */
  toFormValues: (item: T) => TValues
  /** How a record is named in the delete confirmation. */
  itemName: (item: T) => string
  /** Which permissions (`<resource>:create|update|delete`) gate the buttons. */
  permissionResource: Resource
  searchPlaceholder?: string
  /** One line under the title, saying what the page is for. */
  subtitle?: string
  /** Append "Created" and "Updated" columns (default true). */
  timestamps?: boolean
  /** Shown above the table, e.g. a hint that a prerequisite record is missing. */
  notice?: ReactNode
  /** Change this number to reload the current page (e.g. after editing a row's related records). */
  refreshKey?: number
  /** Hides Delete on rows that must never be deleted (e.g. admin accounts). Default: every row can be. */
  canDeleteRow?: (item: T) => boolean
  /** When set, "Add" opens this page (e.g. '/orders/new') instead of the pop-up form. */
  addTo?: string
  /** Extra buttons in each row's Actions cell, before Edit / Delete (e.g. a "Details" button). */
  rowActions?: (item: T) => ReactNode
}

/**
 * A full list + add / edit / delete screen for one REST resource, built from the same pieces as the
 * other pages (useCrud, DataTable, EntityFormModal) and gated by the role's permissions.
 */
export default function CrudPage<
  T extends { id: string | number },
  TValues extends Record<string, string>,
  TInput,
>({
  title,
  singular,
  resource,
  schema,
  columns,
  fields,
  inputSchema,
  emptyValues,
  toFormValues,
  itemName,
  permissionResource,
  searchPlaceholder,
  subtitle,
  timestamps = true,
  notice,
  addTo,
  canDeleteRow,
  refreshKey,
  rowActions,
}: CrudPageProps<T, TValues, TInput>) {
  // A link such as /products?q=mouse (from the Search page) opens the list already searched.
  const query = useSearchParams()[0].get('q') ?? ''
  const { items, loading, error, create, update, remove, reload, paging } = usePagedCrud<T, TInput>(
    resource,
    schema,
    query,
  )

  const { onSearchChange } = paging
  useEffect(() => {
    onSearchChange(query)
  }, [query, onSearchChange])

  useEffect(() => {
    if (refreshKey) void reload()
    // Only a new key should reload here; paging changes already reload by themselves.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey])

  // The page's own columns, then Created and Updated (every record has both).
  const allColumns = useMemo(
    () => (timestamps ? [...columns, ...timestampColumns<T>()] : columns),
    [columns, timestamps],
  )

  /** Name of the record that was just updated, shown in a confirmation dialog. */
  const [updatedName, setUpdatedName] = useState<string | null>(null)

  // undefined = closed, null = adding, T = editing
  const [editing, setEditing] = useState<T | null | undefined>(undefined)

  const { can } = usePermissions()
  const canCreate = can(`${permissionResource}:create`)
  const canUpdate = can(`${permissionResource}:update`)
  const canDelete = can(`${permissionResource}:delete`)

  return (
    <div>
      {/* One container: header (title, subheader, add button) on top, the table inside a p-4 wrapper. */}
      <div className="rounded-lg border border-slate-200 bg-white p-8">
        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold">{title}</h1>
            {subtitle && <p className="mt-1 text-[1.175rem] text-slate-500">{subtitle}</p>}
          </div>
          {canCreate &&
            (addTo ? (
              <Link
                to={addTo}
                className="shrink-0 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                Add {singular}
              </Link>
            ) : (
              <button
                type="button"
                onClick={() => setEditing(null)}
                className="shrink-0 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                Add {singular}
              </button>
            ))}
        </div>

        <div className="space-y-4">
          {notice && (
            <p className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
              {notice}
            </p>
          )}

          {error && (
            <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
          )}

          <DataTable
            bare
            columns={allColumns}
            data={items}
            rowKey={(item) => item.id}
            server={paging}
            // Keep the rows on screen while the next page is fetched.
            loading={loading && items.length === 0}
            searchPlaceholder={searchPlaceholder}
            actions={
              rowActions || canUpdate || canDelete
                ? (item) => (
                    <div className="flex justify-end gap-3">
                      {rowActions?.(item)}
                      {canUpdate && (
                        <button
                          type="button"
                          onClick={() => setEditing(item)}
                          className="text-sm font-medium text-blue-600 hover:underline"
                        >
                          Edit
                        </button>
                      )}
                      {canDelete && (canDeleteRow?.(item) ?? true) && (
                        <button
                          type="button"
                          onClick={() => {
                            if (window.confirm(`Delete "${itemName(item)}"?`)) void remove(item.id).catch(() => {})
                          }}
                          className="text-sm font-medium text-red-600 hover:underline"
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  )
                : undefined
            }
          />
        </div>
      </div>

      <EntityFormModal<TValues, TInput>
        open={editing !== undefined && (editing ? canUpdate : canCreate)}
        title={editing ? `Edit ${singular}` : `Add ${singular}`}
        submitLabel={editing ? 'Save changes' : `Add ${singular}`}
        fields={fields}
        schema={inputSchema}
        initialValues={editing ? toFormValues(editing) : emptyValues}
        resetKey={editing?.id ?? 'new'}
        onClose={() => setEditing(undefined)}
        onSubmit={async (values) => {
          if (!editing) {
            await create(values)
            return
          }
          const saved = await update(editing.id, values)
          setUpdatedName(itemName(saved ?? editing))
        }}
      />

      <NoticeModal open={updatedName !== null} title="Update saved" onClose={() => setUpdatedName(null)}>
        Your changes to the {singular} <span className="font-semibold text-slate-900">{updatedName}</span> were saved.
      </NoticeModal>
    </div>
  )
}
