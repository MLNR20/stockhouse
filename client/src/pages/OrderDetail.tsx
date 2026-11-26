import { useEffect, useState } from 'react'
import axios from 'axios'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import OrderLines from '../components/OrderLines.tsx'
import StatusBadge from '../components/StatusBadge.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { orderSchema, type Order } from '../schemas/order.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import { customerLookupSchema, type CustomerLookup } from '../schemas/shipment.ts'
import { unitOfMeasureSchema, type UnitOfMeasure } from '../schemas/unitOfMeasure.ts'
import { formatDate, formatDateTime, shortId } from '../utils/format.ts'

const money = (amount: number) =>
  amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

type LoadState = 'loading' | 'ready' | 'missing' | 'error'

/** One order on its own page: a summary, then its items (products, units, prices and stock). */
export default function OrderDetail() {
  const { orderId } = useParams()
  const { can } = usePermissions()

  // Only used to name and offer products and units, so a failure here just leaves the dropdowns empty.
  const { items: products } = useCrud<Product>('/api/products', { schema: productSchema })
  const { items: units } = useCrud<UnitOfMeasure>('/api/how-sold', { schema: unitOfMeasureSchema })

  const [order, setOrder] = useState<Order | null>(null)
  const [customer, setCustomer] = useState<CustomerLookup | null>(null)
  const [state, setState] = useState<LoadState>('loading')

  useEffect(() => {
    let cancelled = false
    setState('loading')
    ;(async () => {
      try {
        const { data } = await api.get(`/api/orders/${orderId}`)
        const loaded = orderSchema.parse(data)
        const loadedCustomer = await api
          .get(`/api/customers/${loaded.customerId}`)
          .then((r) => customerLookupSchema.parse(r.data))
          .catch(() => null) // the page still works without the name
        if (cancelled) return
        setOrder(loaded)
        setCustomer(loadedCustomer)
        setState('ready')
      } catch (err) {
        if (!cancelled) setState(axios.isAxiosError(err) && err.response?.status === 404 ? 'missing' : 'error')
      }
    })()
    return () => {
      cancelled = true
    }
  }, [orderId])

  const back = (
    <Link to="/orders" className="text-sm font-medium text-blue-600 hover:underline">
      ← Back to orders
    </Link>
  )

  if (state !== 'ready' || !order) {
    return (
      <div className="rounded-lg border border-slate-200 bg-white p-8">
        {back}
        <p className="mt-6 text-slate-600">
          {state === 'loading' && 'Loading…'}
          {state === 'missing' && 'This order does not exist (it may have been deleted).'}
          {state === 'error' && 'Could not load this order. Please try again.'}
        </p>
      </div>
    )
  }

  const customerName = customer ? `${customer.firstName} ${customer.lastName}` : 'Unknown customer'

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-8">
      {back}

      <div className="mb-6 mt-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Order #{shortId(order.id)}</h1>
          <p className="mt-1 text-[1.175rem] text-slate-500">{customerName}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>

      <hr className="border-slate-200" />
      <dl className="grid grid-cols-2 gap-4 py-5 text-sm sm:grid-cols-4">
        <div>
          <dt className="text-slate-500">Customer</dt>
          <dd className="mt-1 font-medium text-slate-900">{customerName}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Delivery date</dt>
          <dd className="mt-1 font-medium text-slate-900">{formatDate(order.targetDeliveryDate)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Order total</dt>
          <dd className="mt-1 font-medium text-slate-900">{money(order.totalPrice)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Created / updated</dt>
          <dd className="mt-1 font-medium text-slate-900">
            {formatDateTime(order.createdAt)}
            <span className="block text-xs font-normal text-slate-400">
              {order.updatedAt ? `updated ${formatDateTime(order.updatedAt)}` : 'not edited yet'}
            </span>
          </dd>
        </div>
      </dl>
      <hr className="mb-8 border-slate-200" />

      <h2 className="mb-4 text-lg font-semibold text-slate-900">Items</h2>
      <OrderLines key={order.id} order={order} products={products} units={units} canEdit={can('orders:update')} />
    </div>
  )
}
