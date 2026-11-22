import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { formatCountdown, formatRupees } from './format'
import type { Page, Product } from './types'
import { useNow } from './useNow'
import { useLiveStock, type LiveStock } from '../live/useLiveStock'

export function ProductList() {
    const { user } = useAuth()
    const [products, setProducts] = useState<Product[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const now = useNow()
    const { stocks, status } = useLiveStock()

    const load = useCallback(() => {
        api<Page<Product>>('/api/products?size=50&sort=id,desc')
            .then((page) => setProducts(page.content))
            .catch((e: Error) => setError(e.message))
    }, [])

    useEffect(() => {
        load()
    }, [load])

    if (error) {
        return <p className="error">Could not load products: {error}</p>
    }
    if (!products) {
        return <p className="muted">Loading products…</p>
    }

    return (
        <>
            {user?.role === 'ADMIN' && <NewSaleForm onCreated={load} />}
            <p className={`live-status ${status}`}>
                ● {status === 'live' ? 'Live stock' : status === 'connecting' ? 'Connecting…' : 'Offline, reconnecting…'}
            </p>
            <div className="grid">
                {products.map((product) => (
                    <ProductCard key={product.id} product={product} live={stocks[product.id]} now={now} />
                ))}
            </div>
        </>
    )
}

function ProductCard({ product, live, now }: { product: Product; live?: LiveStock; now: number }) {
    const stock = live?.stock ?? product.stock
    const startsAt = product.saleStartsAt ? Date.parse(product.saleStartsAt) : null
    const upcoming = startsAt !== null && startsAt > now
    const soldOut = stock === 0

    return (
        <article className="card product">
            <h3>{product.name}</h3>
            {product.description && <p className="muted desc">{product.description}</p>}
            <p className="price">{formatRupees(product.pricePaise)}</p>
            {upcoming ? (
                <p className="badge upcoming">
                    Sale starts in <strong>{formatCountdown(startsAt - now)}</strong>
                </p>
            ) : soldOut ? (
                <p key={`out-${live?.changedAtEpochMs}`} className="badge soldout flash">Sold out</p>
            ) : (
                <p key={`left-${live?.changedAtEpochMs}`} className="badge live flash">{stock} left</p>
            )}
            <button className="buy" disabled={upcoming || soldOut} title="Checkout arrives in Step 14.5">
                {upcoming ? 'Not yet' : soldOut ? 'Sold out' : 'Buy now'}
            </button>
        </article>
    )
}

function NewSaleForm({ onCreated }: { onCreated: () => void }) {
    const [name, setName] = useState('')
    const [priceRupees, setPriceRupees] = useState('999')
    const [stock, setStock] = useState('50')
    const [startsInMinutes, setStartsInMinutes] = useState('2')
    const [error, setError] = useState<string | null>(null)

    async function submit(event: FormEvent) {
        event.preventDefault()
        setError(null)
        try {
            await api<Product>('/api/products', {
                method: 'POST',
                body: JSON.stringify({
                    name,
                    description: 'Flash-sale item',
                    pricePaise: Math.round(Number(priceRupees) * 100),
                    stock: Number(stock),
                    saleStartsAt: new Date(Date.now() + Number(startsInMinutes) * 60_000).toISOString(),
                }),
            })
            setName('')
            onCreated()
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Could not create the sale')
        }
    }

    return (
        <form className="card admin-form" onSubmit={submit}>
            <strong>New flash sale</strong>
            <input placeholder="Product name" value={name} onChange={(e) => setName(e.target.value)} required />
            <label>₹ <input type="number" min="1" step="0.01" value={priceRupees} onChange={(e) => setPriceRupees(e.target.value)} /></label>
            <label>Stock <input type="number" min="0" value={stock} onChange={(e) => setStock(e.target.value)} /></label>
            <label>Starts in (min) <input type="number" min="0" value={startsInMinutes} onChange={(e) => setStartsInMinutes(e.target.value)} /></label>
            <button type="submit">Create</button>
            {error && <span className="error">{error}</span>}
        </form>
    )
}