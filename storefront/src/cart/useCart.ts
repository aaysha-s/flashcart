import { useCallback, useState } from 'react'

export const MAX_PER_ORDER = 5

/** productId -> quantity */
export function useCart() {
    const [items, setItems] = useState<Record<number, number>>({})

    const add = useCallback((productId: number, available: number) => {
        setItems((current) => {
            const next = Math.min((current[productId] ?? 0) + 1, MAX_PER_ORDER, available)
            return next > 0 ? { ...current, [productId]: next } : current
        })
    }, [])

    const setQuantity = useCallback((productId: number, quantity: number) => {
        setItems((current) => {
            const copy = { ...current }
            if (quantity <= 0) {
                delete copy[productId]
            } else {
                copy[productId] = Math.min(quantity, MAX_PER_ORDER)
            }
            return copy
        })
    }, [])

    const clear = useCallback(() => setItems({}), [])

    return { items, add, setQuantity, clear }
}

export type Cart = ReturnType<typeof useCart>