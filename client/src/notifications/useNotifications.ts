import { useCallback, useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { API_URL, api, ensureFreshAccessToken } from '../api.ts'
import { useAuth } from '../auth/useAuth.ts'
import {
  notificationPageSchema,
  notificationSchema,
  unreadCountSchema,
  type AppNotification,
} from '../schemas/notification.ts'

/** How many notifications the bell keeps in memory. */
const MAX_ITEMS = 50

/** http://host -> ws://host, https://host -> wss://host */
const WS_URL = `${API_URL.replace(/^http/, 'ws')}/ws`

/**
 * The signed-in user's notifications: loaded over REST, then kept live over STOMP/WebSocket
 * (subscribed to /user/queue/notifications). Does nothing while signed out.
 */
export function useNotifications() {
  const { isAuthenticated, user } = useAuth()
  const userId = user?.id ?? null

  const [items, setItems] = useState<AppNotification[]>([])
  const [unread, setUnread] = useState(0)
  const [loading, setLoading] = useState(false)
  const [connected, setConnected] = useState(false)

  const reload = useCallback(async () => {
    setLoading(true)
    try {
      const [page, count] = await Promise.all([
        api.get('/api/notifications/page', { params: { size: MAX_ITEMS } }),
        api.get('/api/notifications/unread-count'),
      ])
      setItems(notificationPageSchema.parse(page.data).items)
      setUnread(unreadCountSchema.parse(count.data).count)
    } catch {
      // Keep what we have; the next reload (reconnect or opening the bell) tries again.
    } finally {
      setLoading(false)
    }
  }, [])

  // Live connection. Re-created when the signed-in user changes so one user never sees another's feed.
  useEffect(() => {
    if (!isAuthenticated) {
      setItems([])
      setUnread(0)
      setConnected(false)
      return
    }

    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
      // The JWT goes in the STOMP CONNECT frame (browsers cannot set headers on a WebSocket
      // handshake). Fetch a fresh one on every (re)connect so an expired token never loops.
      beforeConnect: async () => {
        const token = await ensureFreshAccessToken()
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}
      },
      onConnect: () => {
        setConnected(true)
        void reload() // catch anything missed while disconnected
        client.subscribe('/user/queue/notifications', (message) => {
          const parsed = notificationSchema.safeParse(JSON.parse(message.body))
          if (!parsed.success) return
          const incoming = parsed.data
          setItems((prev) => [incoming, ...prev.filter((n) => n.id !== incoming.id)].slice(0, MAX_ITEMS))
          if (!incoming.read) setUnread((count) => count + 1)
        })
      },
      onWebSocketClose: () => setConnected(false),
    })
    client.activate()
    return () => {
      setConnected(false)
      void client.deactivate()
    }
  }, [isAuthenticated, userId, reload])

  const markRead = useCallback(
    async (id: string) => {
      const target = items.find((n) => n.id === id)
      if (!target || target.read) return
      // Optimistic: update now, roll back by reloading if the server says no.
      setItems((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)))
      setUnread((count) => Math.max(0, count - 1))
      try {
        await api.put(`/api/notifications/${id}/read`)
      } catch {
        void reload()
      }
    },
    [items, reload],
  )

  const markAllRead = useCallback(async () => {
    setItems((prev) => prev.map((n) => ({ ...n, read: true })))
    setUnread(0)
    try {
      await api.put('/api/notifications/read-all')
    } catch {
      void reload()
    }
  }, [reload])

  return { items, unread, loading, connected, reload, markRead, markAllRead }
}
