import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { aisleSchema, warehouseSchema, type Aisle, type Warehouse } from '../schemas/aisle.ts'
import {
  shelfInputSchema,
  shelfSchema,
  type Shelf,
  type ShelfFormValues,
  type ShelfInput,
} from '../schemas/shelf.ts'

const emptyValues: ShelfFormValues = { aisleId: '', name: '' }

export default function Shelves() {
  // Only used to name and offer aisles, so a failure here just leaves the dropdown empty.
  const { items: aisles, loading: aislesLoading, error: aislesError } = useCrud<Aisle>('/api/aisles', {
    schema: aisleSchema,
  })
  const { items: warehouses } = useCrud<Warehouse>('/api/warehouses', { schema: warehouseSchema })

  const warehouseName = useMemo(() => new Map(warehouses.map((w) => [w.id, w.name])), [warehouses])
  /** "Main Warehouse / Aisle A": an aisle name alone can repeat across warehouses. */
  const aisleLabel = useMemo(
    () => new Map(aisles.map((a) => [a.id, `${warehouseName.get(a.warehouseId) ?? '—'} / ${a.name}`])),
    [aisles, warehouseName],
  )

  const columns = useMemo<Column<Shelf>[]>(
    () => [
      { key: 'name', header: 'Shelf' },
      {
        key: 'aisle',
        header: 'Aisle',
        render: (s) => aisleLabel.get(s.aisleId) ?? '—',
        searchValue: (s) => aisleLabel.get(s.aisleId),
      },
    ],
    [aisleLabel],
  )

  const fields = useMemo<FieldConfig<ShelfFormValues>[]>(
    () => [
      {
        name: 'aisleId',
        label: 'Aisle',
        type: 'select',
        placeholder: 'Select an aisle',
        options: aisles.map((a) => ({ value: a.id, label: aisleLabel.get(a.id) ?? a.name })),
      },
      { name: 'name', label: 'Shelf name' },
    ],
    [aisles, aisleLabel],
  )

  return (
    <CrudPage<Shelf, ShelfFormValues, ShelfInput>
      title="Shelves"
      subtitle="The shelves in each aisle."
      singular="shelf"
      resource="/api/shelves"
      schema={shelfSchema}
      columns={columns}
      fields={fields}
      inputSchema={shelfInputSchema}
      emptyValues={emptyValues}
      toFormValues={(s) => ({ aisleId: s.aisleId, name: s.name })}
      itemName={(s) => s.name}
      permissionResource="shelves"
      searchPlaceholder="Search shelves…"
      notice={
        !aislesLoading && !aislesError && aisles.length === 0
          ? 'There are no aisles yet. A shelf must belong to an aisle, so create one first.'
          : undefined
      }
    />
  )
}
