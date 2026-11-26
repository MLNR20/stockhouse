import { useCallback, useEffect, useState } from 'react'
import axios from 'axios'
import { api } from '../api.ts'
import { BarList, ChartCard, ColumnChart, StatCard, StatIcon } from '../components/AnalyticsWidgets.tsx'
import { analyticsSchema, type Analytics } from '../schemas/analytics.ts'
import { formatMoney } from '../utils/format.ts'

const ICONS = {
  orders: (
    <StatIcon>
      <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 012-2h2a2 2 0 012 2M9 5h6M9 12h6M9 16h4" />
    </StatIcon>
  ),
  overdue: (
    <StatIcon>
      <path d="M12 21a9 9 0 100-18 9 9 0 000 18zM12 7v5l3 2" />
    </StatIcon>
  ),
  value: (
    <StatIcon>
      <path d="M12 2v20M17 6.5C16 5.5 14.5 5 12.5 5 9.5 5 8 6.4 8 8.3c0 4 9 2.2 9 6.4 0 2-1.8 3.3-4.8 3.3-2.2 0-3.9-.6-5.2-1.8" />
    </StatIcon>
  ),
  revenue: (
    <StatIcon>
      <path d="M3 17l6-6 4 4 8-8M15 7h6v6" />
    </StatIcon>
  ),
  products: (
    <StatIcon>
      <path d="M21 8l-9-5-9 5 9 5 9-5zM3 8v8l9 5 9-5V8M12 13v8" />
    </StatIcon>
  ),
  suppliers: (
    <StatIcon>
      <path d="M3 21h18M5 21V7l8-4v18M19 21V11l-6-4M9 9h.01M9 12h.01M9 15h.01M9 18h.01" />
    </StatIcon>
  ),
  customers: (
    <StatIcon>
      <path d="M17 21v-2a4 4 0 00-4-4H7a4 4 0 00-4 4v2M10 11a4 4 0 100-8 4 4 0 000 8zM21 21v-2a4 4 0 00-3-3.87M17 3.13a4 4 0 010 7.75" />
    </StatIcon>
  ),
  containers: (
    <StatIcon>
      <path d="M3 7h18v12H3zM3 7l2-3h14l2 3M9 11v4M15 11v4" />
    </StatIcon>
  ),
}

const ORDER_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-400',
  CONFIRMED: 'bg-blue-500',
  SHIPPED: 'bg-indigo-500',
  DELIVERED: 'bg-green-500',
  CANCELLED: 'bg-slate-400',
}

const INVENTORY_STATUS_COLORS: Record<string, string> = {
  IN_STOCK: 'bg-green-500',
  LOW_STOCK: 'bg-amber-400',
  OUT_OF_STOCK: 'bg-red-500',
  DISCONTINUED: 'bg-slate-400',
}

const humanize = (status: string) => status.replace(/_/g, ' ').toLowerCase()

/** 'YYYY-MM-DD' -> a Date at local midnight (new Date('2026-11-24') would be UTC and can shift a day). */
const localDate = (iso: string) => new Date(`${iso}T00:00:00`)

export default function Home() {
  const [data, setData] = useState<Analytics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get('/api/analytics/summary')
      setData(analyticsSchema.parse(response.data))
    } catch (err) {
      setError(axios.isAxiosError(err) ? 'Could not load the analytics.' : 'The analytics response was not what was expected.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  return (
    <div>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="mt-1 text-[1.175rem] text-slate-500">Orders, inventory and catalog at a glance.</p>
        </div>
        <button
          type="button"
          onClick={() => void load()}
          disabled={loading}
          className="shrink-0 rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50"
        >
          {loading ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      <div className="space-y-4">
        {error && (
          <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </p>
        )}

        {!data && loading && <p className="py-10 text-center text-sm text-slate-500">Loading…</p>}

        {data && (
          <>
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
              <StatCard
                label="Open orders"
                value={data.orders.open}
                hint={`${data.orders.total} orders in total`}
                icon={ICONS.orders}
              />
              <StatCard
                label="Overdue deliveries"
                value={data.orders.overdue}
                tone={data.orders.overdue > 0 ? 'danger' : 'good'}
                hint="Open orders past their target date"
                icon={ICONS.overdue}
              />
              <StatCard label="Open order value" value={formatMoney(data.orders.openValue)} icon={ICONS.value} />
              <StatCard
                label="Revenue"
                value={formatMoney(data.orders.revenue)}
                hint="All orders except cancelled"
                icon={ICONS.revenue}
              />
              <StatCard
                label="Products"
                value={data.totals.products}
                hint={`${data.totals.categories} categories`}
                icon={ICONS.products}
              />
              <StatCard label="Suppliers" value={data.totals.suppliers} icon={ICONS.suppliers} />
              <StatCard label="Customers" value={data.totals.customers} icon={ICONS.customers} />
              <StatCard
                icon={ICONS.containers}
                label="Containers"
                value={data.totals.containers}
                hint={`${data.totals.warehouses} ${data.totals.warehouses === 1 ? 'warehouse' : 'warehouses'}`}
              />
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <ChartCard title="Orders by status">
                <BarList
                  items={data.orders.byStatus.map((s) => ({
                    label: humanize(s.status),
                    value: s.count,
                    color: ORDER_STATUS_COLORS[s.status] ?? 'bg-slate-400',
                  }))}
                />
              </ChartCard>

              <ChartCard title="Inventory by status">
                <BarList
                  items={data.inventoryByStatus.map((s) => ({
                    label: humanize(s.status),
                    value: s.count,
                    color: INVENTORY_STATUS_COLORS[s.status] ?? 'bg-slate-400',
                  }))}
                />
              </ChartCard>

              <ChartCard title="Products by category">
                <BarList
                  emptyMessage="No products yet."
                  items={data.productsByCategory.map((c) => ({
                    label: c.category,
                    value: c.count,
                    color: 'bg-blue-500',
                  }))}
                />
              </ChartCard>

              <ChartCard title="Open orders due, next 14 days">
                <ColumnChart
                  columns={data.deliveriesNext14Days.map((d) => {
                    const date = localDate(d.date)
                    return {
                      label: String(date.getDate()),
                      title: date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' }),
                      value: d.count,
                    }
                  })}
                />
              </ChartCard>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
