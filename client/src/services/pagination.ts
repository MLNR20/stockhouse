import { z } from 'zod'
import { api } from '../api.ts'

/** Query params sent to the backend. Pages are 1-based. */
export type PageParams = {
  page: number
  pageSize: number
  search?: string
}

export type PagedResult<T> = {
  items: T[]
  total: number
  page: number
  pageSize: number
}

/** Zod schema for a paged response: { items, total, page, pageSize }. Adjust to match your API. */
export function pagedSchema<T>(item: z.ZodType<T>) {
  return z.object({
    items: z.array(item),
    total: z.number().int().nonnegative(),
    page: z.number().int().positive(),
    pageSize: z.number().int().positive(),
  })
}

/** Server-side pagination: GET {resource}?page=&pageSize=&search= */
export async function fetchPage<T>(
  resource: string,
  { page, pageSize, search }: PageParams,
  schema?: z.ZodType<T>,
): Promise<PagedResult<T>> {
  const { data } = await api.get(resource, {
    params: { page, pageSize, search: search?.trim() || undefined },
  })
  return schema ? pagedSchema(schema).parse(data) : (data as PagedResult<T>)
}

/** Client-side pagination of an in-memory list. Pages are 1-based; out-of-range pages are clamped. */
export function paginate<T>(items: T[], page: number, pageSize: number): PagedResult<T> {
  const totalPages = getTotalPages(items.length, pageSize)
  const current = Math.min(Math.max(1, page), totalPages)
  const start = (current - 1) * pageSize
  return { items: items.slice(start, start + pageSize), total: items.length, page: current, pageSize }
}

export function getTotalPages(total: number, pageSize: number): number {
  return Math.max(1, Math.ceil(total / pageSize))
}

/** Page numbers with '…' gaps, e.g. [1, '…', 4, 5, 6, '…', 20]. */
export function getPageList(current: number, total: number): (number | '…')[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const sorted = [...new Set([1, total, current - 1, current, current + 1])]
    .filter((p) => p >= 1 && p <= total)
    .sort((a, b) => a - b)
  const result: (number | '…')[] = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push('…')
    result.push(p)
  })
  return result
}
