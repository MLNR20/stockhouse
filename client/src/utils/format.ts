const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })
const dateOnly = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' })

/** An ISO timestamp (e.g. createdAt) as "Nov 24, 2026, 6:05 PM", or "—" when missing. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? '—' : dateTime.format(date)
}

/** A calendar date 'YYYY-MM-DD' as "Nov 24, 2026". Parsed at local midnight so the day never shifts. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(`${iso}T00:00:00`)
  return Number.isNaN(date.getTime()) ? '—' : dateOnly.format(date)
}

/** 'OUT_OF_STOCK' -> 'out of stock' */
export const humanize = (value: string) => value.replace(/_/g, ' ').toLowerCase()

/** 'OUT_OF_STOCK' -> 'Out Of Stock' */
export const titleCase = (value: string) => humanize(value).replace(/\b\w/g, (c) => c.toUpperCase())

/** 'WarehouseEmployee' -> 'Warehouse Employee' */
export const splitCamel = (value: string) => value.replace(/([a-z])([A-Z])/g, '$1 $2')

const pesos = new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' })

/** An amount in Philippine pesos: 1234.5 -> "₱1,234.50". Every price and total on screen goes through this. */
export const formatMoney = (amount: number) => pesos.format(amount)

/** The first 8 characters of a UUID, enough to tell records apart on screen. */
export const shortId = (id: string) => id.slice(0, 8)
