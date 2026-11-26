import { useEffect, useState } from 'react'
import { z } from 'zod'
import { api } from '../api.ts'
import type { Permission } from '../auth/permissions.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import { titleCase } from '../utils/format.ts'
import { apiErrorMessage } from '../utils/apiError.ts'
import { CardsSkeleton } from '../components/Skeleton.tsx'

/** Who may open each report: the same rule the server applies. */
const REPORT_ACCESS: Record<string, Permission[]> = {
  orders: ['orders:view', 'customers:view'],
  customers: ['orders:view', 'customers:view'],
  products: ['products:view'],
  suppliers: ['suppliers:view'],
}

type Preview = { type: string; url: string }

export default function Reports() {
  const { can } = usePermissions()
  const [types, setTypes] = useState<string[] | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [preview, setPreview] = useState<Preview | null>(null)

  useEffect(() => {
    api
      .get('/api/reports')
      .then((r) => setTypes(z.array(z.string()).parse(r.data).sort()))
      .catch((err: unknown) => {
        setTypes([])
        setError(apiErrorMessage(err))
      })
  }, [])

  // Free the previous PDF when another is previewed or the page is left.
  useEffect(() => {
    if (!preview) return
    return () => URL.revokeObjectURL(preview.url)
  }, [preview])

  /** The PDF is fetched with the session token, so it cannot simply be linked to. */
  async function fetchPdf(type: string): Promise<string> {
    const { data } = await api.get<Blob>(`/api/reports/${type}.pdf`, { responseType: 'blob' })
    return URL.createObjectURL(new Blob([data], { type: 'application/pdf' }))
  }

  async function run(type: string, action: 'preview' | 'download') {
    setBusy(`${type}:${action}`)
    setError(null)
    try {
      const url = await fetchPdf(type)
      if (action === 'preview') {
        setPreview({ type, url })
      } else {
        const link = document.createElement('a')
        link.href = url
        link.download = `${type}.pdf`
        link.click()
        setTimeout(() => URL.revokeObjectURL(url), 10_000)
      }
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(null)
    }
  }

  const allowed = (types ?? []).filter((type) => (REPORT_ACCESS[type] ?? []).some((p) => can(p)))

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      <h1 className="text-2xl font-bold">Reports</h1>
      <p className="mb-6 mt-1 text-[1.175rem] text-slate-500">Preview or download your data as PDF.</p>

      {error && (
        <p role="alert" className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {types === null ? (
        <CardsSkeleton />
      ) : allowed.length === 0 ? (
        <p className="text-sm text-slate-500">There are no reports your role can open.</p>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {allowed.map((type) => (
            <li key={type} className="rounded-lg border border-slate-200 p-4">
              <p className="font-semibold text-slate-900">{titleCase(type)} report</p>
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  disabled={busy !== null}
                  onClick={() => void run(type, 'preview')}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
                >
                  {busy === `${type}:preview` ? 'Loading…' : 'Preview'}
                </button>
                <button
                  type="button"
                  disabled={busy !== null}
                  onClick={() => void run(type, 'download')}
                  className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  {busy === `${type}:download` ? 'Preparing…' : 'Download'}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {preview && (
        <section className="mt-6">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold text-slate-900">{titleCase(preview.type)} preview</h2>
            <button type="button" onClick={() => setPreview(null)} className="text-sm font-medium text-blue-600 hover:underline">
              Close preview
            </button>
          </div>
          <iframe src={preview.url} title={`${preview.type} report`} className="h-[70vh] w-full rounded-lg border border-slate-200" />
        </section>
      )}
    </div>
  )
}
