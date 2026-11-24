import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/categories. */
export const categorySchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string().nullable(),
  ...timestampFields,
})

export type Category = z.infer<typeof categorySchema>

/** Form validation: the limits mirror the backend (name 100, description 500 characters). */
export const categoryInputSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer'),
  description: z
    .string()
    .trim()
    .max(500, 'Description must be 500 characters or fewer')
    .transform((value) => (value === '' ? null : value)),
})

export type CategoryFormValues = z.input<typeof categoryInputSchema>
export type CategoryInput = z.output<typeof categoryInputSchema>
