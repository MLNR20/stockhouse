import { useEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react'

export type LineSeries = {
  name: string
  /** A hex colour; the pair used by the app has been checked for colour-blind separation and contrast. */
  color: string
  values: number[]
}

type LineChartProps = {
  /** One label per point along the x axis. */
  labels: string[]
  /** Longer form of each label, for the tooltip. */
  fullLabels?: string[]
  series: LineSeries[]
  /** Extra lines for the tooltip at one point, e.g. what happened that day. */
  details?: (index: number) => string[]
  /** What the chart shows, for screen readers. */
  description: string
  height?: number
}

const MARGIN = { top: 16, right: 64, bottom: 28, left: 56 }
const number = new Intl.NumberFormat()
const compact = new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 })

/** About five round-numbered ticks covering the range, always including zero. */
function ticksFor(min: number, max: number): number[] {
  const low = Math.min(0, min)
  const high = Math.max(max, low + 1)
  const rough = (high - low) / 4
  const magnitude = 10 ** Math.floor(Math.log10(rough))
  const step = Math.max(1, [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= rough) ?? rough)
  const ticks: number[] = []
  for (let t = Math.floor(low / step) * step; t < high + step; t += step) ticks.push(t)
  return ticks
}

/**
 * A line chart with one value axis: thin lines, quiet gridlines, a legend, each line's latest value at its end,
 * and a crosshair with a tooltip on hover or with the arrow keys.
 */
export default function LineChart({
  labels,
  fullLabels = labels,
  series,
  details,
  description,
  height = 320,
}: LineChartProps) {
  const box = useRef<HTMLDivElement>(null)
  const [width, setWidth] = useState(640)
  const [active, setActive] = useState<number | null>(null)

  useEffect(() => {
    const element = box.current
    if (!element) return
    const observer = new ResizeObserver(([entry]) => setWidth(Math.max(280, entry.contentRect.width)))
    observer.observe(element)
    return () => observer.disconnect()
  }, [])

  const count = labels.length
  const all = series.flatMap((s) => s.values)
  const ticks = ticksFor(Math.min(...all, 0), Math.max(...all, 0))
  const low = ticks[0]
  const high = ticks[ticks.length - 1]
  const plotWidth = width - MARGIN.left - MARGIN.right
  const plotHeight = height - MARGIN.top - MARGIN.bottom
  const x = (i: number) => MARGIN.left + (count <= 1 ? plotWidth / 2 : (i / (count - 1)) * plotWidth)
  const y = (v: number) => MARGIN.top + ((high - v) / (high - low)) * plotHeight

  // As many date labels as fit without touching.
  const every = Math.max(1, Math.ceil(count / Math.max(2, Math.floor(plotWidth / 64))))

  // Latest values at the line ends, pushed apart if they would overlap.
  const ends = series
    .map((s) => ({ color: s.color, value: s.values[count - 1] ?? 0, y: y(s.values[count - 1] ?? 0) }))
    .sort((a, b) => a.y - b.y)
  ends.forEach((end, i) => {
    if (i > 0 && end.y - ends[i - 1].y < 14) end.y = ends[i - 1].y + 14
  })

  function point(event: PointerEvent<SVGSVGElement>) {
    const bounds = event.currentTarget.getBoundingClientRect()
    const ratio = (event.clientX - bounds.left - MARGIN.left) / plotWidth
    setActive(Math.min(count - 1, Math.max(0, Math.round(ratio * (count - 1)))))
  }

  function keys(event: KeyboardEvent) {
    const move: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1 }
    if (event.key in move) {
      event.preventDefault()
      setActive((i) => Math.min(count - 1, Math.max(0, (i ?? 0) + move[event.key])))
    } else if (event.key === 'Escape') {
      setActive(null)
    }
  }

  const tipLeft = active === null ? 0 : x(active)
  const flip = tipLeft > width - 220

  return (
    <div>
      <ul className="mb-3 flex flex-wrap gap-x-5 gap-y-1 text-sm text-slate-600">
        {series.map((s) => (
          <li key={s.name} className="flex items-center gap-2">
            <span aria-hidden="true" className="h-0.5 w-4 rounded-full" style={{ backgroundColor: s.color }} />
            {s.name}
          </li>
        ))}
      </ul>

      <div ref={box} className="relative">
        <svg
          width={width}
          height={height}
          role="img"
          aria-label={`${description} Use the left and right arrow keys to read each day.`}
          tabIndex={0}
          onPointerMove={point}
          onPointerDown={point}
          onPointerLeave={() => setActive(null)}
          onFocus={() => setActive((i) => i ?? 0)}
          onBlur={() => setActive(null)}
          onKeyDown={keys}
          className="block max-w-full touch-pan-y rounded-md outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
        >
          {ticks.map((t) => (
            <g key={t}>
              <line
                x1={MARGIN.left}
                x2={width - MARGIN.right}
                y1={y(t)}
                y2={y(t)}
                className={t === 0 ? 'stroke-slate-400' : 'stroke-slate-200'}
                strokeWidth={1}
              />
              <text x={MARGIN.left - 8} y={y(t)} dy="0.32em" textAnchor="end" className="fill-slate-500 text-xs">
                {compact.format(t)}
              </text>
            </g>
          ))}

          {labels.map((label, i) =>
            i % every === 0 ? (
              <text key={i} x={x(i)} y={height - 8} textAnchor="middle" className="fill-slate-500 text-xs">
                {label}
              </text>
            ) : null,
          )}

          {active !== null && (
            <line
              x1={x(active)}
              x2={x(active)}
              y1={MARGIN.top}
              y2={height - MARGIN.bottom}
              className="stroke-slate-400"
              strokeWidth={1}
            />
          )}

          {series.map((s) => (
            <polyline
              key={s.name}
              points={s.values.map((v, i) => `${x(i)},${y(v)}`).join(' ')}
              fill="none"
              stroke={s.color}
              strokeWidth={2}
              strokeLinejoin="round"
              strokeLinecap="round"
            />
          ))}

          {ends.map((end) => (
            <g key={end.color}>
              <circle cx={x(count - 1) + 10} cy={end.y} r={4} fill={end.color} />
              <text x={x(count - 1) + 18} y={end.y} dy="0.32em" className="fill-slate-700 text-xs font-semibold">
                {compact.format(end.value)}
              </text>
            </g>
          ))}

          {active !== null &&
            series.map((s) => (
              <circle
                key={s.name}
                cx={x(active)}
                cy={y(s.values[active])}
                r={5}
                fill={s.color}
                className="stroke-white"
                strokeWidth={2}
              />
            ))}
        </svg>

        {active !== null && (
          <div
            role="status"
            className="pointer-events-none absolute top-2 z-10 min-w-48 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm shadow-lg"
            style={{ left: tipLeft, transform: flip ? 'translateX(calc(-100% - 12px))' : 'translateX(12px)' }}
          >
            <p className="font-semibold text-slate-900">{fullLabels[active]}</p>
            {series.map((s) => (
              <p key={s.name} className="mt-1 flex items-center justify-between gap-4">
                <span className="flex items-center gap-2 text-slate-600">
                  <span aria-hidden="true" className="h-0.5 w-3 rounded-full" style={{ backgroundColor: s.color }} />
                  {s.name}
                </span>
                <span className="font-semibold text-slate-900">{number.format(s.values[active])}</span>
              </p>
            ))}
            {details?.(active).map((line) => (
              <p key={line} className="mt-1 text-xs text-slate-500">
                {line}
              </p>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
