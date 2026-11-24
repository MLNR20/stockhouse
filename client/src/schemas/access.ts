import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/access/catalog: everything that can be granted. */
export const catalogSchema = z.object({
  resources: z.array(z.string()),
  actions: z.array(z.string()),
  pages: z.array(z.object({ path: z.string(), label: z.string(), section: z.string() })),
})
export type Catalog = z.infer<typeof catalogSchema>

/** GET /api/access/roles. */
export const roleSchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string().nullable(),
  system: z.boolean(),
  permissions: z.array(z.string()),
  pages: z.array(z.string()),
  userCount: z.number(),
  ...timestampFields,
})
export type RoleRecord = z.infer<typeof roleSchema>

/** GET /api/users. */
export const userSchema = z.object({
  id: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string(),
  roleId: z.string().nullable(),
  roleName: z.string().nullable(),
  ...timestampFields,
})
export type UserRecord = z.infer<typeof userSchema>

/** Form validation: the limits mirror the backend. The password is optional here; the server asks for it on a new user. */
export const userInputSchema = z.object({
  firstName: z.string().trim().min(1, 'First name is required').max(100, 'First name must be 100 characters or fewer'),
  lastName: z.string().trim().min(1, 'Last name is required').max(100, 'Last name must be 100 characters or fewer'),
  email: z.string().trim().pipe(z.email('Enter a valid email address')),
  password: z
    .string()
    .refine((v) => v === '' || (v.length >= 8 && v.length <= 72), 'Password must be 8 to 72 characters')
    .transform((v) => (v === '' ? undefined : v)),
  roleId: z.string().min(1, 'Role is required'),
})
export type UserFormValues = z.input<typeof userInputSchema>
export type UserInput = z.output<typeof userInputSchema>
