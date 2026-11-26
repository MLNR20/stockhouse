import { z } from 'zod'

/** GET /api/analytics/orders-vs-inventory: quantities are in base units. */
export const trendSchema = z.object({
  from: z.string(),
  to: z.string(),
  stockNow: z.number(),
  totalOrdered: z.number(),
  totalIncoming: z.number(),
  /** The first day stock is projected to run short, or null. */
  firstShortage: z.string().nullable(),
  days: z.array(
    z.object({
      date: z.string(),
      ordered: z.number(),
      orderedTotal: z.number(),
      incoming: z.number(),
      stock: z.number(),
    }),
  ),
})

export type Trend = z.infer<typeof trendSchema>
