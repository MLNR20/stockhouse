import { useMemo } from 'react'
import { aisleSchema, warehouseSchema, type Aisle, type Warehouse } from '../schemas/aisle.ts'
import { shelfSchema, type Shelf } from '../schemas/shelf.ts'
import { useCrud } from './useCrud.ts'

/**
 * "Warehouse / Aisle / Shelf" for every shelf, so places with the same short name can be told apart.
 * Used to name a row's shelf and a container's row; a failed load just leaves the names blank.
 */
export function useShelfLabels() {
  const { items: shelves, loading, error } = useCrud<Shelf>('/api/shelves', { schema: shelfSchema })
  const { items: aisles } = useCrud<Aisle>('/api/aisles', { schema: aisleSchema })
  const { items: warehouses } = useCrud<Warehouse>('/api/warehouses', { schema: warehouseSchema })

  const labels = useMemo(() => {
    const warehouseName = new Map(warehouses.map((w) => [w.id, w.name]))
    const aisleLabel = new Map(
      aisles.map((a) => [a.id, `${warehouseName.get(a.warehouseId) ?? '—'} / ${a.name}`]),
    )
    return new Map(shelves.map((s) => [s.id, `${aisleLabel.get(s.aisleId) ?? '—'} / ${s.name}`]))
  }, [shelves, aisles, warehouses])

  return { shelves, labels, loading, error }
}
