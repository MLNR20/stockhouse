import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
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
  return (
    <CrudPage<Supplier, SupplierFormValues, SupplierInput>
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
  )
}
