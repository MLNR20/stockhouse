import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/rows: a row on a shelf. */
export const shelfRowSchema = z.object({
  id: z.string(),
  shelfId: z.string(),
  name: z.string(),
  ...timestampFields,
})
export type ShelfRow = z.infer<typeof shelfRowSchema>

/** GET /api/containers: a container standing in a shelf row. */
export const containerSchema = z.object({
  id: z.string(),
  rowId: z.string(),
  name: z.string(),
  ...timestampFields,
})
export type StorageContainer = z.infer<typeof containerSchema>

/** GET /api/containers/{id}/products: what is inside a container. */
export const containerContentSchema = z.object({
  id: z.string(),
  productId: z.string(),
  quantity: z.number(),
})
export type ContainerContent = z.infer<typeof containerContentSchema>

const name = z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer')

export const shelfRowInputSchema = z.object({ shelfId: z.string().min(1, 'Shelf is required'), name })
export type ShelfRowFormValues = z.input<typeof shelfRowInputSchema>
export type ShelfRowInput = z.output<typeof shelfRowInputSchema>

export const containerInputSchema = z.object({ rowId: z.string().min(1, 'Shelf row is required'), name })
export type ContainerFormValues = z.input<typeof containerInputSchema>
export type ContainerInput = z.output<typeof containerInputSchema>
