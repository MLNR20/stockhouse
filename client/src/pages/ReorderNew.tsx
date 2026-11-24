import { useMemo } from 'react'
import { api } from '../api.ts'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import RecordFormPage from '../components/RecordFormPage.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import {
  REORDER_STATUSES,
  reorderInputSchema,
  reorderSchema,
  type ReorderFormValues,
  type ReorderInput,
} from '../schemas/reorder.ts'
import { supplierSchema, type Supplier } from '../schemas/supplier.ts'
import { unitOfMeasureSchema, type UnitOfMeasure } from '../schemas/unitOfMeasure.ts'
import { titleCase } from '../utils/format.ts'

const emptyValues: ReorderFormValues = {
  supplierId: '',
  productId: '',
  quantity: '',
  howSoldId: '',
  expectedDate: '',
  status: 'DRAFT',
}

const statusOptions = REORDER_STATUSES.map((status) => ({ value: status, label: titleCase(status) }))

/** Add a reorder on its own page, laid out like the reorder's detail page, which opens after saving. */
export default function ReorderNew() {
  // Only used to offer suppliers and products, so a failure here just leaves the dropdowns empty.
  const { items: suppliers } = useCrud<Supplier>('/api/suppliers', { schema: supplierSchema })
  const { items: products } = useCrud<Product>('/api/products', { schema: productSchema })
  const { items: units } = useCrud<UnitOfMeasure>('/api/how-sold', { schema: unitOfMeasureSchema })

  const fields = useMemo<FieldConfig<ReorderFormValues>[]>(
    () => [
      {
        name: 'supplierId',
        label: 'Supplier',
        type: 'select',
        placeholder: 'Select a supplier',
        options: suppliers.map((s) => ({ value: s.id, label: s.supplierName })),
      },
      {
        name: 'productId',
        label: 'Product (must be one this supplier supplies)',
        type: 'select',
        placeholder: 'Select a product',
        options: products.map((p) => ({ value: p.id, label: p.name })),
      },
      { name: 'quantity', label: 'Quantity', type: 'number' },
      {
        name: 'howSoldId',
        label: 'Sold as (optional)',
        type: 'select',
        placeholder: 'No unit',
        options: units.map((u) => ({ value: u.id, label: u.name })),
      },
      { name: 'expectedDate', label: 'Expected delivery date (optional)', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', placeholder: 'Select a status', options: statusOptions },
    ],
    [suppliers, products, units],
  )

  return (
    <RecordFormPage<ReorderFormValues, ReorderInput>
      backTo="/reorders"
      backLabel="Back to reorders"
      title="New reorder"
      subtitle="A request to a supplier for more stock. Its stock and unit details appear once it is saved."
      permission="reorders:create"
      fields={fields}
      schema={reorderInputSchema}
      emptyValues={emptyValues}
      submitLabel="Create reorder"
      create={async (values) => {
        const { data } = await api.post('/api/reorders', values)
        return reorderSchema.parse(data).id
      }}
      openCreated={(id) => `/reorders/${id}`}
    />
  )
}
