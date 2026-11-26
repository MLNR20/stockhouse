import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../api.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import { useCrud } from '../hooks/useCrud.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import { supplierSchema, type Supplier } from '../schemas/supplier.ts'
import { apiErrorMessage } from '../utils/apiError.ts'
import ComboBox from './ComboBox.tsx'
import Modal from './Modal.tsx'
import { ListSkeleton } from './Skeleton.tsx'

type Props = {
  /** The supplier to manage, or null when closed. */
  supplier: Supplier | null
  onClose: () => void
  /** Called after a product is linked or unlinked. */
  onChanged: () => void
}

function Links({ supplier, onChanged }: { supplier: Supplier; onChanged: () => void }) {
  const { can } = usePermissions()
  const canEdit = can('suppliers:update')
  const { items: products, error: productsError } = useCrud<Product>('/api/products', { schema: productSchema })

  const [linked, setLinked] = useState<string[] | null>(null)
  const [choice, setChoice] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const { data } = await api.get(`/api/suppliers/${supplier.id}`)
      setLinked(supplierSchema.parse(data).productIds)
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }, [supplier.id])

  useEffect(() => {
    void load()
  }, [load])

  async function change(action: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    try {
      await action()
      await load()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function add(event: FormEvent) {
    event.preventDefault()
    if (!choice) {
      setError('Choose a product to add.')
      return
    }
    void change(async () => {
      await api.put(`/api/suppliers/${supplier.id}/products/${choice}`)
      setChoice('')
    })
  }

  const rows = products.filter((p) => linked?.includes(p.id))
  const available = products.filter((p) => !linked?.includes(p.id))

  return (
    <div className="space-y-4">
      <p className="text-sm text-slate-500">A reorder can only ask this supplier for the products listed here.</p>

      {(error || productsError) && (
        <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error ?? productsError}
        </p>
      )}

      {canEdit && (
        <form onSubmit={add} className="flex gap-2">
          <ComboBox
            value={choice}
            options={available.map((p) => ({ value: p.id, label: p.name }))}
            onChange={setChoice}
            placeholder="Type a product to add"
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
          <button
            type="submit"
            disabled={busy}
            className="shrink-0 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            Add
          </button>
        </form>
      )}

      {linked === null ? (
        <ListSkeleton />
      ) : rows.length === 0 ? (
        <p className="text-sm text-slate-500">This supplier has no products yet.</p>
      ) : (
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {rows.map((p) => (
            <li key={p.id} className="flex items-center justify-between gap-3 px-3 py-2 text-sm">
              <span className="text-slate-800">{p.name}</span>
              {canEdit && (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void change(() => api.delete(`/api/suppliers/${supplier.id}/products/${p.id}`))}
                  className="font-medium text-red-600 hover:underline disabled:opacity-50"
                >
                  Remove
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** The products a supplier provides, with adding and removing. */
export default function SupplierProducts({ supplier, onClose, onChanged }: Props) {
  return (
    <Modal open={supplier !== null} title={supplier ? `Products from ${supplier.supplierName}` : ''} onClose={onClose}>
      {supplier && <Links key={supplier.id} supplier={supplier} onChanged={onChanged} />}
    </Modal>
  )
}
