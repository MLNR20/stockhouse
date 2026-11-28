import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

export const SHIPMENT_STATUSES = ['SCHEDULED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'] as const

/** GET /api/shipments. employeeId is null until someone is assigned. */
export const shipmentSchema = z.object({
  id: z.string(),
  orderId: z.string(),
  truckId: z.string(),
  employeeId: z.string().nullable(),
  scheduledDate: z.string(),
  status: z.string(),
  ...timestampFields,
})

export type Shipment = z.infer<typeof shipmentSchema>

/** Form validation: "unassigned" becomes null; the date comes from <input type="date"> as YYYY-MM-DD. */
export const shipmentInputSchema = z.object({
  orderId: z.string().min(1, 'Order is required'),
  truckId: z.string().min(1, 'Truck is required'),
  employeeId: z.string().transform((value) => (value === '' ? null : value)),
  scheduledDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Choose a date'),
  status: z.string().min(1, 'Status is required'),
})

export type ShipmentFormValues = z.input<typeof shipmentInputSchema>
export type ShipmentInput = z.output<typeof shipmentInputSchema>

/** What the order dropdown needs from GET /api/orders. */
export const orderLookupSchema = z.object({
  id: z.string(),
  customerName: z.string(),
  customerActive: z.boolean(),
  totalPrice: z.number(),
  status: z.string(),
})
export type OrderLookup = z.infer<typeof orderLookupSchema>

/** What the order dropdown needs from GET /api/customers (to show a name instead of an id). */
export const customerLookupSchema = z.object({
  id: z.string(),
  firstName: z.string(),
  lastName: z.string(),
})
export type CustomerLookup = z.infer<typeof customerLookupSchema>
