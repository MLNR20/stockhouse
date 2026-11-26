import { useMemo, useState } from 'react'
import ContainerContents from '../components/ContainerContents.tsx'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { useShelfLabels } from '../hooks/useStorageLabels.ts'
import {
  containerInputSchema,
  containerSchema,
  shelfRowSchema,
  type ContainerFormValues,
  type ContainerInput,
  type ShelfRow,
  type StorageContainer,
} from '../schemas/storage.ts'

const emptyValues: ContainerFormValues = { rowId: '', name: '' }

export default function Containers() {
  const { labels: shelfLabels } = useShelfLabels()
  const { items: rows, loading, error } = useCrud<ShelfRow>('/api/rows', { schema: shelfRowSchema })
  const [opened, setOpened] = useState<StorageContainer | null>(null)

  const rowLabel = useMemo(
    () => new Map(rows.map((r) => [r.id, `${shelfLabels.get(r.shelfId) ?? '—'} / ${r.name}`])),
    [rows, shelfLabels],
  )

  const columns = useMemo<Column<StorageContainer>[]>(
    () => [
      { key: 'name', header: 'Container' },
      { key: 'row', header: 'Location', render: (c) => rowLabel.get(c.rowId) ?? '—' },
    ],
    [rowLabel],
  )

  const fields = useMemo<FieldConfig<ContainerFormValues>[]>(
    () => [
      {
        name: 'rowId',
        label: 'Shelf row',
        type: 'select',
        placeholder: 'Select a row',
        options: rows.map((r) => ({ value: r.id, label: rowLabel.get(r.id) ?? r.name })),
      },
      { name: 'name', label: 'Container name' },
    ],
    [rows, rowLabel],
  )

  return (
    <>
      <CrudPage<StorageContainer, ContainerFormValues, ContainerInput>
        title="Containers"
        subtitle="The containers in each shelf row, and what is inside them."
        singular="container"
        resource="/api/containers"
        schema={containerSchema}
        columns={columns}
        fields={fields}
        inputSchema={containerInputSchema}
        emptyValues={emptyValues}
        toFormValues={(c) => ({ rowId: c.rowId, name: c.name })}
        itemName={(c) => c.name}
        permissionResource="shelves"
        searchPlaceholder="Search containers…"
        rowActions={(c) => (
          <button
            type="button"
            onClick={() => setOpened(c)}
            className="text-sm font-medium text-slate-700 hover:underline"
          >
            Contents
          </button>
        )}
        notice={
          !loading && !error && rows.length === 0
            ? 'There are no shelf rows yet. A container must stand in a row, so create one first.'
            : undefined
        }
      />
      <ContainerContents container={opened} onClose={() => setOpened(null)} />
    </>
  )
}
