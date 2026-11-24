import { z } from 'zod'

/** GET /api/audit-logs: one thing a user did, newest first. */
export const historyEntrySchema = z.object({
  id: z.string(),
  entityName: z.string(),
  entityId: z.string(),
  action: z.string(),
  /** The user's name, "System" for automatic actions, or the raw id if that user was removed. */
  performedByName: z.string(),
  createdAt: z.string(),
})

export type HistoryEntry = z.infer<typeof historyEntrySchema>
