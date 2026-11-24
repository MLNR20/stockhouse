import type { ReactNode } from 'react'

const TONES = {
  default: 'text-slate-900',
  good: 'text-green-700',
  warning: 'text-amber-600',
  danger: 'text-red-600',
} as const

const ICON_TONES = {
  default: 'bg-blue-50 text-blue-600',
  good: 'bg-green-50 text-green-600',
  warning: 'bg-amber-50 text-amber-600',
  danger: 'bg-red-50 text-red-600',
} as const

/** Wraps SVG path data in the 24x24 stroke icon style used across the app. */
export function StatIcon({ children }: { children: ReactNode }) {
  return (
    <svg
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

/** A single headline number. */
export function StatCard({
  label,
  value,
  hint,
  tone = 'default',
  icon,
}: {
  label: string
  value: ReactNode
  hint?: string
  tone?: keyof typeof TONES
  icon?: ReactNode
}) {
  return (
    <div className="flex items-start gap-3 rounded-lg border border-slate-200 bg-white p-4">
      {icon && (
        <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${ICON_TONES[tone]}`}>
          {icon}
        </span>
      )}
      <div className="min-w-0">
        <p className="text-sm text-slate-500">{label}</p>
        <p className={`mt-1 text-2xl font-bold ${TONES[tone]}`}>{value}</p>
        {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
      </div>
    </div>
  )
}

/** A titled box for one chart. */
export function ChartCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <h2 className="mb-4 text-sm font-semibold text-slate-900">{title}</h2>
      {children}
    </section>
  )
}

export type BarItem = {
  label: string
  value: number
  /** A full Tailwind background class, e.g. "bg-green-500" (written out so Tailwind can see it). */
  color: string
}

/** Horizontal bars with the label on the left and the number on the right. */
export function BarList({ items, emptyMessage = 'No data yet.' }: { items: BarItem[]; emptyMessage?: string }) {
  const max = Math.max(1, ...items.map((item) => item.value))
  if (items.length === 0) return <p className="text-sm text-slate-500">{emptyMessage}</p>

  return (
    <ul className="space-y-3">
      {items.map((item) => (
        <li key={item.label} className="grid grid-cols-[8rem_1fr_2.5rem] items-center gap-3 text-sm">
          <span className="truncate capitalize text-slate-600" title={item.label}>
            {item.label}
          </span>
          <span className="h-2.5 overflow-hidden rounded-full bg-slate-100">
            <span
              className={`block h-full rounded-full ${item.color}`}
              style={{ width: `${(item.value / max) * 100}%` }}
            />
          </span>
          <span className="text-right font-medium text-slate-900">{item.value}</span>
        </li>
      ))}
    </ul>
  )
}

export type Column = { label: string; title: string; value: number }

/** Vertical columns, one per label, for a short series such as "orders due per day". */
export function ColumnChart({ columns }: { columns: Column[] }) {
  const max = Math.max(1, ...columns.map((c) => c.value))
  return (
    <div>
      <div className="flex h-40 items-end gap-1.5">
        {columns.map((c) => (
          <div key={c.title} className="flex h-full flex-1 flex-col justify-end" title={`${c.title}: ${c.value}`}>
            {c.value > 0 && <span className="mb-1 text-center text-xs font-medium text-slate-700">{c.value}</span>}
            <div
              className={`w-full rounded-t ${c.value > 0 ? 'bg-blue-500' : 'bg-slate-200'}`}
              style={{ height: c.value > 0 ? `${(c.value / max) * 85}%` : '2px' }}
            />
          </div>
        ))}
      </div>
      <div className="mt-2 flex gap-1.5">
        {columns.map((c) => (
          <span key={c.title} className="flex-1 text-center text-xs text-slate-400">
            {c.label}
          </span>
        ))}
      </div>
    </div>
  )
}
