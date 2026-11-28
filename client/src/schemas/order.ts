import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

export const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'] as const

/** GET /api/orders. */
export const orderSchema = z.object({
  id: z.string(),
  customerId: z.string(),
  customerName: z.string(),
  /** False when the customer was deleted; they are still named on their orders. */
  customerActive: z.boolean(),
  totalPrice: z.number(),
  targetDeliveryDate: z.string(),
  status: z.string(),
  ...timestampFields,
})

export type Order = z.infer<typeof orderSchema>

/** Form validation: the price becomes a number (up to 2 decimals); the date is YYYY-MM-DD from <input type="date">. */
export const orderInputSchema = z.object({
  customerId: z.string().min(1, 'Customer is required'),
  totalPrice: z
    .string()
    .trim()
    .min(1, 'Total price is required')
    .regex(/^\d+(\.\d{1,2})?$/, 'Enter an amount like 120 or 120.50')
    .transform(Number),
  targetDeliveryDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Choose a delivery date'),
  status: z.string().min(1, 'Status is required'),
})

export type OrderFormValues = z.input<typeof orderInputSchema>
export type OrderInput = z.output<typeof orderInputSchema>
