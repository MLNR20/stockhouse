import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/orders/{id}/details: one line of an order. */
export const orderDetailSchema = z.object({
  id: z.string(),
  orderId: z.string(),
  productId: z.string(),
  productName: z.string(),
  /** The unit the quantity is counted in (a "how sold" unit); null means the base unit. */
  howSoldId: z.string().nullable(),
  howSoldName: z.string().nullable(),
  quantity: z.number(),
  salesPrice: z.number(),
  lineTotal: z.number(),
  /** The product's current base price, to compare with the sales price. */
  basePrice: z.number(),
  /** The base unit stock is counted in (e.g. "Each"), when the line has a unit. */
  baseUnitName: z.string().nullable(),
  /** This line's quantity converted to base units (2 Case of 12 Each = 24). */
  baseQuantityNeeded: z.number(),
  /** Base quantity on hand for the product (all its inventory records). */
  availableQuantity: z.number(),
  /** Whether availableQuantity covers baseQuantityNeeded. */
  inStock: z.boolean(),
  ...timestampFields,
})

export type OrderDetail = z.infer<typeof orderDetailSchema>

/** Form validation: quantity is a whole number 1 to 1,000,000; the price has up to 2 decimals. */
export const orderDetailInputSchema = z.object({
  productId: z.string().min(1, 'Product is required'),
  // Empty means "the product's usual unit" (the server picks it from the product's inventory).
  howSoldId: z.string().transform((value) => (value === '' ? null : value)),
  quantity: z
    .string()
    .trim()
    .min(1, 'Quantity is required')
    .regex(/^\d+$/, 'Quantity must be a whole number')
    .transform(Number)
    .pipe(z.number().int().min(1, 'Quantity must be at least 1').max(1_000_000, 'Quantity cannot exceed 1,000,000')),
  salesPrice: z
    .string()
    .trim()
    .min(1, 'Sales price is required')
    .regex(/^\d+(\.\d{1,2})?$/, 'Enter an amount like 12 or 12.50')
    .transform(Number),
})

export type OrderDetailFormValues = z.input<typeof orderDetailInputSchema>
export type OrderDetailInput = z.output<typeof orderDetailInputSchema>
