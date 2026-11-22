import { createContext } from 'react'

export interface User {
    id: number
    email: string
    role: 'CUSTOMER' | 'ADMIN'
    createdAt: string
}

export interface AuthState {
    user: User | null
    loading: boolean
    login: (email: string, password: string) => Promise<void>
    register: (email: string, password: string) => Promise<void>
    logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)