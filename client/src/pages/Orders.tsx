import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import StatusBadge from '../components/StatusBadge.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import {
  ORDER_STATUSES,
  orderInputSchema,
  orderSchema,
  type Order,
  type OrderFormValues,
  type OrderInput,
} from '../schemas/order.ts'
import { customerLookupSchema, type CustomerLookup } from '../schemas/shipment.ts'
import { formatDate, humanize, shortId, titleCase } from '../utils/format.ts'

const emptyValues: OrderFormValues = { customerId: '', totalPrice: '', targetDeliveryDate: '', status: 'PENDING' }

const statusOptions = ORDER_STATUSES.map((status) => ({ value: status, label: titleCase(status) }))

const formatMoney = (amount: number) =>
  amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export default function Orders() {
  // Only used to name and offer customers, so a failure here just leaves the dropdown empty.
  const { items: customers } = useCrud<CustomerLookup>('/api/customers', { schema: customerLookupSchema })

  const customerName = useMemo(
    () => new Map(customers.map((c) => [c.id, `${c.firstName} ${c.lastName}`])),
    [customers],
  )

  const columns = useMemo<Column<Order>[]>(
    () => [
      {
        key: 'order',
        header: 'Order',
        render: (o) => `#${shortId(o.id)}`,
        searchValue: (o) => shortId(o.id),
      },
      {
        key: 'customer',
        header: 'Customer',
        render: (o) => customerName.get(o.customerId) ?? '—',
        searchValue: (o) => customerName.get(o.customerId),
      },
      {
        key: 'totalPrice',
        header: 'Total',
        render: (o) => formatMoney(o.totalPrice),
        searchValue: (o) => o.totalPrice,
      },
      {
        key: 'targetDeliveryDate',
        header: 'Delivery date',
        render: (o) => formatDate(o.targetDeliveryDate),
        searchValue: (o) => formatDate(o.targetDeliveryDate),
      },
      {
        key: 'status',
        header: 'Status',
        render: (o) => <StatusBadge status={o.status} />,
        searchValue: (o) => humanize(o.status),
      },
    ],
    [customerName],
  )

  const fields = useMemo<FieldConfig<OrderFormValues>[]>(
    () => [
      {
        name: 'customerId',
        label: 'Customer',
        type: 'select',
        placeholder: 'Select a customer',
        options: customers.map((c) => ({ value: c.id, label: `${c.firstName} ${c.lastName}` })),
      },
      { name: 'totalPrice', label: 'Total price' },
      { name: 'targetDeliveryDate', label: 'Target delivery date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', placeholder: 'Select a status', options: statusOptions },
    ],
    [customers],
  )

  return (
    <CrudPage<Order, OrderFormValues, OrderInput>
      title="Orders"
      addTo="/orders/new"
      subtitle="Customer orders: what they cost, when they are due and where they stand."
      rowActions={(o) => (
        <Link to={`/orders/${o.id}`} className="text-sm font-medium text-slate-700 hover:underline">
          Details
        </Link>
      )}
      singular="order"
      resource="/api/orders"
      schema={orderSchema}
      columns={columns}
      fields={fields}
      inputSchema={orderInputSchema}
      emptyValues={emptyValues}
      toFormValues={(o) => ({
        customerId: o.customerId,
        totalPrice: o.totalPrice.toFixed(2),
        targetDeliveryDate: o.targetDeliveryDate,
        status: o.status,
      })}
      itemName={(o) => `order #${shortId(o.id)}`}
      permissionResource="orders"
      searchPlaceholder="Search orders…"
    />
  )
}
