import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, hasRefreshToken, refreshSession, setTokens, whenSessionEnds, type Tokens } from '../api/client'
import { AuthContext, type AuthState, type User } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    const [loading, setLoading] = useState(hasRefreshToken())

    useEffect(() => {
        whenSessionEnds(() => setUser(null))
        if (!hasRefreshToken()) {
            return
        }
        // A page reload loses the in-memory access token: get a new one from the refresh token
        refreshSession()
            .then((ok) => (ok ? api<User>('/api/auth/me') : null))
            .then(setUser)
            .catch(() => setUser(null))
            .finally(() => setLoading(false))
    }, [])

    const login = useCallback(async (email: string, password: string) => {
        const tokens = await api<Tokens>('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password }),
        })
        setTokens(tokens)
        setUser(await api<User>('/api/auth/me'))
    }, [])

    const register = useCallback(
        async (email: string, password: string) => {
            await api<User>('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ email, password }),
            })
            await login(email, password)
        },
        [login],
    )

    const logout = useCallback(() => {
        setTokens(null)
        setUser(null)
    }, [])

    const value = useMemo<AuthState>(
        () => ({ user, loading, login, register, logout }),
        [user, loading, login, register, logout],
    )

    return <AuthContext value={value}>{children}</AuthContext>
}