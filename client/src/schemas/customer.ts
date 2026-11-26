import { z } from 'zod'
import { personName } from './personName.ts'
import { timestampFields } from './timestamps.ts'

/** GET /api/customers. */
export const customerSchema = z.object({
  id: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string(),
  phoneNumber: z.string(),
  address: z.string(),
  ...timestampFields,
})

export type Customer = z.infer<typeof customerSchema>

/** Form validation: the rules mirror the backend. */
export const customerInputSchema = z.object({
  firstName: personName('First name'),
  lastName: personName('Last name'),
  email: z
    .string()
    .trim()
    .max(256, 'Email must be 256 characters or fewer')
    .pipe(z.email('Enter a valid email address')),
  phoneNumber: z
    .string()
    .trim()
    .min(1, 'Phone number is required')
    .max(30, 'Phone number must be 30 characters or fewer'),
  address: z.string().trim().min(1, 'Address is required').max(500, 'Address must be 500 characters or fewer'),
})

export type CustomerFormValues = z.input<typeof customerInputSchema>
export type CustomerInput = z.output<typeof customerInputSchema>
