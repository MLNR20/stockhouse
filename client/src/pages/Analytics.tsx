import { useEffect, useState } from 'react'
import { api } from '../api.ts'
import { StatCard } from '../components/AnalyticsWidgets.tsx'
import ComboBox from '../components/ComboBox.tsx'
import LineChart from '../components/LineChart.tsx'
import { Skeleton, TableSkeleton } from '../components/Skeleton.tsx'
import { useCrud } from '../hooks/useCrud.ts'
import { productSchema, type Product } from '../schemas/product.ts'
import { trendSchema, type Trend } from '../schemas/trend.ts'
import { apiErrorMessage } from '../utils/apiError.ts'
import { formatDate } from '../utils/format.ts'

const RANGES = [14, 30, 60, 90]
/** Blue for stock, orange for orders: the colour follows the measure whatever is filtered. */
const STOCK_COLOR = '#2a78d6'
const ORDERS_COLOR = '#eb6834'

const shortDate = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' })
const axisDate = (iso: string) => shortDate.format(new Date(`${iso}T00:00:00`))

export default function Analytics() {
  const [days, setDays] = useState(30)
  const [productId, setProductId] = useState('')
  const [trend, setTrend] = useState<Trend | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showTable, setShowTable] = useState(false)

  // Only used for the product filter, so a failure here just leaves "All products".
  const { items: products } = useCrud<Product>('/api/products', { schema: productSchema })

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    api
      .get('/api/analytics/orders-vs-inventory', { params: { days, productId: productId || undefined } })
      .then((r) => {
        if (!cancelled) setTrend(trendSchema.parse(r.data))
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(apiErrorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [days, productId])

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Orders vs Inventory</h1>
        <p className="mt-1 text-[1.175rem] text-slate-500">
          From today: how open orders and expected reorders will change the stock on hand.
        </p>
      </div>

      {/* Filters apply to everything below. */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div role="group" aria-label="Date range" className="flex overflow-hidden rounded-md border border-slate-300">
          {RANGES.map((range) => (
            <button
              key={range}
              type="button"
              aria-pressed={days === range}
              onClick={() => setDays(range)}
              className={`px-3 py-2 text-sm font-medium ${
                days === range ? 'bg-blue-600 text-white' : 'bg-white text-slate-700 hover:bg-slate-100'
              }`}
            >
              {range} days
            </button>
          ))}
        </div>
        <ComboBox
          value={productId}
          options={products.map((p) => ({ value: p.id, label: p.name }))}
          onChange={setProductId}
          placeholder="All products (type to filter)"
          className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 sm:w-72"
        />
      </div>

      {error && (
        <p role="alert" className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {!trend ? (
        loading && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
              {Array.from({ length: 4 }, (_, i) => (
                <Skeleton key={i} className="h-24 rounded-lg" />
              ))}
            </div>
            <TableSkeleton columns={6} rows={6} label="Loading the chart…" />
          </div>
        )
      ) : (
        // While a new range loads the old chart stays, dimmed.
        <div className={`space-y-4 ${loading ? 'opacity-60' : ''}`} aria-busy={loading}>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard label="Stock now" value={trend.stockNow.toLocaleString()} hint="Base units on hand" />
            <StatCard
              label="Ordered"
              value={trend.totalOrdered.toLocaleString()}
              hint={`Open orders due by ${formatDate(trend.to)}`}
            />
            <StatCard
              label="Incoming"
              value={trend.totalIncoming.toLocaleString()}
              hint="From reorders marked ordered"
            />
            <StatCard
              label="First shortage"
              value={trend.firstShortage ? axisDate(trend.firstShortage) : 'None'}
              tone={trend.firstShortage ? 'danger' : 'good'}
              hint={trend.firstShortage ? 'Stock falls below zero' : 'Stock covers every order'}
            />
          </div>

          <section className="rounded-lg border border-slate-200 bg-white p-4">
            <div className="mb-2 flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold text-slate-900">Projected stock and units ordered, by date</h2>
              <button
                type="button"
                onClick={() => setShowTable((s) => !s)}
                className="text-sm font-medium text-blue-600 hover:underline"
              >
                {showTable ? 'Show chart' : 'Show table'}
              </button>
            </div>

            {showTable ? (
              <div className="max-h-96 overflow-auto rounded-lg border border-slate-200">
                <table className="w-full text-sm">
                  <thead className="sticky top-0 bg-slate-50 text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-4 py-2 text-left font-medium">Date</th>
                      <th className="px-4 py-2 text-right font-medium">Ordered</th>
                      <th className="px-4 py-2 text-right font-medium">Ordered so far</th>
                      <th className="px-4 py-2 text-right font-medium">Incoming</th>
                      <th className="px-4 py-2 text-right font-medium">Projected stock</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 tabular-nums">
                    {trend.days.map((d) => (
                      <tr key={d.date}>
                        <td className="px-4 py-2 text-slate-800">{formatDate(d.date)}</td>
                        <td className="px-4 py-2 text-right">{d.ordered.toLocaleString()}</td>
                        <td className="px-4 py-2 text-right">{d.orderedTotal.toLocaleString()}</td>
                        <td className="px-4 py-2 text-right">{d.incoming.toLocaleString()}</td>
                        <td className={`px-4 py-2 text-right ${d.stock < 0 ? 'font-semibold text-red-600' : ''}`}>
                          {d.stock.toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <LineChart
                labels={trend.days.map((d) => axisDate(d.date))}
                fullLabels={trend.days.map((d) => formatDate(d.date))}
                series={[
                  { name: 'Projected stock', color: STOCK_COLOR, values: trend.days.map((d) => d.stock) },
                  { name: 'Units ordered (running total)', color: ORDERS_COLOR, values: trend.days.map((d) => d.orderedTotal) },
                ]}
                details={(i) => [
                  `Ordered that day: ${trend.days[i].ordered.toLocaleString()}`,
                  `Incoming that day: ${trend.days[i].incoming.toLocaleString()}`,
                ]}
                description={`Line chart of projected stock and running total of units ordered from ${formatDate(trend.from)} to ${formatDate(trend.to)}.`}
              />
            )}
          </section>

          <p className="text-xs text-slate-400">
            Quantities are in base units. Overdue orders and deliveries are counted today. The projection assumes
            reorders arrive on their expected date.
          </p>
        </div>
      )}
    </div>
  )
}
