import type { ReactNode } from 'react'

/*
 * Placeholders shown while data loads. Each one is shaped like the content it stands in for, so the page does
 * not jump when the real thing arrives. The grey blocks are decoration; screen readers hear the status label.
 */

/** One pulsing grey block. Size it with classes, e.g. "h-4 w-32". */
export function Skeleton({ className = '' }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`block animate-pulse rounded-md bg-slate-200 motion-reduce:animate-none ${className}`}
    />
  )
}

/** Wraps a group of blocks and announces what is loading. */
function Loading({ label = 'Loading…', className = '', children }: { label?: string; className?: string; children: ReactNode }) {
  return (
    <div role="status" aria-busy="true" className={className}>
      <span className="sr-only">{label}</span>
      {children}
    </div>
  )
}

/** Different widths so rows look like text rather than a grid of bars. */
const WIDTHS = ['w-3/4', 'w-1/2', 'w-2/3', 'w-5/6', 'w-2/5']
const widthAt = (row: number, column: number) => WIDTHS[(row * 2 + column) % WIDTHS.length]

/** Rows for inside a <tbody>, matching a table with this many columns. */
export function TableRowsSkeleton({ columns, rows = 5 }: { columns: number; rows?: number }) {
  return (
    <>
      {Array.from({ length: rows }, (_, row) => (
        <tr key={row} aria-busy="true" className="odd:bg-white even:bg-slate-50">
          {Array.from({ length: columns }, (_, column) => (
            <td key={column} className="p-5">
              {row === 0 && column === 0 && (
                <span role="status" className="sr-only">
                  Loading…
                </span>
              )}
              <Skeleton className={`h-4 ${widthAt(row, column)}`} />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}

/** A whole table: header strip and rows. */
export function TableSkeleton({ columns = 5, rows = 5, label }: { columns?: number; rows?: number; label?: string }) {
  const row = (key: number, header: boolean) => (
    <div
      key={key}
      className={`grid gap-6 px-5 ${header ? 'bg-slate-50 py-3' : 'border-t border-slate-100 py-4'}`}
      style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}
    >
      {Array.from({ length: columns }, (_, column) => (
        <Skeleton key={column} className={`${header ? 'h-3' : 'h-4'} ${widthAt(key, column)}`} />
      ))}
    </div>
  )
  return (
    <Loading label={label} className="overflow-hidden rounded-lg border border-slate-200">
      {row(0, true)}
      {Array.from({ length: rows }, (_, i) => row(i + 1, false))}
    </Loading>
  )
}

/** A bordered list of rows, e.g. inside a dialog. */
export function ListSkeleton({ rows = 3, label }: { rows?: number; label?: string }) {
  return (
    <Loading label={label} className="divide-y divide-slate-100 rounded-lg border border-slate-200">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex items-center justify-between gap-4 px-3 py-3">
          <Skeleton className={`h-4 ${widthAt(i, 0)}`} />
          <Skeleton className="h-4 w-14 shrink-0" />
        </div>
      ))}
    </Loading>
  )
}

function Heading() {
  return (
    <div className="space-y-3">
      <Skeleton className="h-7 w-56" />
      <Skeleton className="h-4 w-80 max-w-full" />
    </div>
  )
}

/** A list page (heading, toolbar, table): used while the app finds out what the user may open. */
export function PageSkeleton() {
  return (
    <Loading className="space-y-6 rounded-lg border border-slate-200 bg-white p-8">
      <Heading />
      <div className="flex justify-between gap-4">
        <Skeleton className="h-9 w-40" />
        <Skeleton className="h-9 w-64 max-w-full" />
      </div>
      <TableSkeleton />
    </Loading>
  )
}

/** The dashboard's stat cards and charts. */
export function DashboardSkeleton() {
  return (
    <Loading label="Loading the dashboard…" className="space-y-4">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 8 }, (_, i) => (
          <div key={i} className="flex items-start gap-3 rounded-lg border border-slate-200 bg-white p-4">
            <Skeleton className="h-10 w-10 shrink-0 rounded-lg" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-3 w-3/4" />
              <Skeleton className="h-6 w-1/2" />
              <Skeleton className="h-3 w-2/3" />
            </div>
          </div>
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        {Array.from({ length: 4 }, (_, card) => (
          <div key={card} className="space-y-4 rounded-lg border border-slate-200 bg-white p-4">
            <Skeleton className="h-4 w-40" />
            {Array.from({ length: 5 }, (_, bar) => (
              <div key={bar} className="flex items-center gap-3">
                <Skeleton className="h-3 w-24 shrink-0" />
                <Skeleton className={`h-2.5 rounded-full ${widthAt(card, bar)}`} />
              </div>
            ))}
          </div>
        ))}
      </div>
    </Loading>
  )
}

/** A record's detail page, below its "Back" link. */
export function DetailSkeleton() {
  return (
    <Loading className="mt-4 space-y-6">
      <Heading />
      <div className="grid grid-cols-2 gap-4 border-y border-slate-200 py-5 sm:grid-cols-4">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="space-y-2">
            <Skeleton className="h-3 w-20" />
            <Skeleton className="h-4 w-32 max-w-full" />
          </div>
        ))}
      </div>
      <Skeleton className="h-5 w-24" />
      <TableSkeleton columns={5} rows={3} />
    </Loading>
  )
}

