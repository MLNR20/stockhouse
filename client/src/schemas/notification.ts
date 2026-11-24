import { z } from 'zod'

export const NOTIFICATION_TYPES = [
  'ORDER_CREATED',
  'ORDER_STATUS_CHANGED',
  'LOW_STOCK',
  'OUT_OF_STOCK',
  'REORDER_RECEIVED',
  'GENERAL',
] as const

/** Same shape for GET /api/notifications* and for the live push on /user/queue/notifications. */
export const notificationSchema = z.object({
  id: z.string(),
  // Unknown types from a newer backend still render, as GENERAL.
  type: z.enum(NOTIFICATION_TYPES).catch('GENERAL'),
  title: z.string(),
  message: z.string(),
  entityName: z.string().nullable(),
  entityId: z.string().nullable(),
  read: z.boolean(),
  readAt: z.string().nullable(),
  createdAt: z.string(),
})

export type AppNotification = z.infer<typeof notificationSchema>
export type NotificationType = AppNotification['type']

export const notificationPageSchema = z.object({
  items: z.array(notificationSchema),
})

export const unreadCountSchema = z.object({ count: z.number() })
