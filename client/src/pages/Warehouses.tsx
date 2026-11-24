import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import {
  warehouseInputSchema,
  warehouseRecordSchema,
  type WarehouseFormValues,
  type WarehouseInput,
  type WarehouseRecord,
} from '../schemas/warehouse.ts'

const columns: Column<WarehouseRecord>[] = [{ key: 'name', header: 'Warehouse' }]

const fields: FieldConfig<WarehouseFormValues>[] = [{ name: 'name', label: 'Warehouse name' }]

const emptyValues: WarehouseFormValues = { name: '' }

export default function Warehouses() {
  return (
    <CrudPage<WarehouseRecord, WarehouseFormValues, WarehouseInput>
      title="Warehouses"
      subtitle="The buildings that hold your stock. Each one has aisles, and each aisle has shelves."
      singular="warehouse"
      resource="/api/warehouses"
      schema={warehouseRecordSchema}
      columns={columns}
      fields={fields}
      inputSchema={warehouseInputSchema}
      emptyValues={emptyValues}
      toFormValues={(w) => ({ name: w.name })}
      itemName={(w) => w.name}
      permissionResource="warehouses"
      searchPlaceholder="Search warehouses…"
    />
  )
}
