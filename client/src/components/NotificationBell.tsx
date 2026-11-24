import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useNotifications } from '../notifications/useNotifications.ts'
import type { AppNotification, NotificationType } from '../schemas/notification.ts'

/** Where clicking a notification should take the user, by the kind of record it is about. */
const ENTITY_ROUTES: Record<string, string> = {
  Order: '/orders',
  Inventory: '/inventory',
  Reorder: '/reorders',
}

const TYPE_DOT: Record<NotificationType, string> = {
  ORDER_CREATED: 'bg-blue-500',
  ORDER_STATUS_CHANGED: 'bg-blue-500',
  LOW_STOCK: 'bg-amber-500',
  OUT_OF_STOCK: 'bg-red-500',
  REORDER_RECEIVED: 'bg-green-500',
  GENERAL: 'bg-slate-400',
}

const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })

function timeAgo(iso: string): string {
  const seconds = Math.round((new Date(iso).getTime() - Date.now()) / 1000)
  const abs = Math.abs(seconds)
  if (abs < 60) return 'just now'
  if (abs < 3600) return rtf.format(Math.round(seconds / 60), 'minute')
  if (abs < 86400) return rtf.format(Math.round(seconds / 3600), 'hour')
  return rtf.format(Math.round(seconds / 86400), 'day')
}

function BellIcon() {
  return (
    <svg
      className="h-6 w-6"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M6 8a6 6 0 1112 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 003.4 0" />
    </svg>
  )
}

export default function NotificationBell() {
  const { items, unread, loading, connected, reload, markRead, markAllRead } = useNotifications()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)

  // Close on outside click or Escape.
  useEffect(() => {
    if (!open) return
    function onPointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
        buttonRef.current?.focus()
      }
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  function toggle() {
    if (!open) void reload() // fresh list each time it opens
    setOpen((o) => !o)
  }

  function handleSelect(notification: AppNotification) {
    void markRead(notification.id)
    const route = notification.entityName ? ENTITY_ROUTES[notification.entityName] : undefined
    if (route) {
      setOpen(false)
      // Orders and reorders have their own page, so go straight to the one the notification is about.
      const hasPage = notification.entityName === 'Order' || notification.entityName === 'Reorder'
      navigate(hasPage && notification.entityId ? `${route}/${notification.entityId}` : route)
    }
  }

  const badge = unread > 99 ? '99+' : String(unread)

  return (
    <div ref={rootRef} className="relative">
      <button
        ref={buttonRef}
        type="button"
        onClick={toggle}
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-haspopup="dialog"
        aria-expanded={open}
        className="relative rounded-md p-2 text-slate-600 hover:bg-slate-100"
      >
        <BellIcon />
        {unread > 0 && (
          <span className="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold leading-none text-white">
            {badge}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Notifications"
          className="absolute right-0 top-12 z-50 w-[calc(100vw-2rem)] max-w-sm overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg"
        >
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-semibold text-slate-900">Notifications</h2>
              <span
                title={connected ? 'Live updates on' : 'Reconnecting…'}
                className={`h-2 w-2 rounded-full ${connected ? 'bg-green-500' : 'bg-slate-300'}`}
              />
            </div>
            <button
              type="button"
              onClick={() => void markAllRead()}
              disabled={unread === 0}
              className="text-xs font-medium text-blue-600 hover:text-blue-800 disabled:cursor-default disabled:text-slate-400"
            >
              Mark all as read
            </button>
          </div>

          <ul className="max-h-96 divide-y divide-slate-100 overflow-y-auto">
            {items.length === 0 && (
              <li className="px-4 py-8 text-center text-sm text-slate-500">
                {loading ? 'Loading…' : 'You’re all caught up.'}
              </li>
            )}
            {items.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  onClick={() => handleSelect(n)}
                  className={`flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-slate-50 ${
                    n.read ? '' : 'bg-blue-50/50'
                  }`}
                >
                  <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${TYPE_DOT[n.type]}`} aria-hidden="true" />
                  <span className="min-w-0 flex-1">
                    <span className={`block text-sm text-slate-900 ${n.read ? '' : 'font-semibold'}`}>
                      {n.title}
                      {!n.read && <span className="sr-only"> (unread)</span>}
                    </span>
                    <span className="block text-sm text-slate-600">{n.message}</span>
                    <span className="mt-0.5 block text-xs text-slate-400">{timeAgo(n.createdAt)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
