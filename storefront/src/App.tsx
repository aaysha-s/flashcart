import { useState } from 'react'
import { AssistantPanel } from './assist/AssistantPanel'
import { LoginForm } from './auth/LoginForm'
import { useAuth } from './auth/useAuth'
import { OrderHistory } from './orders/OrderHistory'
import { ProductList } from './products/ProductList'
import { AdminDashboard } from './admin/AdminDashboard'

type View = 'shop' | 'orders' | 'dashboard'
export default function App() {
    const { user, loading, logout } = useAuth()
    const [view, setView] = useState<View>('shop')

    if (loading) {
        return <p className="center muted">Loading…</p>
    }
    if (!user) {
        return (
            <main className="center">
                <LoginForm />
            </main>
        )
    }
    return (
        <>
            <header className="topbar">
                <strong>FlashCart</strong>
                <nav className="tabs">
                    <button className={view === 'shop' ? 'active' : ''} onClick={() => setView('shop')}>
                        Shop
                    </button>
                    <button className={view === 'orders' ? 'active' : ''} onClick={() => setView('orders')}>
                        My orders
                    </button>
                    {user.role === 'ADMIN' && (
                        <button className={view === 'dashboard' ? 'active' : ''} onClick={() => setView('dashboard')}>
                            Dashboard
                        </button>
                    )}
                </nav>
                <span className="muted who">
          {user.email} · {user.role}
        </span>
                <button className="link" onClick={logout}>
                    Log out
                </button>
            </header>
            <main className="page">
                {/* The shop stays mounted, so the cart and the live connection survive a tab switch */}
                <div hidden={view !== 'shop'}>
                    <ProductList />
                </div>
                {view === 'orders' && <OrderHistory />}
                {view === 'dashboard' && user.role === 'ADMIN' && <AdminDashboard />}
            </main>
            <AssistantPanel />
        </>
    )
}