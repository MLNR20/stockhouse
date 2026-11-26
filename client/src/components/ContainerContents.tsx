import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { z } from 'zod'
import { api } from '../api.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import { useCrud } from '../hooks/useCrud.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import { containerContentSchema, type ContainerContent, type StorageContainer } from '../schemas/storage.ts'
import { apiErrorMessage } from '../utils/apiError.ts'
import ComboBox from './ComboBox.tsx'
import Modal from './Modal.tsx'

const inputClass =
  'rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500'

function Contents({ container }: { container: StorageContainer }) {
  const { can } = usePermissions()
  const canEdit = can('shelves:update')
  const { items: products } = useCrud<Product>('/api/products', { schema: productSchema })

  const [contents, setContents] = useState<ContainerContent[] | null>(null)
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const { data } = await api.get(`/api/containers/${container.id}/products`)
      setContents(z.array(containerContentSchema).parse(data))
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }, [container.id])

  useEffect(() => {
    void load()
  }, [load])

  async function change(action: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    try {
      await action()
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function save(event: FormEvent) {
    event.preventDefault()
    const amount = Number(quantity)
    if (!productId) {
      setError('Choose a product.')
    } else if (!Number.isInteger(amount) || amount < 1) {
      setError('Quantity must be a whole number of at least 1.')
    } else {
      void change(async () => {
        await api.put(`/api/containers/${container.id}/products/${productId}`, { quantity: amount })
        setProductId('')
        setQuantity('1')
      })
    }
  }

  const productName = new Map(products.map((p) => [p.id, p.name]))

  return (
    <div className="space-y-4">
      {error && (
        <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {canEdit && (
        <form onSubmit={save} noValidate className="space-y-2">
          <div className="flex gap-2">
            <ComboBox
              value={productId}
              options={products.map((p) => ({ value: p.id, label: p.name }))}
              onChange={setProductId}
              placeholder="Type a product"
              className={`w-full ${inputClass}`}
            />
            <input
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              aria-label="Quantity"
              className={`w-24 ${inputClass}`}
            />
            <button
              type="submit"
              disabled={busy}
              className="shrink-0 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              Save
            </button>
          </div>
          <p className="text-xs text-slate-400">Saving a product that is already inside sets its new quantity.</p>
        </form>
      )}

      {contents === null ? (
        <p className="text-sm text-slate-500">Loading…</p>
      ) : contents.length === 0 ? (
        <p className="text-sm text-slate-500">This container is empty.</p>
      ) : (
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {contents.map((c) => (
            <li key={c.id} className="flex items-center justify-between gap-3 px-3 py-2 text-sm">
              <span className="text-slate-800">{productName.get(c.productId) ?? '—'}</span>
              <span className="flex items-center gap-4">
                <span className="font-medium text-slate-900">{c.quantity.toLocaleString()}</span>
                {canEdit && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() =>
                      void change(() => api.delete(`/api/containers/${container.id}/products/${c.productId}`))
                    }
                    className="font-medium text-red-600 hover:underline disabled:opacity-50"
                  >
                    Remove
                  </button>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** The products inside one container, with adding, changing quantities and removing. */
export default function ContainerContents({
  container,
  onClose,
}: {
  container: StorageContainer | null
  onClose: () => void
}) {
  return (
    <Modal open={container !== null} title={container ? `Contents of ${container.name}` : ''} onClose={onClose}>
      {container && <Contents key={container.id} container={container} />}
    </Modal>
  )
}
