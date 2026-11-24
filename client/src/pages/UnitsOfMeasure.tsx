import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import {
  unitOfMeasureInputSchema,
  unitOfMeasureSchema,
  type UnitOfMeasure,
  type UnitOfMeasureFormValues,
  type UnitOfMeasureInput,
} from '../schemas/unitOfMeasure.ts'

const columns: Column<UnitOfMeasure>[] = [{ key: 'name', header: 'Name' }]

const fields: FieldConfig<UnitOfMeasureFormValues>[] = [{ name: 'name', label: 'Name' }]

const emptyValues: UnitOfMeasureFormValues = { name: '' }

export default function UnitsOfMeasure() {
  return (
    <CrudPage<UnitOfMeasure, UnitOfMeasureFormValues, UnitOfMeasureInput>
      title="Units of Measure"
      subtitle="The units your products are measured and sold in (also called how sold: Each, Pack, Case, Pallet…)."
      singular="unit"
      resource="/api/how-sold"
      schema={unitOfMeasureSchema}
      columns={columns}
      fields={fields}
      inputSchema={unitOfMeasureInputSchema}
      emptyValues={emptyValues}
      toFormValues={(unit) => ({ name: unit.name })}
      itemName={(unit) => unit.name}
      permissionResource="units"
      searchPlaceholder="Search units…"
    />
  )
}
