import { useMemo, useState, type ReactNode } from 'react'
import { getPageList, getTotalPages, paginate } from '../services/pagination.ts'

export type Column<T> = {
  /** Unique column key; also used to read the value when `render` isn't given. */
  key: string
  header: string
  /** Custom cell renderer. */
  render?: (row: T) => ReactNode
  /** Value used for searching (defaults to row[key]). */
  searchValue?: (row: T) => string | number | null | undefined
}

type DataTableProps<T> = {
  columns: Column<T>[]
  data: T[]
  rowKey: (row: T) => string | number
  loading?: boolean
  searchPlaceholder?: string
  pageSizeOptions?: number[]
  /** Optional trailing actions column (edit / delete buttons, etc). */
  actions?: (row: T) => ReactNode
  emptyMessage?: string
  /** No outer border, for when the table sits inside a container that already has one. */
  bare?: boolean
}

const DEFAULT_PAGE_SIZES = [10, 25, 50, 100]

export default function DataTable<T>({
  columns,
  data,
  rowKey,
  loading = false,
  searchPlaceholder = 'Search…',
  pageSizeOptions = DEFAULT_PAGE_SIZES,
  actions,
  emptyMessage = 'No records found.',
  bare = false,
}: DataTableProps<T>) {
  const [search, setSearch] = useState('')
  const [pageSize, setPageSize] = useState(pageSizeOptions[0])
  const [page, setPage] = useState(1)

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) return data
    return data.filter((row) =>
      columns.some((col) => {
        const value = col.searchValue
          ? col.searchValue(row)
          : (row as Record<string, unknown>)[col.key]
        return value != null && String(value).toLowerCase().includes(term)
      }),
    )
  }, [data, columns, search])

  const totalPages = getTotalPages(filtered.length, pageSize)
  const { items: rows, page: currentPage } = paginate(filtered, page, pageSize)

  const pageButton =
    'min-w-9 rounded-md border px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-50'

  return (
    <div className={bare ? '' : 'rounded-lg border border-slate-200 bg-white'}>
      {/* Toolbar: rows per page + search */}
      <div className="flex flex-col gap-3 border-b border-slate-200 py-4 pl-3 pr-5 sm:flex-row sm:items-center sm:justify-between">
        <label className="flex items-center gap-2 text-sm text-slate-600">
          Show
          <select
            value={pageSize}
            onChange={(e) => {
              setPageSize(Number(e.target.value))
              setPage(1)
            }}
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm outline-none focus:border-blue-500"
          >
            {pageSizeOptions.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
          rows per page
        </label>

        <input
          type="search"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(1)
          }}
          placeholder={searchPlaceholder}
          className="w-full rounded-md border border-slate-300 px-3 py-1.5 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 sm:w-64"
        />
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              {columns.map((col) => (
                <th key={col.key} className="px-5 py-3 font-medium">
                  {col.header}
                </th>
              ))}
              {actions && <th className="px-5 py-3 text-right font-medium">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={columns.length + (actions ? 1 : 0)} className="px-4 py-10 text-center text-slate-500">
                  Loading…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length + (actions ? 1 : 0)} className="px-4 py-10 text-center text-slate-500">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                // Alternating row colours (no divider lines needed), blue tint on hover.
                <tr key={rowKey(row)} className="odd:bg-white even:bg-slate-50 hover:bg-blue-50">
                  {columns.map((col) => (
                    <td key={col.key} className="p-5">
                      {col.render
                        ? col.render(row)
                        : String((row as Record<string, unknown>)[col.key] ?? '')}
                    </td>
                  ))}
                  {actions && <td className="p-5 text-right">{actions(row)}</td>}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Footer: "10 out of 10" + pagination */}
      <div className="flex flex-col gap-3 border-t border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-slate-600">
          Showing {rows.length} out of {filtered.length} entries
          {filtered.length !== data.length && ` (filtered from ${data.length})`}
        </p>

        <nav className="flex items-center gap-1" aria-label="Pagination">
          <button
            type="button"
            onClick={() => setPage(currentPage - 1)}
            disabled={currentPage === 1}
            className={`${pageButton} border-slate-300 text-slate-700 hover:bg-slate-100`}
          >
            Previous
          </button>
          {getPageList(currentPage, totalPages).map((p, i) =>
            p === '…' ? (
              <span key={`gap-${i}`} className="px-2 text-slate-400">
                …
              </span>
            ) : (
              <button
                key={p}
                type="button"
                onClick={() => setPage(p)}
                aria-current={p === currentPage ? 'page' : undefined}
                className={`${pageButton} ${
                  p === currentPage
                    ? 'border-blue-600 bg-blue-600 text-white'
                    : 'border-slate-300 text-slate-700 hover:bg-slate-100'
                }`}
              >
                {p}
              </button>
            ),
          )}
          <button
            type="button"
            onClick={() => setPage(currentPage + 1)}
            disabled={currentPage === totalPages}
            className={`${pageButton} border-slate-300 text-slate-700 hover:bg-slate-100`}
          >
            Next
          </button>
        </nav>
      </div>
    </div>
  )
}
