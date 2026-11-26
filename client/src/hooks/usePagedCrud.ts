import { useCallback, useEffect, useRef, useState } from 'react'
import { z } from 'zod'
import { api } from '../api.ts'
import { apiErrorMessage } from '../utils/apiError.ts'

type Id = string | number

/** The backend's page shape (PageResponse). Its page numbers start at 0. */
function pageResponseSchema<T>(item: z.ZodType<T>) {
  return z.object({
    items: z.array(item),
    totalItems: z.number(),
    totalPages: z.number(),
  })
}

const SEARCH_DELAY_MS = 300

/**
 * CRUD for a list that the server pages and searches: GET {resource}/page?page&size&search, plus
 * POST / PUT / DELETE on {resource}. Only the page on screen is downloaded, and it is reloaded after every change.
 */
export function usePagedCrud<T extends { id: Id }, TInput>(
  resource: string,
  schema: z.ZodType<T>,
  initialSearch = '',
) {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [search, setSearch] = useState(initialSearch)
  const [appliedSearch, setAppliedSearch] = useState(initialSearch.trim())

  const [items, setItems] = useState<T[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  /** Only the newest request may update the screen, however the responses arrive. */
  const latest = useRef(0)

  // Wait for a pause in typing before asking the server.
  useEffect(() => {
    if (search.trim() === appliedSearch) return
    const timer = setTimeout(() => {
      setAppliedSearch(search.trim())
      setPage(1)
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [search, appliedSearch])

  const reload = useCallback(async () => {
    const request = ++latest.current
    setLoading(true)
    setError(null)
    try {
      const { data } = await api.get(`${resource}/page`, {
        params: { page: page - 1, size: pageSize, search: appliedSearch || undefined },
      })
      if (request !== latest.current) return
      const result = pageResponseSchema(schema).parse(data)
      const pages = Math.max(1, result.totalPages)
      if (page > pages) {
        setPage(pages) // the last row of the last page was removed
        return
      }
      setItems(result.items)
      setTotalItems(result.totalItems)
      setTotalPages(pages)
    } catch (err) {
      if (request === latest.current) setError(apiErrorMessage(err))
    } finally {
      if (request === latest.current) setLoading(false)
    }
  }, [resource, schema, page, pageSize, appliedSearch])

  useEffect(() => {
    void reload()
  }, [reload])

  /** Runs a change, shows its error if it fails (and rethrows), then reloads the page. */
  const change = useCallback(
    async (action: () => Promise<unknown>) => {
      setError(null)
      try {
        await action()
      } catch (err) {
        setError(apiErrorMessage(err))
        throw err
      }
      await reload()
    },
    [reload],
  )

  const create = useCallback(
    (input: TInput) => change(() => api.post(resource, input)),
    [change, resource],
  )
  const update = useCallback(
    (id: Id, input: TInput) => change(() => api.put(`${resource}/${id}`, input)),
    [change, resource],
  )
  const remove = useCallback((id: Id) => change(() => api.delete(`${resource}/${id}`)), [change, resource])

  const changePageSize = useCallback((size: number) => {
    setPageSize(size)
    setPage(1)
  }, [])

  return {
    items,
    loading,
    error,
    create,
    update,
    remove,
    reload,
    paging: {
      page,
      pageSize,
      totalItems,
      totalPages,
      search,
      onPageChange: setPage,
      onPageSizeChange: changePageSize,
      onSearchChange: setSearch,
    },
  }
}
