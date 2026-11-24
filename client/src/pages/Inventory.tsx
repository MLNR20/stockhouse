import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import StatusBadge from '../components/StatusBadge.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import {
  INVENTORY_STATUSES,
  inventoryInputSchema,
  inventorySchema,
  type Inventory as InventoryRecord,
  type InventoryFormValues,
  type InventoryInput,
} from '../schemas/inventory.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import { unitOfMeasureSchema, type UnitOfMeasure } from '../schemas/unitOfMeasure.ts'
import { humanize, titleCase } from '../utils/format.ts'

const emptyValues: InventoryFormValues = {
  productId: '',
  status: 'IN_STOCK',
  baseQuantity: '0',
  minimumThreshold: '0',
  howSoldId: '',
}

const statusOptions = INVENTORY_STATUSES.map((status) => ({ value: status, label: titleCase(status) }))

export default function Inventory() {
  // Only used to name and offer products and units, so a failure here just leaves the dropdowns empty.
  const { items: products } = useCrud<Product>('/api/products', { schema: productSchema })
  const { items: units } = useCrud<UnitOfMeasure>('/api/how-sold', { schema: unitOfMeasureSchema })

  const productName = useMemo(() => new Map(products.map((p) => [p.id, p.name])), [products])
  const unitName = useMemo(() => new Map(units.map((u) => [u.id, u.name])), [units])

  const columns = useMemo<Column<InventoryRecord>[]>(
    () => [
      {
        key: 'product',
        header: 'Product',
        render: (row) => productName.get(row.productId) ?? '—',
        searchValue: (row) => productName.get(row.productId),
      },
      {
        key: 'status',
        header: 'Status',
        render: (row) => <StatusBadge status={row.status} />,
        searchValue: (row) => humanize(row.status),
      },
      {
        key: 'baseQuantity',
        header: 'Base quantity',
        render: (row) => (
          <span className={row.belowMinimum ? 'font-semibold text-red-600' : undefined}>
            {row.baseQuantity.toLocaleString()}
            {row.belowMinimum && <span className="ml-2 text-xs font-medium">below minimum</span>}
          </span>
        ),
        searchValue: (row) => row.baseQuantity,
      },
      {
        key: 'minimumThreshold',
        header: 'Minimum',
        render: (row) => row.minimumThreshold.toLocaleString(),
        searchValue: (row) => row.minimumThreshold,
      },
      {
        key: 'howSold',
        header: 'Sold as',
        render: (row) => (row.howSoldId ? (unitName.get(row.howSoldId) ?? '—') : '—'),
        searchValue: (row) => (row.howSoldId ? unitName.get(row.howSoldId) : undefined),
      },
    ],
    [productName, unitName],
  )

  const fields = useMemo<FieldConfig<InventoryFormValues>[]>(
    () => [
      {
        name: 'productId',
        label: 'Product',
        type: 'select',
        placeholder: 'Select a product',
        options: products.map((p) => ({ value: p.id, label: p.name })),
      },
      { name: 'status', label: 'Status', type: 'select', placeholder: 'Select a status', options: statusOptions },
      { name: 'baseQuantity', label: 'Base quantity (on hand)', type: 'number' },
      { name: 'minimumThreshold', label: 'Minimum threshold', type: 'number' },
      {
        name: 'howSoldId',
        label: 'How sold',
        type: 'select',
        placeholder: 'Not set',
        options: units.map((u) => ({ value: u.id, label: u.name })),
      },
    ],
    [products, units],
  )

  return (
    <CrudPage<InventoryRecord, InventoryFormValues, InventoryInput>
      title="Inventory"
      subtitle="Stock on hand for each product, the level that counts as low, and the unit it is sold in."
      singular="inventory record"
      resource="/api/inventory"
      schema={inventorySchema}
      columns={columns}
      fields={fields}
      inputSchema={inventoryInputSchema}
      emptyValues={emptyValues}
      toFormValues={(row) => ({
        productId: row.productId,
        status: row.status,
        baseQuantity: String(row.baseQuantity),
        minimumThreshold: String(row.minimumThreshold),
        howSoldId: row.howSoldId ?? '',
      })}
      itemName={(row) => productName.get(row.productId) ?? 'this record'}
      permissionResource="inventory"
      searchPlaceholder="Search inventory…"
    />
  )
}
