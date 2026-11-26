/** One page of an in-memory list. */
export type PagedResult<T> = {
  items: T[]
  total: number
  page: number
  pageSize: number
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
