import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/warehouses. */
export const warehouseRecordSchema = z.object({
  id: z.string(),
  name: z.string(),
  ...timestampFields,
})

export type WarehouseRecord = z.infer<typeof warehouseRecordSchema>

/** Form validation: the limit mirrors the backend (200 characters). */
export const warehouseInputSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(200, 'Name must be 200 characters or fewer'),
})

export type WarehouseFormValues = z.input<typeof warehouseInputSchema>
export type WarehouseInput = z.output<typeof warehouseInputSchema>
