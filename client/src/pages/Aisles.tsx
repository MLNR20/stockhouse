import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import {
  aisleInputSchema,
  aisleSchema,
  warehouseSchema,
  type Aisle,
  type AisleFormValues,
  type AisleInput,
  type Warehouse,
} from '../schemas/aisle.ts'

const emptyValues: AisleFormValues = { warehouseId: '', name: '' }

export default function Aisles() {
  const {
    items: warehouses,
    loading: warehousesLoading,
    error: warehousesError,
  } = useCrud<Warehouse>('/api/warehouses', { schema: warehouseSchema })

  const warehouseName = useMemo(() => new Map(warehouses.map((w) => [w.id, w.name])), [warehouses])

  const columns = useMemo<Column<Aisle>[]>(
    () => [
      { key: 'name', header: 'Aisle' },
      {
        key: 'warehouse',
        header: 'Warehouse',
        render: (a) => warehouseName.get(a.warehouseId) ?? '—',
        searchValue: (a) => warehouseName.get(a.warehouseId),
      },
    ],
    [warehouseName],
  )

  const fields = useMemo<FieldConfig<AisleFormValues>[]>(
    () => [
      {
        name: 'warehouseId',
        label: 'Warehouse',
        type: 'select',
        placeholder: 'Select a warehouse',
        options: warehouses.map((w) => ({ value: w.id, label: w.name })),
      },
      { name: 'name', label: 'Aisle name' },
    ],
    [warehouses],
  )

  return (
    <CrudPage<Aisle, AisleFormValues, AisleInput>
      title="Aisles"
      subtitle="The aisles in each warehouse."
      singular="aisle"
      resource="/api/aisles"
      schema={aisleSchema}
      columns={columns}
      fields={fields}
      inputSchema={aisleInputSchema}
      emptyValues={emptyValues}
      toFormValues={(a) => ({ warehouseId: a.warehouseId, name: a.name })}
      itemName={(a) => a.name}
      permissionResource="aisles"
      searchPlaceholder="Search aisles…"
      notice={
        // Only claim "none" when the list really loaded; a failed load is reported by the page's error banner.
        !warehousesLoading && !warehousesError && warehouses.length === 0
          ? 'There are no warehouses yet. An aisle must belong to a warehouse, so create one first.'
          : undefined
      }
    />
  )
}
