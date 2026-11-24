import { z } from 'zod'
import { timestampFields } from './timestamps.ts'

/** GET /api/how-sold-conversions: 1 parent (larger) unit = unitConversion child (smaller) units. */
export const conversionSchema = z.object({
  id: z.string(),
  parentHowSoldId: z.string(),
  childHowSoldId: z.string(),
  unitConversion: z.number(),
  /** The product this conversion is for, or null when it applies to every product. */
  productId: z.string().nullable(),
  ...timestampFields,
})

export type Conversion = z.infer<typeof conversionSchema>

/** Form validation: two different units and a factor above 0 with up to 4 decimals (the backend's limits). */
export const conversionInputSchema = z
  .object({
    // "All products" (empty) becomes null.
    productId: z.string().transform((value) => (value === '' ? null : value)),
    parentHowSoldId: z.string().min(1, 'Choose the larger unit'),
    childHowSoldId: z.string().min(1, 'Choose the smaller unit'),
    unitConversion: z
      .string()
      .trim()
      .min(1, 'Conversion is required')
      .regex(/^\d{1,15}(\.\d{1,4})?$/, 'Enter a number like 12 or 0.5')
      .transform(Number)
      .pipe(z.number().positive('Conversion must be greater than 0')),
  })
  .refine((value) => value.parentHowSoldId !== value.childHowSoldId, {
    path: ['childHowSoldId'],
    message: 'Choose two different units',
  })

export type ConversionFormValues = z.input<typeof conversionInputSchema>
export type ConversionInput = z.output<typeof conversionInputSchema>
