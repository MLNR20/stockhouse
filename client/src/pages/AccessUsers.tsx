import { useMemo } from 'react'
import CrudPage from '../components/CrudPage.tsx'
import type { Column } from '../components/DataTable.tsx'
import type { FieldConfig } from '../components/EntityFormModal.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import {
  roleSchema,
  userInputSchema,
  userSchema,
  type RoleRecord,
  type UserFormValues,
  type UserInput,
  type UserRecord,
} from '../schemas/access.ts'

const emptyValues: UserFormValues = { firstName: '', lastName: '', email: '', password: '', roleId: '' }

export default function AccessUsers() {
  // Only used to name and offer roles, so a failure here just leaves the dropdown empty.
  const { items: roles } = useCrud<RoleRecord>('/api/access/roles', { schema: roleSchema })

  const columns = useMemo<Column<UserRecord>[]>(
    () => [
      {
        key: 'name',
        header: 'Name',
        render: (u) => `${u.firstName} ${u.lastName}`,
        searchValue: (u) => `${u.firstName} ${u.lastName}`,
      },
      { key: 'email', header: 'Email' },
      {
        key: 'role',
        header: 'Role',
        render: (u) => <span className="capitalize">{u.roleName ?? 'No role'}</span>,
        searchValue: (u) => u.roleName,
      },
    ],
    [],
  )

  const fields = useMemo<FieldConfig<UserFormValues>[]>(
    () => [
      { name: 'firstName', label: 'First name' },
      { name: 'lastName', label: 'Last name' },
      { name: 'email', label: 'Email' },
      { name: 'password', label: 'Password (8+ characters; leave blank to keep the current one)', type: 'password' },
      {
        name: 'roleId',
        label: 'Role',
        type: 'select',
        placeholder: 'Select a role',
        options: roles.map((r) => ({ value: r.id, label: r.name })),
      },
    ],
    [roles],
  )

  return (
    <CrudPage<UserRecord, UserFormValues, UserInput>
      title="Users"
      subtitle="Who can sign in, and the role that decides what they can do."
      singular="user"
      resource="/api/users"
      schema={userSchema}
      columns={columns}
      fields={fields}
      inputSchema={userInputSchema}
      emptyValues={emptyValues}
      toFormValues={(u) => ({
        firstName: u.firstName,
        lastName: u.lastName,
        email: u.email,
        password: '',
        roleId: u.roleId ?? '',
      })}
      itemName={(u) => u.email}
      permissionResource="users"
      searchPlaceholder="Search users…"
    />
  )
}
