import { useMemo } from 'react'
import { api } from '../api.ts'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import RecordFormPage from '../components/RecordFormPage.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import {
  ORDER_STATUSES,
  orderInputSchema,
  orderSchema,
  type OrderFormValues,
  type OrderInput,
} from '../schemas/order.ts'
import { customerLookupSchema, type CustomerLookup } from '../schemas/shipment.ts'
import { titleCase } from '../utils/format.ts'

const emptyValues: OrderFormValues = { customerId: '', totalPrice: '', targetDeliveryDate: '', status: 'PENDING' }

const statusOptions = ORDER_STATUSES.map((status) => ({ value: status, label: titleCase(status) }))

/** Add an order on its own page, laid out like the order's detail page; its items are added right after saving. */
export default function OrderNew() {
  // Only used to offer customers, so a failure here just leaves the dropdown empty.
  const { items: customers, loading, error } = useCrud<CustomerLookup>('/api/customers', { schema: customerLookupSchema })

  const fields = useMemo<FieldConfig<OrderFormValues>[]>(
    () => [
      {
        name: 'customerId',
        label: 'Customer',
        type: 'select',
        placeholder: 'Select a customer',
        options: customers.map((c) => ({ value: c.id, label: `${c.firstName} ${c.lastName}` })),
      },
      { name: 'targetDeliveryDate', label: 'Delivery date', type: 'date' },
      { name: 'totalPrice', label: 'Order total (₱)' },
      { name: 'status', label: 'Status', type: 'select', placeholder: 'Select a status', options: statusOptions },
    ],
    [customers],
  )

  return (
    <RecordFormPage<OrderFormValues, OrderInput>
      backTo="/orders"
      backLabel="Back to orders"
      title="New order"
      subtitle="Choose the customer and delivery date. You can add the order's items on the next screen."
      permission="orders:create"
      fields={fields}
      schema={orderInputSchema}
      emptyValues={emptyValues}
      submitLabel="Create order"
      create={async (values) => {
        const { data } = await api.post('/api/orders', values)
        return orderSchema.parse(data).id
      }}
      openCreated={(id) => `/orders/${id}`}
      notice={
        !loading && !error && customers.length === 0
          ? 'There are no customers yet. An order must belong to a customer, so create one first.'
          : undefined
      }
    />
  )
}
