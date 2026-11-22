import { useState } from 'react'
import { ApiError } from '../api/client'
import { placeOrder, waitForFinal } from '../orders/orderApi'
import { STATUS_LABEL, type Order } from '../orders/types'
import { formatRupees } from '../products/format'
import type { Product } from '../products/types'
import { MAX_PER_ORDER, type Cart } from './useCart'

interface Props {
    products: Product[]
    cart: Cart
    stockOf: (product: Product) => number
}

export function CartPanel({ products, cart, stockOf }: Props) {
    const [orders, setOrders] = useState<Order[]>([])
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const byId = new Map(products.map((p) => [p.id, p]))
    const lines = Object.entries(cart.items)
        .map(([id, quantity]) => ({ product: byId.get(Number(id)), quantity }))
        .filter((line): line is { product: Product; quantity: number } => line.product !== undefined)
    const total = lines.reduce((sum, line) => sum + line.product.pricePaise * line.quantity, 0)

    async function checkout() {
        setBusy(true)
        setError(null)
        try {
            const placed: Order[] = []
            for (const line of lines) {
                placed.push(await placeOrder(line.product.id, line.quantity))
            }
            setOrders(placed)
            cart.clear()
            await Promise.all(
                placed.map((order) =>
                    waitForFinal(order.id, (updated) =>
                        setOrders((current) => current.map((o) => (o.id === updated.id ? updated : o))),
                    ),
                ),
            )
        } catch (e) {
            setError(e instanceof ApiError ? e.message : 'Checkout failed, please try again')
        } finally {
            setBusy(false)
        }
    }

    return (
        <aside className="card cart">
            <h2>Your cart</h2>

            {lines.length === 0 ? (
                <p className="muted">Empty. Add something before it sells out.</p>
            ) : (
                <>
                    <ul className="lines">
                        {lines.map(({ product, quantity }) => (
                            <li key={product.id}>
                                <span className="name">{product.name}</span>
                                <span className="qty">
                  <button type="button" onClick={() => cart.setQuantity(product.id, quantity - 1)} aria-label="One less">−</button>
                                    {quantity}
                                    <button
                                        type="button"
                                        onClick={() => cart.setQuantity(product.id, quantity + 1)}
                                        disabled={quantity >= Math.min(MAX_PER_ORDER, stockOf(product))}
                                        aria-label="One more"
                                    >
                    +
                  </button>
                </span>
                                <span>{formatRupees(product.pricePaise * quantity)}</span>
                            </li>
                        ))}
                    </ul>
                    <p className="total">
                        Total <strong>{formatRupees(total)}</strong>
                    </p>
                    <button className="buy" onClick={checkout} disabled={busy}>
                        {busy ? 'Placing order…' : 'Checkout'}
                    </button>
                </>
            )}

            {error && <p className="error">{error}</p>}

            {orders.length > 0 && (
                <div className="tracking">
                    <h3>Order status</h3>
                    {orders.map((order) => (
                        <div key={order.id} className={`order-status ${order.status.toLowerCase()}`}>
              <span>
                #{order.id} · {byId.get(order.productId)?.name ?? `Product ${order.productId}`} × {order.quantity}
              </span>
                            <strong>{STATUS_LABEL[order.status]}</strong>
                            {order.failureReason && <small>{order.failureReason}</small>}
                        </div>
                    ))}
                </div>
            )}
        </aside>
    )
}