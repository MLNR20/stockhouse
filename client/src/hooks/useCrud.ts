import { useCallback, useEffect, useState } from 'react'
import { z } from 'zod'
import { api } from '../api.ts'
import { apiErrorMessage as errorMessage } from '../utils/apiError.ts'

type Id = string | number

type UseCrudOptions<T> = {
  /** Optional zod schema; responses are validated against it. */
  schema?: z.ZodType<T>
  /** Fetch the list on mount (default: true). */
  autoFetch?: boolean
}

/**
 * Generic CRUD hook for a REST resource, e.g. useCrud<Item>('/items').
 *
 *   GET    /items        -> fetchAll
 *   POST   /items        -> create
 *   PUT    /items/:id    -> update
 *   DELETE /items/:id    -> remove
 *
 * Mutations update local state and rethrow on failure so callers can react.
 */
export function useCrud<T extends { id: Id }, TInput = Omit<T, 'id'>>(
  resource: string,
  { schema, autoFetch = true }: UseCrudOptions<T> = {},
) {
  const [items, setItems] = useState<T[]>([])
  const [loading, setLoading] = useState(autoFetch)
  const [error, setError] = useState<string | null>(null)

  const parseOne = useCallback((data: unknown): T => (schema ? schema.parse(data) : (data as T)), [schema])

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await api.get(resource)
      setItems(schema ? z.array(schema).parse(data) : (data as T[]))
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [resource, schema])

  const create = useCallback(
    async (input: TInput): Promise<T> => {
      setError(null)
      try {
        const { data } = await api.post(resource, input)
        const created = parseOne(data)
        setItems((prev) => [...prev, created])
        return created
      } catch (err) {
        setError(errorMessage(err))
        throw err
      }
    },
    [resource, parseOne],
  )

  const update = useCallback(
    async (id: Id, input: Partial<TInput>): Promise<T> => {
      setError(null)
      try {
        const { data } = await api.put(`${resource}/${id}`, input)
        const updated = parseOne(data)
        setItems((prev) => prev.map((item) => (item.id === id ? updated : item)))
        return updated
      } catch (err) {
        setError(errorMessage(err))
        throw err
      }
    },
    [resource, parseOne],
  )

  const remove = useCallback(
    async (id: Id): Promise<void> => {
      setError(null)
      try {
        await api.delete(`${resource}/${id}`)
        setItems((prev) => prev.filter((item) => item.id !== id))
      } catch (err) {
        setError(errorMessage(err))
        throw err
      }
    },
    [resource],
  )

  useEffect(() => {
    if (autoFetch) void fetchAll()
  }, [autoFetch, fetchAll])

  return { items, loading, error, fetchAll, create, update, remove }
}
