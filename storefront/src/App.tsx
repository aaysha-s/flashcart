import { LoginForm } from './auth/LoginForm'
import { useAuth } from './auth/useAuth'
import { ProductList } from './products/ProductList'

export default function App() {
  const { user, loading, logout } = useAuth()

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
          <span className="muted">
          {user.email} · {user.role}
        </span>
          <button className="link" onClick={logout}>
            Log out
          </button>
        </header>
        <main className="page">
          <ProductList/>
        </main>
      </>
  )
}