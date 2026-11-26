import { useCallback, useEffect, useState, type ReactNode } from 'react'
import axios from 'axios'
import { z } from 'zod'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import StatusBadge from '../components/StatusBadge.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { conversionSchema, type Conversion } from '../schemas/conversion.ts'
import { inventorySchema, type Inventory } from '../schemas/inventory.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import { reorderSchema, type Reorder } from '../schemas/reorder.ts'
import { supplierSchema, type Supplier } from '../schemas/supplier.ts'
import { unitOfMeasureSchema, type UnitOfMeasure } from '../schemas/unitOfMeasure.ts'
import { apiErrorMessage } from '../utils/apiError.ts'
import { formatDate, formatDateTime, formatMoney, shortId } from '../utils/format.ts'
import { DetailSkeleton } from '../components/Skeleton.tsx'

type LoadState = 'loading' | 'ready' | 'missing' | 'error'

type Data = {
  reorder: Reorder
  supplier: Supplier | null
  product: Product | null
  stock: Inventory[]
  conversions: Conversion[]
}

function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 p-5">
      <h2 className="mb-4 text-sm font-semibold text-slate-900">{title}</h2>
      {children}
    </section>
  )
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-1.5 text-sm">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium text-slate-900">{children}</dd>
    </div>
  )
}

/**
 * One supplier reorder on its own page: the request, who supplies it, how much of the product is in stock
 * and how its units convert, with buttons to move it along (ordered, received, cancelled).
 */
