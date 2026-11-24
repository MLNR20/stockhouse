import { useMemo, useState } from 'react'
import DataTable, { type Column } from '../components/DataTable.tsx'
import StatusBadge from '../components/StatusBadge.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { historyEntrySchema, type HistoryEntry } from '../schemas/history.ts'
import { formatDateTime, shortId, splitCamel, titleCase } from '../utils/format.ts'

/** The server returns the latest entries only; this is how many we ask for. */
const LIMIT = 500

const columns: Column<HistoryEntry>[] = [
  {
    key: 'createdAt',
    header: 'When',
    render: (entry) => formatDateTime(entry.createdAt),
    searchValue: (entry) => formatDateTime(entry.createdAt),
  },
  { key: 'performedByName', header: 'User' },
  {
    key: 'action',
    header: 'Action',
    render: (entry) => <StatusBadge status={entry.action} />,
    searchValue: (entry) => titleCase(entry.action),
  },
  {
    key: 'entityName',
    header: 'Record type',
    render: (entry) => splitCamel(entry.entityName),
    searchValue: (entry) => splitCamel(entry.entityName),
  },
  {
    key: 'entityId',
    header: 'Record',
    render: (entry) => <code className="text-xs text-slate-500">{shortId(entry.entityId)}</code>,
    searchValue: (entry) => entry.entityId,
  },
]

/** Who did what, and when: every create, update and delete, newest first (read-only). */
export default function History() {
  const { items, loading, error, fetchAll } = useCrud<HistoryEntry>(`/api/audit-logs?limit=${LIMIT}`, {
    schema: historyEntrySchema,
  })
  const [recordType, setRecordType] = useState('')

  const recordTypes = useMemo(() => [...new Set(items.map((entry) => entry.entityName))].sort(), [items])
  const visible = useMemo(
    () => (recordType ? items.filter((entry) => entry.entityName === recordType) : items),
    [items, recordType],
  )

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">History</h1>
          <p className="mt-1 text-[1.175rem] text-slate-500">
            Who did what, and when: every create, update and delete.
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-slate-600">
            Show
            <select
              value={recordType}
              onChange={(e) => setRecordType(e.target.value)}
              className="rounded-md border border-slate-300 px-2 py-2 text-sm outline-none focus:border-blue-500"
            >
              <option value="">All records</option>
              {recordTypes.map((type) => (
                <option key={type} value={type}>
                  {splitCamel(type)}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            onClick={() => void fetchAll()}
            disabled={loading}
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
          >
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      <div className="space-y-4">
        {error && (
          <p className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
        )}

        <DataTable
          bare
          columns={columns}
          data={visible}
          rowKey={(entry) => entry.id}
          loading={loading}
          searchPlaceholder="Search history…"
          emptyMessage="Nothing has been recorded yet."
        />

        {items.length >= LIMIT && (
          <p className="text-xs text-slate-400">Showing the latest {LIMIT} entries.</p>
        )}
      </div>
    </div>
  )
}
