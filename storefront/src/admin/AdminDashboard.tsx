import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { formatRupees } from '../products/format'
import type { Page, Product } from '../products/types'

interface Metrics {
    generatedAtEpochMs: number
    windowSeconds: number
    perSecond: Record<string, number[]>
    totals: Record<string, number>
    revenuePaise: number
    soldOutAtEpochMs: Record<string, number>
}

const POLL_MS = 2000
const clock = new Intl.DateTimeFormat('en-IN', { timeStyle: 'medium' })
const count = (n: number | undefined) => (n ?? 0).toLocaleString('en-IN')

function sum(values: number[]): number {
    return values.reduce((a, b) => a + b, 0)
}

function formatDuration(ms: number): string {
    const s = Math.max(0, Math.round(ms / 1000))
    if (s < 60) {
        return `${s} s`
    }
    const m = Math.floor(s / 60)
    if (m < 60) {
        return `${m} min ${s % 60} s`
    }
    return `${Math.floor(m / 60)} h ${m % 60} min`
}

export function AdminDashboard() {
    const [metrics, setMetrics] = useState<Metrics | null>(null)
    const [products, setProducts] = useState<Record<number, Product>>({})
    const [error, setError] = useState<string | null>(null)
    const [tick, setTick] = useState(0)

    useEffect(() => {
        let stale = false
        api<Metrics>('/api/live/admin/metrics')
            .then((m) => {
                if (!stale) {
                    setMetrics(m)
                    setError(null)
                }
            })
            .catch((e: Error) => {
                if (!stale) {
                    setError(e.message)
                }
            })
        const id = window.setTimeout(() => setTick((t) => t + 1), POLL_MS)
        return () => {
            stale = true
            window.clearTimeout(id)
        }
    }, [tick])

    // Reload product names whenever another product sells out
    const soldOutCount = metrics ? Object.keys(metrics.soldOutAtEpochMs).length : 0
    useEffect(() => {
        api<Page<Product>>('/api/products?size=200')
            .then((page) => setProducts(Object.fromEntries(page.content.map((p) => [p.id, p]))))
            .catch(() => setProducts({}))
    }, [soldOutCount])

    if (error) {
        return <p className="error">Could not load metrics: {error}</p>
    }
    if (!metrics) {
        return <p className="muted">Loading metrics…</p>
    }

    const placed = metrics.perSecond.placed ?? []
    const failed = metrics.perSecond.payment_failed ?? []
    const lastFive = placed.slice(-6, -1) // the 5 most recent complete seconds
    const ordersPerSecond = lastFive.length > 0 ? sum(lastFive) / lastFive.length : 0
    const totals = metrics.totals
    const paymentsDone = (totals.confirmed ?? 0) + (totals.payment_failed ?? 0)
    const failRate = paymentsDone > 0 ? (totals.payment_failed ?? 0) / paymentsDone : 0
    const soldOut = Object.entries(metrics.soldOutAtEpochMs)
        .map(([id, at]) => ({ id: Number(id), at }))
        .sort((a, b) => b.at - a.at)

    return (
        <section className="dashboard">
            <h2>Live sales</h2>

            <div className="tiles">
                <Tile label="Orders / sec" value={ordersPerSecond.toFixed(1)} hint="average of the last 5 s" />
                <Tile label="Orders placed" value={count(totals.placed)} />
                <Tile label="Confirmed" value={count(totals.confirmed)} />
                <Tile
                    label="Failed payments"
                    value={count(totals.payment_failed)}
                    hint={`${(failRate * 100).toFixed(1)}% of payments`}
                    bad={(totals.payment_failed ?? 0) > 0}
                />
                <Tile label="Sold-out rejections" value={count(totals.rejected)} />
                <Tile label="Revenue" value={formatRupees(metrics.revenuePaise)} />
            </div>

            <div className="card chart-card">
                <h3>Orders per second, last {metrics.windowSeconds} s</h3>
                <OrdersChart placed={placed} failed={failed} />
                <p className="legend">
                    <span className="key placed" /> Orders placed <span className="key failed" /> Failed payments
                </p>
            </div>

            <div className="card">
                <h3>Sold out</h3>
                {soldOut.length === 0 ? (
                    <p className="muted">Nothing has sold out yet.</p>
                ) : (
                    <div className="table-scroll">
                        <table className="soldout-table">
                            <thead>
                            <tr>
                                <th>Product</th>
                                <th>Sale started</th>
                                <th>Sold out</th>
                                <th className="num">Time to sell out</th>
                            </tr>
                            </thead>
                            <tbody>
                            {soldOut.map(({ id, at }) => {
                                const product = products[id]
                                const started = product ? Date.parse(product.saleStartsAt ?? product.createdAt) : null
                                return (
                                    <tr key={id}>
                                        <td>{product?.name ?? `Product ${id}`}</td>
                                        <td>{started === null ? '—' : clock.format(started)}</td>
                                        <td>{clock.format(at)}</td>
                                        <td className="num">{started === null ? '—' : formatDuration(at - started)}</td>
                                    </tr>
                                )
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <p className="muted small">
                Updates every 2 s · last update {clock.format(metrics.generatedAtEpochMs)}
            </p>
        </section>
    )
}

function Tile({ label, value, hint, bad }: { label: string; value: string; hint?: string; bad?: boolean }) {
    return (
        <div className={`card tile${bad ? ' bad' : ''}`}>
            <span className="tile-label">{label}</span>
            <strong>{value}</strong>
            {hint && <small className="muted">{hint}</small>}
        </div>
    )
}

function OrdersChart({ placed, failed }: { placed: number[]; failed: number[] }) {
    const width = 600
    const height = 170
    const left = 30
    const top = 10
    const bottom = 22
    const plotWidth = width - left
    const plotHeight = height - top - bottom
    const max = Math.max(1, ...placed)
    const barWidth = plotWidth / Math.max(1, placed.length)
    const y = (v: number) => top + plotHeight - (v / max) * plotHeight

    return (
        <div className="chart-scroll">
            <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Orders per second over the last 60 seconds">
                <line x1={left} x2={width} y1={y(max)} y2={y(max)} className="grid" />
                <line x1={left} x2={width} y1={y(0)} y2={y(0)} className="axis" />
                <text x={left - 6} y={y(max) + 4} className="tick" textAnchor="end">{max}</text>
                <text x={left - 6} y={y(0) + 4} className="tick" textAnchor="end">0</text>
                {placed.map((v, i) =>
                    v > 0 ? (
                        <rect key={`p${i}`} x={left + i * barWidth + 1} y={y(v)} width={Math.max(1, barWidth - 2)} height={y(0) - y(v)} className="bar placed" />
                    ) : null,
                )}
                {failed.map((v, i) =>
                    v > 0 ? (
                        <rect key={`f${i}`} x={left + i * barWidth + barWidth * 0.25} y={y(v)} width={barWidth * 0.5} height={y(0) - y(v)} className="bar failed" />
                    ) : null,
                )}
                <text x={left} y={height - 6} className="tick">−60 s</text>
                <text x={width} y={height - 6} className="tick" textAnchor="end">now</text>
            </svg>
        </div>
    )
}