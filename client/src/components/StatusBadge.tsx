import { titleCase } from '../utils/format.ts'

// Full class names written out so Tailwind can see them.
const GREEN = 'bg-green-100 text-green-800'
const BLUE = 'bg-blue-100 text-blue-800'
const AMBER = 'bg-amber-100 text-amber-800'
const RED = 'bg-red-100 text-red-800'
const GRAY = 'bg-slate-100 text-slate-700'
const INDIGO = 'bg-indigo-100 text-indigo-800'

const TONES: Record<string, string> = {
  // trucks
  AVAILABLE: GREEN,
  IN_TRANSIT: BLUE,
  MAINTENANCE: AMBER,
  // shipments (IN_TRANSIT above)
  SCHEDULED: GRAY,
  DELIVERED: GREEN,
  CANCELLED: RED,
  // orders (DELIVERED and CANCELLED above)
  PENDING: AMBER,
  CONFIRMED: BLUE,
  SHIPPED: INDIGO,
  // supplier reorders (CANCELLED above)
  DRAFT: GRAY,
  ORDERED: BLUE,
  RECEIVED: GREEN,
  // inventory
  IN_STOCK: GREEN,
  LOW_STOCK: AMBER,
  OUT_OF_STOCK: RED,
  DISCONTINUED: GRAY,
  // history
  CREATE: GREEN,
  UPDATE: BLUE,
  DELETE: RED,
}

/** A small coloured pill for a status or action value such as IN_TRANSIT or DELETE. */
export default function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ${TONES[status] ?? GRAY}`}
    >
      {titleCase(status)}
    </span>
  )
}
