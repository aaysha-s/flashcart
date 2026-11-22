export type OrderStatus = 'PENDING' | 'AWAITING_PAYMENT' | 'CONFIRMED' | 'CANCELLED'

export interface Order {
    id: number
    productId: number
    quantity: number
    status: OrderStatus
    totalPaise: number | null
    failureReason: string | null
    createdAt: string
    updatedAt: string
}

export function isFinal(status: OrderStatus): boolean {
    return status === 'CONFIRMED' || status === 'CANCELLED'
}

export const STATUS_LABEL: Record<OrderStatus, string> = {
    PENDING: 'Reserving stock…',
    AWAITING_PAYMENT: 'Processing payment…',
    CONFIRMED: 'Confirmed ✓',
    CANCELLED: 'Cancelled',
}