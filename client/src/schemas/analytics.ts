import { z } from 'zod'

const statusCount = z.object({ status: z.string(), count: z.number() })

/** GET /api/analytics/summary: everything the dashboard shows. */
export const analyticsSchema = z.object({
  totals: z.object({
    products: z.number(),
    suppliers: z.number(),
    customers: z.number(),
    categories: z.number(),
    warehouses: z.number(),
    containers: z.number(),
  }),
  orders: z.object({
    total: z.number(),
    /** Not delivered or cancelled. */
    open: z.number(),
    /** Open orders whose target delivery date has passed. */
    overdue: z.number(),
    /** Total price of every order that is not cancelled. */
    revenue: z.number(),
    openValue: z.number(),
    byStatus: z.array(statusCount.extend({ revenue: z.number() })),
  }),
  inventoryByStatus: z.array(statusCount),
  productsByCategory: z.array(z.object({ category: z.string(), count: z.number() })),
  /** Open orders due on each of the next 14 days (zeros included), as { date: 'YYYY-MM-DD', count }. */
  deliveriesNext14Days: z.array(z.object({ date: z.string(), count: z.number() })),
})

export type Analytics = z.infer<typeof analyticsSchema>
