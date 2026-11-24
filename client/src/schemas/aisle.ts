import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/aisles. */
export const aisleSchema = z.object({
  id: z.string(),
  warehouseId: z.string(),
  name: z.string(),
  ...timestampFields,
})

export type Aisle = z.infer<typeof aisleSchema>

/** Form validation: the limit mirrors the backend (100 characters). */
export const aisleInputSchema = z.object({
  warehouseId: z.string().min(1, 'Warehouse is required'),
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer'),
})

export type AisleFormValues = z.input<typeof aisleInputSchema>
export type AisleInput = z.output<typeof aisleInputSchema>

/** GET /api/warehouses (just what the dropdown needs). */
export const warehouseSchema = z.object({ id: z.string(), name: z.string() })
export type Warehouse = z.infer<typeof warehouseSchema>
