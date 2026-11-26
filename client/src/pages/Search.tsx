import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { api } from '../api.ts'
import { PAGE_LABELS, PAGE_ORDER } from '../auth/permissions.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import { useCrud } from '../hooks/useCrud.ts'
import { SEARCH_SOURCES, type SearchNames, type SearchRow, type SearchSource } from '../services/searchSources.ts'
import { SearchSkeleton } from '../components/Skeleton.tsx'

const SHOWN_PER_GROUP = 5

const resultSchema = z.object({
  items: z.array(z.looseObject({ id: z.string() })),
  totalItems: z.number(),
})

type Group = { source: SearchSource; items: SearchRow[]; total: number }
type Results = { term: string; groups: Group[]; failed: number }

const nameSchema = z.looseObject({ id: z.string() })
type Named = z.infer<typeof nameSchema>

const toMap = (items: Named[], label: (item: Named) => string) => new Map(items.map((i) => [i.id, label(i)]))

/** Search everything the user's pages hold: GET /search?q=... */
export default function Search() {
  const [params, setParams] = useSearchParams()
  const term = (params.get('q') ?? '').trim()
  const { pages, can } = usePermissions()

  const [input, setInput] = useState(term)
  const [results, setResults] = useState<Results | null>(null)

  // Used only to name what results point at.
  const products = useCrud<Named>('/api/products', { schema: nameSchema, autoFetch: can('products:view') })
  const suppliers = useCrud<Named>('/api/suppliers', { schema: nameSchema, autoFetch: can('suppliers:view') })
  const customers = useCrud<Named>('/api/customers', { schema: nameSchema, autoFetch: can('customers:view') })

  const names = useMemo<SearchNames>(
    () => ({
      product: toMap(products.items, (p) => String(p.name)),
      supplier: toMap(suppliers.items, (s) => String(s.supplierName)),
      customer: toMap(customers.items, (c) => `${String(c.firstName)} ${String(c.lastName)}`),
    }),
    [products.items, suppliers.items, customers.items],
  )

  // The navbar can change the term while this page is open.
  useEffect(() => setInput(term), [term])

  useEffect(() => {
    if (!term) return
    let cancelled = false
    const sources = SEARCH_SOURCES.filter((s) => pages.has(s.page))
    void Promise.all(
      sources.map(async (source): Promise<Group | null> => {
        try {
          const { data } = await api.get(`${source.resource}/page`, {
            params: { search: term, size: SHOWN_PER_GROUP },
          })
          const result = resultSchema.parse(data)
          return { source, items: result.items, total: result.totalItems }
        } catch {
          return null // this area could not be searched; the others still show
        }
      }),
    ).then((groups) => {
      if (cancelled) return
      const found = groups.filter((g): g is Group => g !== null)
      setResults({ term, groups: found.filter((g) => g.total > 0), failed: groups.length - found.length })
    })
    return () => {
      cancelled = true
    }
  }, [term, pages])

  function submit(event: FormEvent) {
    event.preventDefault()
    const next = input.trim()
    setParams(next ? { q: next } : {})
  }

  const matchingPages = term
    ? PAGE_ORDER.filter((p) => pages.has(p) && PAGE_LABELS[p].toLowerCase().includes(term.toLowerCase()))
    : []
  const current = results && results.term === term ? results : null
  const nothing = current && current.groups.length === 0 && matchingPages.length === 0

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      <h1 className="text-2xl font-bold">Search</h1>
      <p className="mt-1 text-[1.175rem] text-slate-500">
        {term ? `Results for “${term}”` : 'Find products, orders, people, places and pages.'}
      </p>

      <form onSubmit={submit} role="search" className="mt-5 flex gap-2">
        <input
          type="search"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          autoFocus
          aria-label="Search everything"
          placeholder="Type a name, email, status or order number…"
          className="w-full max-w-xl rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
        />
        <button type="submit" className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
          Search
        </button>
      </form>

      <div className="mt-6 space-y-6">
        {matchingPages.length > 0 && (
          <section>
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Pages</h2>
            <div className="flex flex-wrap gap-2">
              {matchingPages.map((p) => (
                <Link
                  key={p}
                  to={p}
                  className="rounded-full border border-slate-200 px-3 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-50"
                >
                  {PAGE_LABELS[p]}
                </Link>
              ))}
            </div>
          </section>
        )}

        {term && !current && <SearchSkeleton />}

        {current && current.failed > 0 && (
          <p role="alert" className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            Some areas could not be searched. Try again in a moment.
          </p>
        )}

        {nothing && <p className="text-sm text-slate-500">Nothing matches “{term}”.</p>}

        {current?.groups.map(({ source, items, total }) => (
          <section key={source.page}>
            <div className="mb-2 flex items-baseline justify-between gap-3">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-400">
                {source.label} <span className="normal-case">({total})</span>
              </h2>
              <Link
                to={`${source.page}?q=${encodeURIComponent(term)}`}
                className="text-sm font-medium text-blue-600 hover:underline"
              >
                {total > items.length ? `See all ${total}` : 'Open list'}
              </Link>
            </div>
            <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
              {items.map((row) => (
                <li key={row.id}>
                  <Link to={source.href(row, term, names)} className="block px-4 py-2.5 text-sm hover:bg-blue-50">
                    <span className="font-medium text-slate-900">{source.title(row, names)}</span>
                    {source.detail && <span className="ml-2 text-slate-500">{source.detail(row, names)}</span>}
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  )
}
