import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { z } from 'zod'
import { api } from '../api.ts'
import {
  orderDetailInputSchema,
  orderDetailSchema,
  type OrderDetail,
  type OrderDetailFormValues,
} from '../schemas/orderDetail.ts'
import type { Order } from '../schemas/order.ts'
import type { Product } from '../schemas/product.ts'
import type { UnitOfMeasure } from '../schemas/unitOfMeasure.ts'
import { apiErrorMessage } from '../utils/apiError.ts'
import ComboBox from './ComboBox.tsx'
import { formatMoney } from '../utils/format.ts'
import { TableRowsSkeleton } from './Skeleton.tsx'

const emptyForm: OrderDetailFormValues = { productId: '', howSoldId: '', quantity: '1', salesPrice: '' }

/** One row of the "add products" form. The key only identifies the row for React. */
type Row = OrderDetailFormValues & { key: number }
type RowErrors = Partial<Record<'productId' | 'quantity' | 'salesPrice', string>>

let rowKey = 0
const newRow = (): Row => ({ ...emptyForm, key: ++rowKey })
const isBlank = (row: Row) => !row.productId && !row.salesPrice

const inputClass =
  'w-full rounded-md border px-3 py-2 text-sm outline-none focus:ring-1 border-slate-300 focus:border-blue-500 focus:ring-blue-500'

type Props = {
  order: Order
  products: Product[]
  /** The "how sold" units a line can be counted in (Each, Case...). */
  units: UnitOfMeasure[]
  /** Whether the signed-in role may add, edit and delete lines. */
  canEdit: boolean
}

/**
 * The items of one order: product, unit, quantity, sales price, and how much of the product is on hand.
 * Render it with `key={order.id}` so a different order starts with fresh state.
 */
