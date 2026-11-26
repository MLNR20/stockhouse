import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import {
  customerInputSchema,
  customerSchema,
  type Customer,
  type CustomerFormValues,
  type CustomerInput,
} from '../schemas/customer.ts'

const columns: Column<Customer>[] = [
  { key: 'name', header: 'Name', render: (c) => `${c.firstName} ${c.lastName}` },
  { key: 'email', header: 'Email' },
  { key: 'phoneNumber', header: 'Phone' },
  { key: 'address', header: 'Address' },
]

const fields: FieldConfig<CustomerFormValues>[] = [
  { name: 'firstName', label: 'First name' },
  { name: 'lastName', label: 'Last name' },
  { name: 'email', label: 'Email' },
  { name: 'phoneNumber', label: 'Phone number' },
  { name: 'address', label: 'Address' },
]

const emptyValues: CustomerFormValues = { firstName: '', lastName: '', email: '', phoneNumber: '', address: '' }

export default function Customers() {
  return (
    <CrudPage<Customer, CustomerFormValues, CustomerInput>
      title="Customers"
      subtitle="The people you sell to."
      singular="customer"
      resource="/api/customers"
      schema={customerSchema}
      columns={columns}
      fields={fields}
      inputSchema={customerInputSchema}
      emptyValues={emptyValues}
      toFormValues={(c) => ({
        firstName: c.firstName,
        lastName: c.lastName,
        email: c.email,
        phoneNumber: c.phoneNumber,
        address: c.address,
      })}
      itemName={(c) => `${c.firstName} ${c.lastName}`}
      permissionResource="customers"
      searchPlaceholder="Search customers…"
    />
  )
}
