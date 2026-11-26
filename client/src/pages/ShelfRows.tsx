import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useShelfLabels } from '../hooks/useStorageLabels.ts'
import {
  shelfRowInputSchema,
  shelfRowSchema,
  type ShelfRow,
  type ShelfRowFormValues,
  type ShelfRowInput,
} from '../schemas/storage.ts'

const emptyValues: ShelfRowFormValues = { shelfId: '', name: '' }

export default function ShelfRows() {
  const { shelves, labels, loading, error } = useShelfLabels()

  const columns = useMemo<Column<ShelfRow>[]>(
    () => [
      { key: 'name', header: 'Row' },
      { key: 'shelf', header: 'Shelf', render: (r) => labels.get(r.shelfId) ?? '—' },
    ],
    [labels],
  )

  const fields = useMemo<FieldConfig<ShelfRowFormValues>[]>(
    () => [
      {
        name: 'shelfId',
        label: 'Shelf',
        type: 'select',
        placeholder: 'Select a shelf',
        options: shelves.map((s) => ({ value: s.id, label: labels.get(s.id) ?? s.name })),
      },
      { name: 'name', label: 'Row name' },
    ],
    [shelves, labels],
  )

  return (
    <CrudPage<ShelfRow, ShelfRowFormValues, ShelfRowInput>
      title="Shelf Rows"
      subtitle="The rows on each shelf."
      singular="shelf row"
      resource="/api/rows"
      schema={shelfRowSchema}
      columns={columns}
      fields={fields}
      inputSchema={shelfRowInputSchema}
      emptyValues={emptyValues}
      toFormValues={(r) => ({ shelfId: r.shelfId, name: r.name })}
      itemName={(r) => r.name}
      permissionResource="shelves"
      searchPlaceholder="Search rows…"
      notice={
        !loading && !error && shelves.length === 0
          ? 'There are no shelves yet. A row must belong to a shelf, so create one first.'
          : undefined
      }
    />
  )
}