export default function OrderLines({ order, products, units, canEdit }: Props) {
  const [lines, setLines] = useState<OrderDetail[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<string | null>(null)
  const [rows, setRows] = useState<Row[]>(() => [newRow()])
  const [rowErrors, setRowErrors] = useState<Record<number, RowErrors>>({})
  const [saving, setSaving] = useState(false)

  // Lines can only change before the order ships; the server enforces this too.
  const editable = canEdit && (order.status === 'PENDING' || order.status === 'CONFIRMED')

  const basePrice = useMemo(() => new Map(products.map((p) => [p.id, p.basePrice])), [products])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await api.get(`/api/orders/${order.id}/details`)
      setLines(z.array(orderDetailSchema).parse(data))
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [order.id])

  useEffect(() => {
    void load()
  }, [load])

  const linesTotal = lines.reduce((sum, line) => sum + line.lineTotal, 0)
  const difference = Math.round((order.totalPrice - linesTotal) * 100) / 100

  function resetForm() {
    setEditingId(null)
    setRows([newRow()])
    setRowErrors({})
  }

  function startEdit(line: OrderDetail) {
    setEditingId(line.id)
    setRows([
      {
        key: ++rowKey,
        productId: line.productId,
        howSoldId: line.howSoldId ?? '',
        quantity: String(line.quantity),
        salesPrice: line.salesPrice.toFixed(2),
      },
    ])
    setRowErrors({})
    setError(null)
  }

  function updateRow(key: number, patch: Partial<OrderDetailFormValues>) {
    setRows((all) => all.map((r) => (r.key === key ? { ...r, ...patch } : r)))
  }

  function chooseProduct(key: number, productId: string) {
    // Start from the product's base price; the sales price can still be changed.
    const price = basePrice.get(productId)
    setRows((all) =>
      all.map((r) =>
        r.key === key ? { ...r, productId, salesPrice: price === undefined ? r.salesPrice : price.toFixed(2) } : r,
      ),
    )
    setRowErrors((e) => ({ ...e, [key]: { ...e[key], productId: undefined } }))
  }

  function removeRow(key: number) {
    setRows((all) => (all.length > 1 ? all.filter((r) => r.key !== key) : all))
    setRowErrors((e) => {
      const { [key]: _removed, ...rest } = e
      return rest
    })
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // Untouched extra rows are ignored; if every row is untouched the first one reports what is missing.
    let toSave = editingId ? rows : rows.filter((r) => !isBlank(r))
    if (toSave.length === 0) toSave = rows.slice(0, 1)

    const errors: Record<number, RowErrors> = {}
    const payloads: { row: Row; data: z.output<typeof orderDetailInputSchema> }[] = []
    for (const row of toSave) {
      const parsed = orderDetailInputSchema.safeParse(row)
      if (parsed.success) {
        payloads.push({ row, data: parsed.data })
      } else {
        const flat = z.flattenError(parsed.error).fieldErrors as Record<string, string[] | undefined>
        errors[row.key] = {
          productId: flat.productId?.[0],
          quantity: flat.quantity?.[0],
          salesPrice: flat.salesPrice?.[0],
        }
      }
    }
    setRowErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    let saved = 0
    try {
      for (const { data } of payloads) {
        if (editingId) {
          await api.put(`/api/order-details/${editingId}`, data)
        } else {
          await api.post(`/api/orders/${order.id}/details`, data)
        }
        saved++
      }
      resetForm()
    } catch (err) {
      // Keep the rows that were not saved so nothing has to be typed again.
      if (saved > 0) setRows((all) => all.filter((r) => !payloads.slice(0, saved).some((p) => p.row.key === r.key)))
      setError(
        saved > 0
          ? `Added ${saved} of ${payloads.length} items. ${apiErrorMessage(err)}`
          : apiErrorMessage(err),
      )
    } finally {
      setSaving(false)
      await load()
    }
  }

  async function remove(line: OrderDetail) {
    if (!window.confirm(`Remove ${line.productName} from this order?`)) return
    setError(null)
    try {
      await api.delete(`/api/order-details/${line.id}`)
      if (editingId === line.id) resetForm()
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const productOptions = useMemo(() => products.map((p) => ({ value: p.id, label: p.name })), [products])
  const unitOptions = useMemo(() => units.map((u) => ({ value: u.id, label: u.name })), [units])

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-3">
        <h2 className="text-lg font-semibold text-slate-900">Items</h2>
        {editable && !editingId && (
          <button
            type="button"
            onClick={() => setRows((all) => [...all, newRow()])}
            disabled={saving}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
          >
            + Add row
          </button>
        )}
      </div>

      {error && (
        <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {canEdit && !editable && (
        <p className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          The items of a {order.status.toLowerCase()} order can no longer be changed.
        </p>
      )}

      {/* Add / edit lines: one row per product, with more rows added on demand */}
      {editable && (
        <form onSubmit={submit} noValidate className="rounded-lg border border-slate-200 p-4">
          <h3 className="mb-3 text-sm font-semibold text-slate-900">
            {editingId ? 'Edit line' : rows.length > 1 ? 'Add products' : 'Add a product'}
          </h3>

          <div className="space-y-3">
            {rows.map((row, index) => {
              const errors = rowErrors[row.key] ?? {}
              const base = row.productId ? basePrice.get(row.productId) : undefined
              // Labels sit above the first row only on wide screens; later rows reuse them.
              const label = `mb-1 block text-sm font-medium text-slate-700 ${index > 0 ? 'sm:hidden' : ''}`
              return (
                <div key={row.key} className="grid gap-3 sm:grid-cols-[2fr_1fr_1fr_1fr_2.25rem] sm:items-start">
                  <div>
                    <label htmlFor={`od-product-${row.key}`} className={label}>
                      Product
                    </label>
                    <ComboBox
                      id={`od-product-${row.key}`}
                      value={row.productId}
                      options={productOptions}
                      onChange={(id) => chooseProduct(row.key, id)}
                      placeholder="Type or select a product"
                      aria-invalid={errors.productId ? true : undefined}
                      className={inputClass}
                    />
                    {errors.productId && <p className="mt-1 text-xs text-red-600">{errors.productId}</p>}
                  </div>

                  <div>
                    <label htmlFor={`od-unit-${row.key}`} className={label}>
                      Sold as
                    </label>
                    <ComboBox
                      id={`od-unit-${row.key}`}
                      value={row.howSoldId}
                      options={unitOptions}
                      onChange={(id) => updateRow(row.key, { howSoldId: id })}
                      placeholder="Product’s usual unit"
                      className={inputClass}
                    />
                  </div>

                  <div>
                    <label htmlFor={`od-quantity-${row.key}`} className={label}>
                      Quantity
                    </label>
                    <input
                      id={`od-quantity-${row.key}`}
                      type="number"
                      min={1}
                      value={row.quantity}
                      onChange={(e) => updateRow(row.key, { quantity: e.target.value })}
                      aria-invalid={errors.quantity ? true : undefined}
                      className={inputClass}
                    />
                    {errors.quantity && <p className="mt-1 text-xs text-red-600">{errors.quantity}</p>}
                  </div>

                  <div>
                    <label htmlFor={`od-price-${row.key}`} className={label}>
                      Sales price (₱)
                    </label>
                    <input
                      id={`od-price-${row.key}`}
                      value={row.salesPrice}
                      onChange={(e) => updateRow(row.key, { salesPrice: e.target.value })}
                      aria-invalid={errors.salesPrice ? true : undefined}
                      className={inputClass}
                    />
                    {errors.salesPrice ? (
                      <p className="mt-1 text-xs text-red-600">{errors.salesPrice}</p>
                    ) : (
                      base !== undefined && <p className="mt-1 text-xs text-slate-400">Base price {formatMoney(base)}</p>
                    )}
                  </div>

                  <div className={index === 0 ? 'sm:mt-6' : ''}>
                    {!editingId && rows.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeRow(row.key)}
                        disabled={saving}
                        aria-label={`Remove row ${index + 1}`}
                        title="Remove this row"
                        className="flex h-[2.25rem] w-[2.25rem] items-center justify-center rounded-md text-slate-400 hover:bg-red-50 hover:text-red-600"
                      >
                        <svg
                          className="h-4 w-4"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                          strokeWidth={2}
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          aria-hidden="true"
                        >
                          <path d="M6 6l12 12M6 18L18 6" />
                        </svg>
                      </button>
                    )}
                  </div>
                </div>
              )
            })}
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-2">
            <button
              type="submit"
              disabled={saving}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {saving
                ? 'Saving…'
                : editingId
                  ? 'Save'
                  : rows.filter((r) => !isBlank(r)).length > 1
                    ? `Add ${rows.filter((r) => !isBlank(r)).length} items`
                    : 'Add'}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={resetForm}
                disabled={saving}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      )}

      {/* The lines */}
      <div className="overflow-x-auto rounded-lg border border-slate-200">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3 font-medium">Product</th>
              <th className="px-4 py-3 font-medium">Sold as</th>
              <th className="px-4 py-3 font-medium">Qty</th>
              <th className="px-4 py-3 font-medium">Sales price</th>
              <th className="px-4 py-3 font-medium">Base price</th>
              <th className="px-4 py-3 font-medium">Line total</th>
              <th className="px-4 py-3 font-medium">Available</th>
              {editable && <th className="px-4 py-3 text-right font-medium">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <TableRowsSkeleton columns={editable ? 8 : 7} rows={3} />
            ) : lines.length === 0 ? (
              <tr>
                <td colSpan={editable ? 8 : 7} className="px-4 py-8 text-center text-slate-500">
                  This order has no items yet.
                </td>
              </tr>
            ) : (
              lines.map((line) => (
                <tr key={line.id} className="odd:bg-white even:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-900">{line.productName}</td>
                  <td className="px-4 py-3">{line.howSoldName ?? '—'}</td>
                  <td className="px-4 py-3">
                    {line.quantity.toLocaleString()}
                    {line.baseUnitName && line.baseQuantityNeeded !== line.quantity && (
                      <span className="block text-xs text-slate-400">
                        = {line.baseQuantityNeeded.toLocaleString()} {line.baseUnitName}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">{formatMoney(line.salesPrice)}</td>
                  <td className="px-4 py-3 text-slate-500">{formatMoney(line.basePrice)}</td>
                  <td className="px-4 py-3">{formatMoney(line.lineTotal)}</td>
                  <td className="px-4 py-3">
                    <span className="mr-2">
                      {line.availableQuantity.toLocaleString()}
                      {line.baseUnitName ? ` ${line.baseUnitName}` : ''}
                    </span>
                    {line.inStock ? (
                      <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800">
                        Enough
                      </span>
                    ) : (
                      <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800">
                        Short by {(line.baseQuantityNeeded - line.availableQuantity).toLocaleString()}
                      </span>
                    )}
                  </td>
                  {editable && (
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-3">
                        <button
                          type="button"
                          onClick={() => startEdit(line)}
                          className="text-sm font-medium text-blue-600 hover:underline"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => void remove(line)}
                          className="text-sm font-medium text-red-600 hover:underline"
                        >
                          Remove
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <hr className="border-slate-200" />

      {/* Totals */}
      <div className="flex flex-col gap-1 text-sm sm:items-end">
        <p className="text-slate-600">
          Items total: <span className="font-semibold text-slate-900">{formatMoney(linesTotal)}</span>
        </p>
        <p className="text-slate-600">
          Order total: <span className="font-semibold text-slate-900">{formatMoney(order.totalPrice)}</span>
        </p>
        {!loading && lines.length > 0 && difference !== 0 && (
          <p className="text-amber-700">
            The items are {formatMoney(Math.abs(difference))} {difference > 0 ? 'below' : 'above'} the order total.
          </p>
        )}
      </div>
    </div>
  )
}
