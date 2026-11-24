import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

export const TRUCK_STATUSES = ['AVAILABLE', 'IN_TRANSIT', 'MAINTENANCE'] as const

/** GET /api/trucks. */
export const truckSchema = z.object({
  id: z.string(),
  plateNumber: z.string(),
  model: z.string(),
  capacityKg: z.number(),
  status: z.string(),
  ...timestampFields,
})

export type Truck = z.infer<typeof truckSchema>

/** Form validation: limits mirror the backend (20 / 100 characters, capacity above 0 with 2 decimals). */
export const truckInputSchema = z.object({
  plateNumber: z.string().trim().min(1, 'Plate number is required').max(20, 'Plate number must be 20 characters or fewer'),
  model: z.string().trim().min(1, 'Model is required').max(100, 'Model must be 100 characters or fewer'),
  capacityKg: z
    .string()
    .trim()
    .min(1, 'Capacity is required')
    .regex(/^\d{1,8}(\.\d{1,2})?$/, 'Enter a weight like 1500 or 1500.50')
    .transform(Number)
    .pipe(z.number().positive('Capacity must be greater than 0')),
  status: z.string().min(1, 'Status is required'),
})

export type TruckFormValues = z.input<typeof truckInputSchema>
export type TruckInput = z.output<typeof truckInputSchema>
