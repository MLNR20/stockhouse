import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import {
  categoryInputSchema,
  categorySchema,
  type Category,
  type CategoryFormValues,
  type CategoryInput,
} from '../schemas/category.ts'

const columns: Column<Category>[] = [
  { key: 'name', header: 'Name' },
  { key: 'description', header: 'Description', render: (c) => c.description ?? '—' },
]

const fields: FieldConfig<CategoryFormValues>[] = [
  { name: 'name', label: 'Name' },
  { name: 'description', label: 'Description (optional)' },
]

const emptyValues: CategoryFormValues = { name: '', description: '' }

export default function Categories() {
  return (
    <CrudPage<Category, CategoryFormValues, CategoryInput>
      title="Categories"
      subtitle="The groups your products are sorted into."
      singular="category"
      resource="/api/categories"
      schema={categorySchema}
      columns={columns}
      fields={fields}
      inputSchema={categoryInputSchema}
      emptyValues={emptyValues}
      toFormValues={(c) => ({ name: c.name, description: c.description ?? '' })}
      itemName={(c) => c.name}
      permissionResource="categories"
      searchPlaceholder="Search categories…"
    />
  )
}
