import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { warehouseSchema, type Warehouse } from '../schemas/aisle.ts'
import {
  employeeInputSchema,
  employeeSchema,
  type Employee,
  type EmployeeFormValues,
  type EmployeeInput,
} from '../schemas/employee.ts'

const emptyValues: EmployeeFormValues = { warehouseId: '', firstName: '', lastName: '', email: '', phoneNumber: '' }

export default function Employees() {
  const {
    items: warehouses,
    loading: warehousesLoading,
    error: warehousesError,
  } = useCrud<Warehouse>('/api/warehouses', { schema: warehouseSchema })

  const warehouseName = useMemo(() => new Map(warehouses.map((w) => [w.id, w.name])), [warehouses])

  const columns = useMemo<Column<Employee>[]>(
    () => [
      {
        key: 'name',
        header: 'Name',
        render: (e) => `${e.firstName} ${e.lastName}`,
        searchValue: (e) => `${e.firstName} ${e.lastName}`,
      },
      { key: 'email', header: 'Email' },
      { key: 'phoneNumber', header: 'Phone' },
      {
        key: 'warehouse',
        header: 'Warehouse',
        render: (e) => warehouseName.get(e.warehouseId) ?? '—',
        searchValue: (e) => warehouseName.get(e.warehouseId),
      },
    ],
    [warehouseName],
  )

  const fields = useMemo<FieldConfig<EmployeeFormValues>[]>(
    () => [
      {
        name: 'warehouseId',
        label: 'Warehouse',
        type: 'select',
        placeholder: 'Select a warehouse',
        options: warehouses.map((w) => ({ value: w.id, label: w.name })),
      },
      { name: 'firstName', label: 'First name' },
      { name: 'lastName', label: 'Last name' },
      { name: 'email', label: 'Email' },
      { name: 'phoneNumber', label: 'Phone number' },
    ],
    [warehouses],
  )

  return (
    <CrudPage<Employee, EmployeeFormValues, EmployeeInput>
      title="Warehouse Employees"
      subtitle="The staff of each warehouse."
      singular="employee"
      resource="/api/warehouse-employees"
      schema={employeeSchema}
      columns={columns}
      fields={fields}
      inputSchema={employeeInputSchema}
      emptyValues={emptyValues}
      toFormValues={(e) => ({
        warehouseId: e.warehouseId,
        firstName: e.firstName,
        lastName: e.lastName,
        email: e.email,
        phoneNumber: e.phoneNumber,
      })}
      itemName={(e) => `${e.firstName} ${e.lastName}`}
      permissionResource="employees"
      searchPlaceholder="Search employees…"
      notice={
        !warehousesLoading && !warehousesError && warehouses.length === 0
          ? 'There are no warehouses yet. An employee must work in a warehouse, so create one first.'
          : undefined
      }
    />
  )
}
