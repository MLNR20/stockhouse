import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/products. categoryId is null for uncategorised products. */
export const productSchema = z.object({
  id: z.string(),
  name: z.string(),
  basePrice: z.number(),
  categoryId: z.string().nullable(),
  supplierIds: z.array(z.string()).default([]),
  ...timestampFields,
})

export type Product = z.infer<typeof productSchema>

/** Form validation: inputs are strings; the price becomes a number and "no category" becomes null. */
export const productInputSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(200, 'Name must be 200 characters or fewer'),
  basePrice: z
    .string()
    .trim()
    .min(1, 'Price is required')
    .regex(/^\d+(\.\d{1,2})?$/, 'Enter an amount like 12 or 12.50')
    .transform(Number),
  categoryId: z.string().transform((value) => (value === '' ? null : value)),
})

export type ProductFormValues = z.input<typeof productInputSchema>
export type ProductInput = z.output<typeof productInputSchema>

/** GET /api/categories (just what the dropdown needs). */
export const categorySchema = z.object({ id: z.string(), name: z.string() })
export type Category = z.infer<typeof categorySchema>
