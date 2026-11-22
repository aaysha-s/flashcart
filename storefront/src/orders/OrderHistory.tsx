import { api } from '../api/client'
import { formatRupees } from '../products/format'
import type { Page, Product } from '../products/types'
import { isFinal, STATUS_LABEL, type Order } from './types'
import { useEffect, useState } from 'react'

const PAGE_SIZE = 20
const dateTime = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' })

export function OrderHistory() {
    const [orders, setOrders] = useState<Order[] | null>(null)
    const [names, setNames] = useState<Record<number, string>>({})
    const [pages, setPages] = useState(1)
    const [hasMore, setHasMore] = useState(false)

    const [error, setError] = useState<string | null>(null)
    const [refreshTick, setRefreshTick] = useState(0)

    useEffect(() => {
        let stale = false
        // Re-read every loaded page in one request, so a refresh never shows a gap
        api<Page<Order>>(`/api/orders?size=${PAGE_SIZE * pages}&sort=createdAt,desc`)
            .then((result) => {
                if (stale) {
                    return
                }
                setOrders(result.content)
                setHasMore(result.page.totalElements > result.content.length)
                setError(null)
            })
            .catch((e: Error) => {
                if (!stale) {
                    setError(e.message)
                }
            })
        return () => {
            stale = true // a newer request replaced this one: ignore its answer
        }
    }, [pages, refreshTick])

    useEffect(() => {
        api<Page<Product>>('/api/products?size=200')
            .then((page) => setNames(Object.fromEntries(page.content.map((p) => [p.id, p.name]))))
            .catch(() => setNames({}))
    }, [])

    // While any order is still in the saga, check again every 3 seconds
    const anyOpen = orders?.some((order) => !isFinal(order.status)) ?? false
    useEffect(() => {
        if (!anyOpen) {
            return
        }
        const id = window.setTimeout(() => setRefreshTick((tick) => tick + 1), 3000)
        return () => window.clearTimeout(id)
    }, [anyOpen, orders])

    if (error) {
        return <p className="error">{error}</p>
    }
    if (!orders) {
        return <p className="muted">Loading your orders…</p>
    }
    if (orders.length === 0) {
        return <p className="muted">No orders yet. Your purchases will show up here.</p>
    }

    return (
        <section className="history">
            <h2>My orders</h2>
            <div className="table-scroll">
                <table>
                    <thead>
                    <tr>
                        <th>Order</th>
                        <th>Product</th>
                        <th className="num">Qty</th>
                        <th className="num">Total</th>
                        <th>Status</th>
                        <th>Placed</th>
                    </tr>
                    </thead>
                    <tbody>
                    {orders.map((order) => (
                        <tr key={order.id}>
                            <td>#{order.id}</td>
                            <td>{names[order.productId] ?? `Product ${order.productId}`}</td>
                            <td className="num">{order.quantity}</td>
                            <td className="num">{order.totalPaise === null ? '—' : formatRupees(order.totalPaise)}</td>
                            <td>
                                <span className={`pill ${order.status.toLowerCase()}`}>{STATUS_LABEL[order.status]}</span>
                                {order.failureReason && <small className="reason">{order.failureReason}</small>}
                            </td>
                            <td className="muted">{dateTime.format(new Date(order.createdAt))}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
            {hasMore && (
                <button className="more" onClick={() => setPages((n) => n + 1)}>
                    Show older orders
                </button>
            )}
        </section>
    )
}