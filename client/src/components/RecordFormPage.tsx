import { useRef, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import type { Permission } from '../auth/permissions.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import Forbidden from '../pages/Forbidden.tsx'
import { EntityForm, type FieldConfig } from './EntityFormModal.tsx'

type RecordFormPageProps<TValues extends Record<string, string>, TInput> = {
  /** Where "Back" and Cancel go, e.g. '/orders'. */
  backTo: string
  backLabel: string
  title: string
  subtitle?: ReactNode
  /** Permission needed to add a record, e.g. 'orders:create'. */
  permission: Permission
  fields: FieldConfig<TValues>[]
  schema: z.ZodType<TInput, TValues>
  emptyValues: TValues
  submitLabel: string
  /** Creates the record and returns its id. */
  create: (values: TInput) => Promise<string>
  /** Page opened after saving, e.g. (id) => `/orders/${id}`. */
  openCreated: (id: string) => string
  /** Shown above the form, e.g. a hint that a prerequisite record is missing. */
  notice?: ReactNode
}

/**
 * A full page for adding one record, laid out like the record's detail page (back link, title, summary panel),
 * so "Add" and "Details" feel like the same screen. After saving it opens the new record's detail page.
 */
export default function RecordFormPage<TValues extends Record<string, string>, TInput>({
  backTo,
  backLabel,
  title,
  subtitle,
  permission,
  fields,
  schema,
  emptyValues,
  submitLabel,
  create,
  openCreated,
  notice,
}: RecordFormPageProps<TValues, TInput>) {
  const navigate = useNavigate()
  const { can } = usePermissions()
  const createdId = useRef<string | null>(null)

  if (!can(permission)) return <Forbidden />

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      <Link to={backTo} className="text-sm font-medium text-blue-600 hover:underline">
        ← {backLabel}
      </Link>

      <div className="mb-6 mt-4">
        <h1 className="text-2xl font-bold">{title}</h1>
        {subtitle && <p className="mt-1 text-[1.175rem] text-slate-500">{subtitle}</p>}
      </div>

      {notice && (
        <p className="mb-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">{notice}</p>
      )}

      <div className="rounded-lg bg-slate-50 p-5">
        <EntityForm<TValues, TInput>
          twoColumns
          submitLabel={submitLabel}
          fields={fields}
          schema={schema}
          initialValues={emptyValues}
          onClose={() => navigate(backTo)}
          onSubmit={async (values) => {
            createdId.current = await create(values)
          }}
          onSaved={() => navigate(createdId.current ? openCreated(createdId.current) : backTo)}
        />
      </div>
    </div>
  )
}