/** A row of small cards, e.g. the reports. */
export function CardsSkeleton({ cards = 4 }: { cards?: number }) {
  return (
    <Loading className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {Array.from({ length: cards }, (_, i) => (
        <div key={i} className="space-y-4 rounded-lg border border-slate-200 p-4">
          <Skeleton className="h-4 w-2/3" />
          <div className="flex gap-2">
            <Skeleton className="h-8 w-20" />
            <Skeleton className="h-8 w-24" />
          </div>
        </div>
      ))}
    </Loading>
  )
}

/** Grouped search results. */
export function SearchSkeleton() {
  return (
    <Loading label="Searching…" className="space-y-6">
      {[3, 2].map((rows, group) => (
        <div key={group} className="space-y-2">
          <Skeleton className="h-3 w-28" />
          <div className="divide-y divide-slate-100 rounded-lg border border-slate-200">
            {Array.from({ length: rows }, (_, i) => (
              <div key={i} className="px-4 py-3.5">
                <Skeleton className={`h-4 ${widthAt(group, i)}`} />
              </div>
            ))}
          </div>
        </div>
      ))}
    </Loading>
  )
}

/** The role tabs and the permissions table under them. */
export function RolesSkeleton() {
  return (
    <Loading className="space-y-6">
      <div className="flex gap-6 border-b border-slate-200 pb-3">
        {Array.from({ length: 4 }, (_, i) => (
          <Skeleton key={i} className="h-4 w-20" />
        ))}
      </div>
      <TableSkeleton columns={6} rows={8} />
    </Loading>
  )
}

/** The sidebar's section headings and links. */
export function SidebarSkeleton({ iconsOnly = false }: { iconsOnly?: boolean }) {
  return (
    <Loading label="Loading the menu…" className="space-y-5">
      {[2, 4, 3].map((links, section) => (
        <div key={section} className="space-y-1">
          <Skeleton className={`mx-3 mb-2 h-3 w-20 ${iconsOnly ? 'md:hidden' : ''}`} />
          {Array.from({ length: links }, (_, i) => (
            <div key={i} className={`flex items-center gap-3 px-3 py-2 ${iconsOnly ? 'md:justify-center md:px-0' : ''}`}>
              <Skeleton className="h-5 w-5 shrink-0" />
              <Skeleton className={`h-3.5 ${widthAt(section, i)} ${iconsOnly ? 'md:hidden' : ''}`} />
            </div>
          ))}
        </div>
      ))}
    </Loading>
  )
}

/** Notifications in the bell's dropdown. */
export function NotificationsSkeleton() {
  return (
    <Loading label="Loading notifications…" className="divide-y divide-slate-100">
      {Array.from({ length: 3 }, (_, i) => (
        <div key={i} className="space-y-2 px-4 py-3">
          <Skeleton className={`h-4 ${widthAt(i, 0)}`} />
          <Skeleton className={`h-3 ${widthAt(i, 1)}`} />
        </div>
      ))}
    </Loading>
  )
}
