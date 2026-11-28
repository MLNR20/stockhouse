import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import RecordName from '../components/RecordName.tsx'
import StatusBadge from '../components/StatusBadge.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import {
  REORDER_STATUSES,
  reorderInputSchema,
  reorderSchema,
  type Reorder,
  type ReorderFormValues,
  type ReorderInput,
} from '../schemas/reorder.ts'
import { supplierSchema, type Supplier } from '../schemas/supplier.ts'
import { unitOfMeasureSchema, type UnitOfMeasure } from '../schemas/unitOfMeasure.ts'
import { withDeleted } from '../utils/deleted.ts'
import { formatDate, humanize, titleCase } from '../utils/format.ts'

const emptyValues: ReorderFormValues = {
  supplierId: '',
  productId: '',
  quantity: '',
  howSoldId: '',
  expectedDate: '',
  status: 'DRAFT',
}

const statusOptions = REORDER_STATUSES.map((status) => ({ value: status, label: titleCase(status) }))

export default function Reorders() {
  // Only used to name and offer suppliers and products, so a failure here just leaves the dropdowns empty.
  const { items: suppliers } = useCrud<Supplier>('/api/suppliers', { schema: supplierSchema })
  const { items: products } = useCrud<Product>('/api/products', { schema: productSchema })
  const { items: units } = useCrud<UnitOfMeasure>('/api/how-sold', { schema: unitOfMeasureSchema })

  const supplierName = useMemo(() => new Map(suppliers.map((s) => [s.id, s.supplierName])), [suppliers])
  const productName = useMemo(() => new Map(products.map((p) => [p.id, p.name])), [products])

  const columns = useMemo<Column<Reorder>[]>(
    () => [
      {
        key: 'product',
        header: 'Product',
        render: (r) => productName.get(r.productId) ?? '—',
        searchValue: (r) => productName.get(r.productId),
      },
      {
        key: 'supplier',
        header: 'Supplier',
        render: (r) => supplierName.get(r.supplierId) ?? '—',
        searchValue: (r) => supplierName.get(r.supplierId),
      },
      {
        key: 'quantity',
        header: 'Quantity',
        render: (r) => r.quantity.toLocaleString(),
        searchValue: (r) => r.quantity,
      },
      {
        key: 'howSold',
        header: 'Sold as',
        render: (r) => <RecordName name={r.howSoldName} active={r.howSoldActive} />,
        searchValue: (r) => r.howSoldName,
      },
      {
        key: 'expectedDate',
        header: 'Expected',
        render: (r) => formatDate(r.expectedDate),
        searchValue: (r) => formatDate(r.expectedDate),
      },
      {
        key: 'status',
        header: 'Status',
        render: (r) => <StatusBadge status={r.status} />,
        searchValue: (r) => humanize(r.status),
      },
    ],
    [productName, supplierName],
  )

  const fields = (editing: Reorder | null): FieldConfig<ReorderFormValues>[] => [
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
      options: withDeleted(
        units.map((u) => ({ value: u.id, label: u.name })),
        ...(editing ? [{ id: editing.howSoldId, name: editing.howSoldName, active: editing.howSoldActive }] : []),
      ),
    },
    { name: 'expectedDate', label: 'Expected delivery date (optional)', type: 'date' },
    { name: 'status', label: 'Status', type: 'select', placeholder: 'Select a status', options: statusOptions },
  ]

  return (
    <CrudPage<Reorder, ReorderFormValues, ReorderInput>
      title="Reorders"
      addTo="/reorders/new"
      rowActions={(r) => (
        <Link to={`/reorders/${r.id}`} className="text-sm font-medium text-slate-700 hover:underline">
          Details
        </Link>
      )}
      subtitle="Requests to suppliers for more stock. Marking one received puts low or out-of-stock items back in stock."
      singular="reorder"
      resource="/api/reorders"
      schema={reorderSchema}
      columns={columns}
      fields={fields}
      inputSchema={reorderInputSchema}
      emptyValues={emptyValues}
      toFormValues={(r) => ({
        supplierId: r.supplierId,
        productId: r.productId,
        quantity: String(r.quantity),
        howSoldId: r.howSoldId ?? '',
        expectedDate: r.expectedDate ?? '',
        status: r.status,
      })}
      itemName={(r) => `reorder of ${productName.get(r.productId) ?? 'this product'}`}
      permissionResource="reorders"
      searchPlaceholder="Search reorders…"
    />
  )
}
