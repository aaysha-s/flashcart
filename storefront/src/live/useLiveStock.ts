import { useEffect, useState } from 'react'

export interface LiveStock {
    stock: number
    changedAtEpochMs: number
}

export type LiveStatus = 'connecting' | 'live' | 'offline'

interface StockMessage {
    type: string
    productId: number
    stock: number
    changedAtEpochMs: number
}

/** Latest stock per product, pushed by live-service. Reconnects on its own. */
export function useLiveStock() {
    const [stocks, setStocks] = useState<Record<number, LiveStock>>({})
    const [status, setStatus] = useState<LiveStatus>('connecting')

    useEffect(() => {
        let socket: WebSocket | null = null
        let retryTimer: number | undefined
        let attempt = 0
        let stopped = false

        const connect = () => {
            const scheme = location.protocol === 'https:' ? 'wss' : 'ws'
            socket = new WebSocket(`${scheme}://${location.host}/ws/stock`)

            socket.onopen = () => {
                attempt = 0
                setStatus('live')
            }

            socket.onmessage = (event) => {
                const message = JSON.parse(event.data as string) as StockMessage
                if (message.type !== 'stock') {
                    return
                }
                setStocks((current) => {
                    const known = current[message.productId]
                    // Never let an older update overwrite a newer one
                    if (known && known.changedAtEpochMs >= message.changedAtEpochMs) {
                        return current
                    }
                    return {
                        ...current,
                        [message.productId]: { stock: message.stock, changedAtEpochMs: message.changedAtEpochMs },
                    }
                })
            }

            socket.onclose = () => {
                if (stopped) {
                    return
                }
                setStatus('offline')
                // Exponential backoff with jitter: 1s, 2s, 4s … capped at 30s
                const delay = Math.min(30_000, 1000 * 2 ** attempt) + Math.random() * 500
                attempt++
                retryTimer = window.setTimeout(connect, delay)
            }
        }

        connect()
        return () => {
            stopped = true
            window.clearTimeout(retryTimer)
            socket?.close()
        }
    }, [])

    return { stocks, status }
}