import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/suppliers. productIds are the active products this supplier provides. */
export const supplierSchema = z.object({
  id: z.string(),
  supplierName: z.string(),
  phoneNumber: z.string(),
  email: z.string(),
  productIds: z.array(z.string()).default([]),
  ...timestampFields,
})

export type Supplier = z.infer<typeof supplierSchema>

/** Form validation: limits mirror the backend (200 / 30 / 256 characters). */
export const supplierInputSchema = z.object({
  supplierName: z.string().trim().min(1, 'Name is required').max(200, 'Name must be 200 characters or fewer'),
  phoneNumber: z
    .string()
    .trim()
    .min(1, 'Phone number is required')
    .max(30, 'Phone number must be 30 characters or fewer'),
  email: z
    .string()
    .trim()
    .max(256, 'Email must be 256 characters or fewer')
    .pipe(z.email('Enter a valid email address')),
})

export type SupplierFormValues = z.input<typeof supplierInputSchema>
export type SupplierInput = z.output<typeof supplierInputSchema>
