const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })

export function formatRupees(paise: number): string {
    return rupees.format(paise / 100)
}

export function formatCountdown(ms: number): string {
    const total = Math.max(0, Math.ceil(ms / 1000))
    const h = Math.floor(total / 3600)
    const m = Math.floor((total % 3600) / 60)
    const s = total % 60
    const two = (n: number) => String(n).padStart(2, '0')
    return h > 0 ? `${h}:${two(m)}:${two(s)}` : `${two(m)}:${two(s)}`
}