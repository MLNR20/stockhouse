import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/shelves. */
export const shelfSchema = z.object({
  id: z.string(),
  aisleId: z.string(),
  name: z.string(),
  ...timestampFields,
})

export type Shelf = z.infer<typeof shelfSchema>

/** Form validation: the limit mirrors the backend (100 characters). */
export const shelfInputSchema = z.object({
  aisleId: z.string().min(1, 'Aisle is required'),
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer'),
})

export type ShelfFormValues = z.input<typeof shelfInputSchema>
export type ShelfInput = z.output<typeof shelfInputSchema>
