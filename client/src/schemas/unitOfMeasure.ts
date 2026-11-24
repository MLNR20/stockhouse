import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/**
 * A unit of measure is the backend's "how sold" unit (Each, Pack, Case, Pallet...):
 * GET /api/how-sold. Conversions between units live at /api/how-sold-conversions.
 */
export const unitOfMeasureSchema = z.object({
  id: z.string(),
  name: z.string(),
  ...timestampFields,
})

export type UnitOfMeasure = z.infer<typeof unitOfMeasureSchema>

/** Form validation: the limit mirrors the backend (100 characters). */
export const unitOfMeasureInputSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer'),
})

export type UnitOfMeasureFormValues = z.input<typeof unitOfMeasureInputSchema>
export type UnitOfMeasureInput = z.output<typeof unitOfMeasureInputSchema>
