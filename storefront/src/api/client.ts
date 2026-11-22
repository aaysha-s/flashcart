export interface Tokens {
    accessToken: string
    refreshToken: string
    tokenType: string
    expiresIn: number
}

export class ApiError extends Error {
    readonly status: number

    constructor(status: number, message: string) {
        super(message)
        this.status = status
    }
}

const REFRESH_KEY = 'flashcart.refreshToken'

let accessToken: string | null = null
let refreshToken: string | null = sessionStorage.getItem(REFRESH_KEY)
let refreshing: Promise<boolean> | null = null
let onSessionEnded: () => void = () => {}

export function setTokens(tokens: Tokens | null): void {
    accessToken = tokens?.accessToken ?? null
    refreshToken = tokens?.refreshToken ?? null
    if (refreshToken) {
        sessionStorage.setItem(REFRESH_KEY, refreshToken)
    } else {
        sessionStorage.removeItem(REFRESH_KEY)
    }
}

export function hasRefreshToken(): boolean {
    return refreshToken !== null
}

export function whenSessionEnds(callback: () => void): void {
    onSessionEnded = callback
}

/** Swap the refresh token for a new pair. Concurrent callers share ONE request. */
export function refreshSession(): Promise<boolean> {
    if (!refreshToken) {
        return Promise.resolve(false)
    }
    refreshing ??= (async () => {
        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken }),
            })
            if (!res.ok) {
                setTokens(null)
                onSessionEnded()
                return false
            }
            setTokens((await res.json()) as Tokens)
            return true
        } finally {
            refreshing = null
        }
    })()
    return refreshing
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
    const send = () => {
        const headers = new Headers(init.headers)
        if (accessToken) {
            headers.set('Authorization', `Bearer ${accessToken}`)
        }
        if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json')
        }
        return fetch(path, { ...init, headers })
    }

    const hadToken = accessToken !== null
    let res = await send()
    if (res.status === 401 && hadToken && (await refreshSession())) {
        res = await send() // retry once with the fresh access token
    }
    if (!res.ok) {
        throw await toApiError(res)
    }
    if (res.status === 204) {
        return undefined as T
    }
    return (await res.json()) as T
}

async function toApiError(res: Response): Promise<ApiError> {
    try {
        const problem = (await res.json()) as { detail?: string; title?: string }
        return new ApiError(res.status, problem.detail ?? problem.title ?? res.statusText)
    } catch {
        return new ApiError(res.status, res.statusText || `Request failed (${res.status})`)
    }
}