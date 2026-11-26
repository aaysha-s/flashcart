import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { useAuth } from './useAuth'

export function LoginForm() {
    const { login, register } = useAuth()
    const [mode, setMode] = useState<'login' | 'register'>('login')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState<string | null>(null)
    const [busy, setBusy] = useState(false)

    async function submit(event: FormEvent) {
        event.preventDefault()
        setError(null)
        setBusy(true)
        try {
            await (mode === 'login' ? login(email, password) : register(email, password))
        } catch (e) {
            if (e instanceof ApiError && e.status === 401) {
                setError('Wrong email or password.')
            } else if (e instanceof ApiError && e.status === 409) {
                setError('An account with this email already exists.')
            } else {
                setError(e instanceof Error ? e.message : 'Something went wrong.')
            }
        } finally {
            setBusy(false)
        }
    }

    return (
        <form className="card auth" onSubmit={submit}>
            <h1 className="brand"><span className="bolt" aria-hidden="true">⚡</span>FlashCart</h1>
            <p className="muted">{mode === 'login' ? 'Log in to join the sale' : 'Create your account'}</p>
            <label>
                Email
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
            </label>
            <label>
                Password
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    minLength={8}
                    autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                />
            </label>
            {error && <p className="error" role="alert">{error}</p>}
            <button type="submit" disabled={busy}>
                {busy ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
            </button>
            <button type="button" className="link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
                {mode === 'login' ? 'New here? Create an account' : 'Already have an account? Log in'}
            </button>
        </form>
    )
}