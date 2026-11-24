import { useState, type FormEvent } from 'react'
import { z } from 'zod'
import { apiErrorMessage } from '../utils/apiError.ts'
import ComboBox from './ComboBox.tsx'
import Modal from './Modal.tsx'

export type SelectOption = { value: string; label: string }

export type FieldConfig<TValues> = {
  name: keyof TValues & string
  label: string
  type?: 'text' | 'number' | 'select' | 'date' | 'password'
  /** For type 'select'. An empty-value option is always offered first (see `placeholder`). */
  options?: SelectOption[]
  /** For type 'select': the text of the empty option, e.g. "No category". */
  placeholder?: string
}

type EntityFormModalProps<TValues extends Record<string, string>, TOutput> = {
  open: boolean
  title: string
  submitLabel: string
  fields: FieldConfig<TValues>[]
  /** Validates the raw (string) form values and produces the API payload. */
  schema: z.ZodType<TOutput, TValues>
  initialValues: TValues
  /** Changing this resets the form (e.g. the id of the record being edited). */
  resetKey: string | number
  onClose: () => void
  onSubmit: (values: TOutput) => Promise<unknown>
}

type FormProps<TValues extends Record<string, string>, TOutput> = Omit<
  EntityFormModalProps<TValues, TOutput>,
  'open' | 'title' | 'resetKey'
> & {
  /** Runs after a successful save instead of onClose (e.g. to open the new record). */
  onSaved?: () => void
  /** Lay the fields out in two columns on wide screens (for full pages; modals stay one column). */
  twoColumns?: boolean
}

export function EntityForm<TValues extends Record<string, string>, TOutput>({
  submitLabel,
  fields,
  schema,
  initialValues,
  onClose,
  onSubmit,
  onSaved,
  twoColumns = false,
}: FormProps<TValues, TOutput>) {
  const [values, setValues] = useState<TValues>(initialValues)
  const [errors, setErrors] = useState<Partial<Record<string, string>>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSubmitError(null)

    const result = schema.safeParse(values)
    if (!result.success) {
      const fieldErrors = z.flattenError(result.error).fieldErrors as Record<string, string[] | undefined>
      setErrors(Object.fromEntries(fields.map((f) => [f.name, fieldErrors[f.name]?.[0]])))
      return
    }

    setErrors({})
    setSaving(true)
    try {
      await onSubmit(result.data)
      ;(onSaved ?? onClose)()
    } catch (err) {
      setSubmitError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      className={twoColumns ? 'grid gap-4 sm:grid-cols-2' : 'space-y-4'}
    >
      {fields.map(({ name, label, type = 'text', options, placeholder }, i) => {
        const id = `field-${name}`
        const error = errors[name]
        const controlProps = {
          id,
          value: values[name],
          autoFocus: i === 0,
          'aria-invalid': error ? true : undefined,
          'aria-describedby': error ? `${id}-error` : undefined,
          className: `w-full rounded-md border px-3 py-2 text-sm outline-none focus:ring-1 ${
            error
              ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
              : 'border-slate-300 focus:border-blue-500 focus:ring-blue-500'
          }`,
        } as const
        const handleChange = (value: string) => {
          setValues((v) => ({ ...v, [name]: value }))
          if (error) setErrors((prev) => ({ ...prev, [name]: undefined }))
        }
        return (
          <div key={name}>
            <label htmlFor={id} className="mb-1 block text-sm font-medium text-slate-700">
              {label}
            </label>
            {type === 'select' ? (
              <ComboBox
                id={controlProps.id}
                value={values[name]}
                options={options ?? []}
                onChange={handleChange}
                placeholder={`${placeholder ?? 'Select…'} (type to search)`}
                autoFocus={controlProps.autoFocus}
                aria-invalid={controlProps['aria-invalid']}
                aria-describedby={controlProps['aria-describedby']}
                className={controlProps.className}
              />
            ) : (
              <input
                {...controlProps}
                type={type}
                min={type === 'number' ? 0 : undefined}
                onChange={(e) => handleChange(e.target.value)}
              />
            )}
            {error && (
              <p id={`${id}-error`} className="mt-1 text-xs text-red-600">
                {error}
              </p>
            )}
          </div>
        )
      })}

      {submitError && (
        <p
          className={`rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 ${twoColumns ? 'sm:col-span-2' : ''}`}
        >
          {submitError}
        </p>
      )}

      <div className={`flex justify-end gap-2 pt-2 ${twoColumns ? 'sm:col-span-2' : ''}`}>
        <button
          type="button"
          onClick={onClose}
          disabled={saving}
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={saving}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {saving ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}

/** Add / edit modal driven by a field list and a zod schema. */
export default function EntityFormModal<TValues extends Record<string, string>, TOutput>({
  open,
  title,
  resetKey,
  ...formProps
}: EntityFormModalProps<TValues, TOutput>) {
  return (
    <Modal open={open} title={title} onClose={formProps.onClose}>
      <EntityForm key={resetKey} {...formProps} />
    </Modal>
  )
}
