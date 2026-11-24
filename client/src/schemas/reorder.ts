import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

export const REORDER_STATUSES = ['DRAFT', 'ORDERED', 'RECEIVED', 'CANCELLED'] as const

/** GET /api/reorders: a request to a supplier for more stock of a product. */
export const reorderSchema = z.object({
  id: z.string(),
  supplierId: z.string(),
  productId: z.string(),
  quantity: z.number(),
  /** The unit the quantity is counted in (a "how sold" unit such as Case), or null when not set. */
  howSoldId: z.string().nullable(),
  howSoldName: z.string().nullable(),
  /** Null when no delivery date was given. */
  expectedDate: z.string().nullable(),
  status: z.string(),
  ...timestampFields,
})

export type Reorder = z.infer<typeof reorderSchema>

/** Form validation: quantity is a whole number from 1 to 1,000,000; an empty date means "not set". */
export const reorderInputSchema = z.object({
  supplierId: z.string().min(1, 'Supplier is required'),
  productId: z.string().min(1, 'Product is required'),
  quantity: z
    .string()
    .trim()
    .min(1, 'Quantity is required')
    .regex(/^\d+$/, 'Quantity must be a whole number')
    .transform(Number)
    .pipe(z.number().int().min(1, 'Quantity must be at least 1').max(1_000_000, 'Quantity cannot exceed 1,000,000')),
  // Empty means "no unit".
  howSoldId: z.string().transform((value) => (value === '' ? null : value)),
  expectedDate: z
    .string()
    .regex(/^(\d{4}-\d{2}-\d{2})?$/, 'Choose a valid date')
    .transform((value) => (value === '' ? null : value)),
  status: z.string().min(1, 'Status is required'),
})

export type ReorderFormValues = z.input<typeof reorderInputSchema>
export type ReorderInput = z.output<typeof reorderInputSchema>
