import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import StatusBadge from '../components/StatusBadge.tsx'
import {
  TRUCK_STATUSES,
  truckInputSchema,
  truckSchema,
  type Truck,
  type TruckFormValues,
  type TruckInput,
} from '../schemas/truck.ts'
import { humanize, titleCase } from '../utils/format.ts'

const columns: Column<Truck>[] = [
  { key: 'plateNumber', header: 'Plate number' },
  { key: 'model', header: 'Model' },
  {
    key: 'capacityKg',
    header: 'Capacity',
    render: (truck) => `${truck.capacityKg.toLocaleString()} kg`,
    searchValue: (truck) => truck.capacityKg,
  },
  {
    key: 'status',
    header: 'Status',
    render: (truck) => <StatusBadge status={truck.status} />,
    searchValue: (truck) => humanize(truck.status),
  },
]

const fields: FieldConfig<TruckFormValues>[] = [
  { name: 'plateNumber', label: 'Plate number' },
  { name: 'model', label: 'Model' },
  { name: 'capacityKg', label: 'Capacity (kg)' },
  {
    name: 'status',
    label: 'Status',
    type: 'select',
    placeholder: 'Select a status',
    options: TRUCK_STATUSES.map((status) => ({ value: status, label: titleCase(status) })),
  },
]

const emptyValues: TruckFormValues = { plateNumber: '', model: '', capacityKg: '', status: 'AVAILABLE' }

export default function Trucks() {
  return (
    <CrudPage<Truck, TruckFormValues, TruckInput>
      title="Trucks"
      subtitle="The delivery fleet. Each truck can carry many shipments."
      singular="truck"
      resource="/api/trucks"
      schema={truckSchema}
      columns={columns}
      fields={fields}
      inputSchema={truckInputSchema}
      emptyValues={emptyValues}
      toFormValues={(t) => ({
        plateNumber: t.plateNumber,
        model: t.model,
        capacityKg: String(t.capacityKg),
        status: t.status,
      })}
      itemName={(t) => t.plateNumber}
      permissionResource="trucks"
      searchPlaceholder="Search trucks…"
    />
  )
}
