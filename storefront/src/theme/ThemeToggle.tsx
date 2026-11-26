import { useState } from 'react'

type Theme = 'light' | 'dark'
const KEY = 'flashcart.theme'

function currentTheme(): Theme {
    const chosen = document.documentElement.dataset.theme
    if (chosen === 'light' || chosen === 'dark') return chosen
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** Switches between light and dark. Until someone chooses, the site follows the device setting. */
export function ThemeToggle({ className = '' }: { className?: string }) {
    const [theme, setTheme] = useState<Theme>(currentTheme)

    function toggle() {
        const next: Theme = theme === 'dark' ? 'light' : 'dark'
        document.documentElement.dataset.theme = next
        try {
            localStorage.setItem(KEY, next)
        } catch {
            // Storage can be unavailable (private mode); the choice then lasts for this visit only
        }
        setTheme(next)
    }

    const label = theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'
    return (
        <button type="button" className={`theme-toggle ${className}`} onClick={toggle} aria-label={label} title={label}>
            <span aria-hidden="true">{theme === 'dark' ? '☀️' : '🌙'}</span>
        </button>
    )
}
