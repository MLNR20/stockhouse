import { useState } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import SupplierProducts from '../components/SupplierProducts.tsx'
import {
  supplierInputSchema,
  supplierSchema,
  type Supplier,
  type SupplierFormValues,
  type SupplierInput,
} from '../schemas/supplier.ts'

const columns: Column<Supplier>[] = [
  { key: 'supplierName', header: 'Supplier' },
  { key: 'phoneNumber', header: 'Phone' },
  { key: 'email', header: 'Email' },
  {
    key: 'products',
    header: 'Products',
    render: (supplier) => supplier.productIds.length,
    searchValue: (supplier) => supplier.productIds.length,
  },
]

const fields: FieldConfig<SupplierFormValues>[] = [
  { name: 'supplierName', label: 'Supplier name' },
  { name: 'phoneNumber', label: 'Phone number' },
  { name: 'email', label: 'Email' },
]

const emptyValues: SupplierFormValues = { supplierName: '', phoneNumber: '', email: '' }

export default function Suppliers() {
  /** The supplier whose products are being managed. */
  const [linking, setLinking] = useState<Supplier | null>(null)
  const [changes, setChanges] = useState(0)

  return (
    <>
    <CrudPage<Supplier, SupplierFormValues, SupplierInput>
      refreshKey={changes}
      rowActions={(s) => (
        <button
          type="button"
          onClick={() => setLinking(s)}
          className="text-sm font-medium text-slate-700 hover:underline"
        >
          Products
        </button>
      )}
      title="Suppliers"
      subtitle="The companies that supply your products."
      singular="supplier"
      resource="/api/suppliers"
      schema={supplierSchema}
      columns={columns}
      fields={fields}
      inputSchema={supplierInputSchema}
      emptyValues={emptyValues}
      toFormValues={(s) => ({ supplierName: s.supplierName, phoneNumber: s.phoneNumber, email: s.email })}
      itemName={(s) => s.supplierName}
      permissionResource="suppliers"
      searchPlaceholder="Search suppliers…"
    />
      <SupplierProducts supplier={linking} onClose={() => setLinking(null)} onChanged={() => setChanges((n) => n + 1)} />
    </>
  )
}
