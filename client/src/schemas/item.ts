import { z } from 'zod'

// Adjust to match your backend's item shape.
export const itemSchema = z.object({
  id: z.union([z.string(), z.number()]),
  name: z.string(),
  sku: z.string(),
  quantity: z.number(),
})

export type Item = z.infer<typeof itemSchema>

/** Form validation. Inputs are strings (as typed); quantity is converted to a number on success. */
export const itemInputSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer'),
  sku: z.string().trim().min(1, 'SKU is required').max(50, 'SKU must be 50 characters or fewer'),
  quantity: z
    .string()
    .trim()
    .min(1, 'Quantity is required')
    .transform(Number)
    .pipe(
      z
        .number({ error: 'Quantity must be a number' })
        .int('Quantity must be a whole number')
        .min(0, 'Quantity cannot be negative'),
    ),
})

/** What the form holds while editing (all strings). */
export type ItemFormValues = z.input<typeof itemInputSchema>
/** What gets sent to the API after validation. */
export type ItemInput = z.output<typeof itemInputSchema>
