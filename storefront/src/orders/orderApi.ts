import { api } from '../api/client'
import { isFinal, type Order } from './types'

export function placeOrder(productId: number, quantity: number): Promise<Order> {
    return api<Order>('/api/orders', {
        method: 'POST',
        body: JSON.stringify({ productId, quantity }),
    })
}

/** Poll until the saga finishes, backing off from 400 ms to 2 s, for at most 30 s. */
export async function waitForFinal(id: number, onUpdate: (order: Order) => void): Promise<Order> {
    const deadline = Date.now() + 30_000
    let delay = 400
    for (;;) {
        const order = await api<Order>(`/api/orders/${id}`)
        onUpdate(order)
        if (isFinal(order.status) || Date.now() > deadline) {
            return order
        }
        await new Promise((resolve) => setTimeout(resolve, delay))
        delay = Math.min(delay * 1.5, 2000)
    }
}