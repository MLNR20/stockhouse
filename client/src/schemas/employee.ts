import { z } from 'zod'
import { personName } from './personName.ts'
import { timestampFields } from './timestamps.ts'

/** GET /api/warehouse-employees. Each employee works in one warehouse. */
export const employeeSchema = z.object({
  id: z.string(),
  warehouseId: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string(),
  phoneNumber: z.string(),
  ...timestampFields,
})

export type Employee = z.infer<typeof employeeSchema>

/** Form validation: mirrors the backend (names: letters only, 2-100 characters; email 256; phone 30). */
export const employeeInputSchema = z.object({
  warehouseId: z.string().min(1, 'Warehouse is required'),
  firstName: personName('First name'),
  lastName: personName('Last name'),
  email: z.string().trim().max(256, 'Email must be 256 characters or fewer').pipe(z.email('Enter a valid email address')),
  phoneNumber: z
    .string()
    .trim()
    .min(1, 'Phone number is required')
    .max(30, 'Phone number must be 30 characters or fewer'),
})

export type EmployeeFormValues = z.input<typeof employeeInputSchema>
export type EmployeeInput = z.output<typeof employeeInputSchema>
