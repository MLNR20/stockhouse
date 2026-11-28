import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

export const INVENTORY_STATUSES = ['IN_STOCK', 'LOW_STOCK', 'OUT_OF_STOCK', 'DISCONTINUED'] as const

/** A whole number from 0 to 1,000,000, typed into a form as text. */
const wholeNumber = (label: string) =>
  z
    .string()
    .trim()
    .min(1, `${label} is required`)
    .regex(/^\d+$/, `${label} must be a whole number`)
    .transform(Number)
    .pipe(z.number().int().min(0, `${label} cannot be negative`).max(1_000_000, `${label} cannot exceed 1,000,000`))

/** GET /api/inventory: stock of one product. */
export const inventorySchema = z.object({
  id: z.string(),
  productId: z.string(),
  status: z.string(),
  /** Quantity on hand, in the base unit. */
  baseQuantity: z.number(),
  /** At or below this much counts as running low. */
  minimumThreshold: z.number(),
  /** True when baseQuantity has fallen below minimumThreshold. */
  belowMinimum: z.boolean(),
  /** The unit it is sold in (a "how sold" unit), or null when not set. */
  howSoldId: z.string().nullable(),
  howSoldName: z.string().nullable(),
  /** False when that unit was deleted; it is still named. */
  howSoldActive: z.boolean(),
  ...timestampFields,
})

export type Inventory = z.infer<typeof inventorySchema>

export const inventoryInputSchema = z.object({
  productId: z.string().min(1, 'Product is required'),
  status: z.string().min(1, 'Status is required'),
  baseQuantity: wholeNumber('Base quantity'),
  minimumThreshold: wholeNumber('Minimum threshold'),
  howSoldId: z.string().transform((value) => (value === '' ? null : value)),
})

export type InventoryFormValues = z.input<typeof inventoryInputSchema>
export type InventoryInput = z.output<typeof inventoryInputSchema>