export default function ReorderDetail() {
  const { reorderId } = useParams()
  const { can } = usePermissions()
  const { items: units } = useCrud<UnitOfMeasure>('/api/how-sold', { schema: unitOfMeasureSchema })

  const [data, setData] = useState<Data | null>(null)
  const [state, setState] = useState<LoadState>('loading')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const { data: raw } = await api.get(`/api/reorders/${reorderId}`)
      const reorder = reorderSchema.parse(raw)
      // The extras are context only: if one fails the page still shows the reorder.
      const [supplier, product, stock, conversions] = await Promise.all([
        api.get(`/api/suppliers/${reorder.supplierId}`).then((r) => supplierSchema.parse(r.data)).catch(() => null),
        api.get(`/api/products/${reorder.productId}`).then((r) => productSchema.parse(r.data)).catch(() => null),
        api
          .get(`/api/inventory?productId=${reorder.productId}`)
          .then((r) => z.array(inventorySchema).parse(r.data))
          .catch(() => []),
        api
          .get(`/api/how-sold-conversions?productId=${reorder.productId}`)
          .then((r) => z.array(conversionSchema).parse(r.data))
          .catch(() => []),
      ])
      setData({ reorder, supplier, product, stock, conversions })
      setState('ready')
    } catch (err) {
      setState(axios.isAxiosError(err) && err.response?.status === 404 ? 'missing' : 'error')
    }
  }, [reorderId])

  useEffect(() => {
    setState('loading')
    void load()
  }, [load])

  const unitName = (id: string | null) => (id ? (units.find((u) => u.id === id)?.name ?? '—') : '—')

  async function changeStatus(next: 'ORDERED' | 'RECEIVED' | 'CANCELLED') {
    if (!data) return
    const { reorder } = data
    const question =
      next === 'RECEIVED'
        ? 'Mark this reorder as received? Low and out-of-stock items for the product go back to In Stock.'
        : next === 'CANCELLED'
          ? 'Cancel this reorder? It cannot be changed afterwards.'
          : null
    if (question && !window.confirm(question)) return

    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      await api.put(`/api/reorders/${reorder.id}`, {
        supplierId: reorder.supplierId,
        productId: reorder.productId,
        quantity: reorder.quantity,
        howSoldId: reorder.howSoldId,
        expectedDate: reorder.expectedDate,
        status: next,
      })
      setNotice(
        next === 'RECEIVED'
          ? 'Marked as received. Stock for this product has been updated.'
          : next === 'ORDERED'
            ? 'Marked as ordered.'
            : 'Reorder cancelled.',
      )
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const back = (
    <Link to="/reorders" className="text-sm font-medium text-blue-600 hover:underline">
      ← Back to reorders
    </Link>
  )

  if (state !== 'ready' || !data) {
    return (
      <div className="rounded-lg border border-slate-200 bg-white p-8">
        {back}
        {state === 'loading' || state === 'ready' ? (
          <DetailSkeleton />
        ) : (
          <p className="mt-6 text-slate-600">
            {state === 'missing' && 'This reorder does not exist (it may have been deleted).'}
            {state === 'error' && 'Could not load this reorder. Please try again.'}
          </p>
        )}
      </div>
    )
  }

  const { reorder, supplier, product, stock, conversions } = data
  const productName = product?.name ?? 'Unknown product'
  const onHand = stock.reduce((sum, s) => sum + s.baseQuantity, 0)
  const canAct = can('reorders:update') && !busy

  const buttonBase = 'rounded-md px-4 py-2 text-sm font-medium disabled:opacity-50'

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      {back}

      <div className="mb-6 mt-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">
            Reorder of {productName} <span className="font-normal text-slate-400">#{shortId(reorder.id)}</span>
          </h1>
          <p className="mt-1 text-[1.175rem] text-slate-500">
            {reorder.quantity.toLocaleString()}
            {reorder.howSoldName ? ` ${reorder.howSoldName}` : ''} from {supplier?.supplierName ?? 'an unknown supplier'}
          </p>
        </div>
        <StatusBadge status={reorder.status} />
      </div>

      {error && (
        <p role="alert" className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}
      {notice && (
        <p role="status" className="mb-4 rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
          {notice}
        </p>
      )}

      {/* Moving the reorder along */}
      <div className="mb-8 flex flex-wrap items-center gap-3">
        {reorder.status === 'DRAFT' && (
          <button
            type="button"
            disabled={!canAct}
            onClick={() => void changeStatus('ORDERED')}
            className={`${buttonBase} bg-blue-600 text-white hover:bg-blue-700`}
          >
            Mark as ordered
          </button>
        )}
        {reorder.status === 'ORDERED' && (
          <button
            type="button"
            disabled={!canAct}
            onClick={() => void changeStatus('RECEIVED')}
            className={`${buttonBase} bg-green-600 text-white hover:bg-green-700`}
          >
            Mark as received
          </button>
        )}
        {(reorder.status === 'DRAFT' || reorder.status === 'ORDERED') && (
          <button
            type="button"
            disabled={!canAct}
            onClick={() => void changeStatus('CANCELLED')}
            className={`${buttonBase} border border-red-300 text-red-700 hover:bg-red-50`}
          >
            Cancel reorder
          </button>
        )}
        {(reorder.status === 'RECEIVED' || reorder.status === 'CANCELLED') && (
          <p className="text-sm text-slate-500">
            This reorder is {reorder.status.toLowerCase()} and can no longer be changed.
          </p>
        )}
        {!can('reorders:update') && (reorder.status === 'DRAFT' || reorder.status === 'ORDERED') && (
          <p className="text-sm text-slate-500">Your role cannot change reorders.</p>
        )}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Reorder">
          <dl>
            <Row label="Product">{productName}</Row>
            <Row label="Supplier">{supplier?.supplierName ?? '—'}</Row>
            <Row label="Quantity">{reorder.quantity.toLocaleString()}</Row>
            <Row label="Sold as">{reorder.howSoldName ?? '—'}</Row>
            <Row label="Expected delivery">{formatDate(reorder.expectedDate)}</Row>
            <Row label="Created">{formatDateTime(reorder.createdAt)}</Row>
            <Row label="Updated">{formatDateTime(reorder.updatedAt)}</Row>
          </dl>
        </Card>

        <Card title="Supplier">
          {supplier ? (
            <dl>
              <Row label="Name">{supplier.supplierName}</Row>
              <Row label="Phone">{supplier.phoneNumber}</Row>
              <Row label="Email">
                <a href={`mailto:${supplier.email}`} className="text-blue-600 hover:underline">
                  {supplier.email}
                </a>
              </Row>
              <Row label="Products supplied">{supplier.productIds.length}</Row>
            </dl>
          ) : (
            <p className="text-sm text-slate-500">The supplier could not be loaded.</p>
          )}
        </Card>

        <Card title="Stock of this product">
          <dl>
            <Row label="Base price">{product ? formatMoney(product.basePrice) : '—'}</Row>
            <Row label="On hand (all records)">{onHand.toLocaleString()}</Row>
          </dl>
          {stock.length === 0 ? (
            <p className="mt-3 text-sm text-slate-500">No inventory record for this product.</p>
          ) : (
            <ul className="mt-3 divide-y divide-slate-100 text-sm">
              {stock.map((s) => (
                <li key={s.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                  <span>
                    <StatusBadge status={s.status} />
                    <span className="ml-2 text-slate-500">sold as {unitName(s.howSoldId)}</span>
                  </span>
                  <span className={s.belowMinimum ? 'font-semibold text-red-600' : 'text-slate-700'}>
                    {s.baseQuantity.toLocaleString()} on hand · minimum {s.minimumThreshold.toLocaleString()}
                    {s.belowMinimum && ' · below minimum'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Unit conversions for this product">
          {conversions.length === 0 ? (
            <p className="text-sm text-slate-500">
              This product has no conversions of its own, so the general ones apply.{' '}
              <Link to="/conversions" className="text-blue-600 hover:underline">
                See all conversions
              </Link>
            </p>
          ) : (
            <ul className="space-y-1 text-sm text-slate-700">
              {conversions.map((c) => (
                <li key={c.id}>
                  1 {unitName(c.parentHowSoldId)} = {c.unitConversion.toLocaleString(undefined, { maximumFractionDigits: 4 })}{' '}
                  {unitName(c.childHowSoldId)}
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}
