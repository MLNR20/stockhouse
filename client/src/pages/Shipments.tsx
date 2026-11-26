import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import StatusBadge from '../components/StatusBadge.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { employeeSchema, type Employee } from '../schemas/employee.ts'
import {
  SHIPMENT_STATUSES,
  customerLookupSchema,
  orderLookupSchema,
  shipmentInputSchema,
  shipmentSchema,
  type CustomerLookup,
  type OrderLookup,
  type Shipment,
  type ShipmentFormValues,
  type ShipmentInput,
} from '../schemas/shipment.ts'
import { truckSchema, type Truck } from '../schemas/truck.ts'
import { formatDate, formatMoney, humanize, shortId, titleCase } from '../utils/format.ts'

const emptyValues: ShipmentFormValues = {
  orderId: '',
  truckId: '',
  employeeId: '',
  scheduledDate: '',
  status: 'SCHEDULED',
}

const statusOptions = SHIPMENT_STATUSES.map((status) => ({ value: status, label: titleCase(status) }))

export default function Shipments() {
  // Lookups, only used to show names and fill the dropdowns.
  const { items: orders } = useCrud<OrderLookup>('/api/orders', { schema: orderLookupSchema })
  const { items: customers } = useCrud<CustomerLookup>('/api/customers', { schema: customerLookupSchema })
  const { items: trucks } = useCrud<Truck>('/api/trucks', { schema: truckSchema })
  const { items: employees } = useCrud<Employee>('/api/warehouse-employees', { schema: employeeSchema })

  const customerName = useMemo(
    () => new Map(customers.map((c) => [c.id, `${c.firstName} ${c.lastName}`])),
    [customers],
  )
  const orderLabel = useMemo(
    () =>
      new Map(
        orders.map((o) => [o.id, `#${shortId(o.id)} · ${customerName.get(o.customerId) ?? 'Unknown customer'}`]),
      ),
    [orders, customerName],
  )
  const truckPlate = useMemo(() => new Map(trucks.map((t) => [t.id, t.plateNumber])), [trucks])
  const employeeName = useMemo(
    () => new Map(employees.map((e) => [e.id, `${e.firstName} ${e.lastName}`])),
    [employees],
  )

  const columns = useMemo<Column<Shipment>[]>(
    () => [
      {
        key: 'order',
        header: 'Order',
        render: (s) => orderLabel.get(s.orderId) ?? `#${shortId(s.orderId)}`,
        searchValue: (s) => orderLabel.get(s.orderId),
      },
      {
        key: 'truck',
        header: 'Truck',
        render: (s) => truckPlate.get(s.truckId) ?? '—',
        searchValue: (s) => truckPlate.get(s.truckId),
      },
      {
        key: 'employee',
        header: 'Handled by',
        render: (s) => (s.employeeId ? (employeeName.get(s.employeeId) ?? '—') : 'Unassigned'),
        searchValue: (s) => (s.employeeId ? employeeName.get(s.employeeId) : 'unassigned'),
      },
      {
        key: 'scheduledDate',
        header: 'Scheduled',
        render: (s) => formatDate(s.scheduledDate),
        searchValue: (s) => formatDate(s.scheduledDate),
      },
      {
        key: 'status',
        header: 'Status',
        render: (s) => <StatusBadge status={s.status} />,
        searchValue: (s) => humanize(s.status),
      },
    ],
    [orderLabel, truckPlate, employeeName],
  )

  const fields = useMemo<FieldConfig<ShipmentFormValues>[]>(
    () => [
      {
        name: 'orderId',
        label: 'Order',
        type: 'select',
        placeholder: 'Select an order',
        options: orders.map((o) => ({
          value: o.id,
          label: `${orderLabel.get(o.id) ?? `#${shortId(o.id)}`} · ${formatMoney(o.totalPrice)} (${humanize(o.status)})`,
        })),
      },
      {
        name: 'truckId',
        label: 'Truck',
        type: 'select',
        placeholder: 'Select a truck',
        options: trucks.map((t) => ({ value: t.id, label: `${t.plateNumber} · ${t.model} (${humanize(t.status)})` })),
      },
      {
        name: 'employeeId',
        label: 'Handled by',
        type: 'select',
        placeholder: 'Unassigned',
        options: employees.map((e) => ({ value: e.id, label: `${e.firstName} ${e.lastName}` })),
      },
      { name: 'scheduledDate', label: 'Scheduled date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', placeholder: 'Select a status', options: statusOptions },
    ],
    [orders, orderLabel, trucks, employees],
  )

  return (
    <CrudPage<Shipment, ShipmentFormValues, ShipmentInput>
      title="Shipments"
      subtitle="Deliveries of orders: an order can have many shipments, and a truck carries many."
      singular="shipment"
      resource="/api/shipments"
      schema={shipmentSchema}
      columns={columns}
      fields={fields}
      inputSchema={shipmentInputSchema}
      emptyValues={emptyValues}
      toFormValues={(s) => ({
        orderId: s.orderId,
        truckId: s.truckId,
        employeeId: s.employeeId ?? '',
        scheduledDate: s.scheduledDate,
        status: s.status,
      })}
      itemName={(s) => `shipment for ${orderLabel.get(s.orderId) ?? `#${shortId(s.orderId)}`}`}
      permissionResource="shipments"
      searchPlaceholder="Search shipments…"
    />
  )
}
