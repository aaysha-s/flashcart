import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../api/client'

type Mode = 'shop' | 'help'

interface SourceRef {
    number: number
    fileName: string
    pageNumber: number | null
    heading: string | null
    snippet: string
    score: number
}

interface AskResponse {
    answer: string
    grounded: boolean
    sources: SourceRef[]
}

interface Message {
    from: 'you' | 'bot'
    text: string
    sources?: SourceRef[]
}

const INTRO: Record<Mode, string> = {
    shop: 'Tell me what you are looking for. I can find products and place an order for you.',
    help: "Ask about warranty, shipping, returns or refunds. I answer only from FlashCart's policy documents and show where each answer comes from.",
}

const SUGGESTIONS: Record<Mode, string[]> = {
    shop: ['Something to listen to music on the train', 'Find me a phone and order 1'],
    help: ['How long is the warranty on earbuds?', 'When will I get my refund if I paid by UPI?'],
}

function sourceLabel(source: SourceRef): string {
    if (source.pageNumber !== null) {
        return `${source.fileName}, page ${source.pageNumber}`
    }
    if (source.heading) {
        return `${source.fileName}, "${source.heading}"`
    }
    return source.fileName
}

/** Render **bold** from the model's Markdown, without injecting HTML */
function renderBold(text: string) {
    return text.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
        part.startsWith('**') && part.endsWith('**') && part.length > 4 ? <strong key={i}>{part.slice(2, -2)}</strong> : part,
    )
}

export function AssistantPanel() {
    const [open, setOpen] = useState(false)
    const [mode, setMode] = useState<Mode>('shop')
    const [threads, setThreads] = useState<Record<Mode, Message[]>>({ shop: [], help: [] })
    const [draft, setDraft] = useState('')
    const [busy, setBusy] = useState(false)
    const endRef = useRef<HTMLDivElement>(null)
    const messages = threads[mode]

    useEffect(() => {
        endRef.current?.scrollIntoView({ block: 'end' })
    }, [messages, busy, open])

    function append(thread: Mode, message: Message) {
        setThreads((current) => ({ ...current, [thread]: [...current[thread], message] }))
    }

    async function send(text: string) {
        const question = text.trim()
        if (!question || busy) {
            return
        }
        const thread = mode
        append(thread, { from: 'you', text: question })
        setDraft('')
        setBusy(true)
        try {
            if (thread === 'shop') {
                const reply = await api<{ reply: string }>('/api/orders/assistant', {
                    method: 'POST',
                    body: JSON.stringify({ message: question }),
                })
                append(thread, { from: 'bot', text: reply.reply })
            } else {
                const answer = await api<AskResponse>('/api/docs/ask', {
                    method: 'POST',
                    body: JSON.stringify({ question }),
                })
                append(thread, { from: 'bot', text: answer.answer, sources: answer.sources })
            }
        } catch {
            append(thread, { from: 'bot', text: 'Sorry, I could not answer that just now. Please try again.' })
        } finally {
            setBusy(false)
        }
    }

    function submit(event: FormEvent) {
        event.preventDefault()
        void send(draft)
    }

    if (!open) {
        return (
            <button className="assist-toggle" onClick={() => setOpen(true)}>
                Ask FlashCart
            </button>
        )
    }

    return (
        <section className="assist card" aria-label="FlashCart assistant">
            <header className="assist-head">
                <div className="tabs">
                    <button className={mode === 'shop' ? 'active' : ''} onClick={() => setMode('shop')}>
                        Shop
                    </button>
                    <button className={mode === 'help' ? 'active' : ''} onClick={() => setMode('help')}>
                        Help
                    </button>
                </div>
                <button className="link" onClick={() => setOpen(false)} aria-label="Close assistant">
                    Close
                </button>
            </header>

            <div className="assist-body">
                <p className="muted intro">{INTRO[mode]}</p>
                {messages.length === 0 && (
                    <div className="suggestions">
                        {SUGGESTIONS[mode].map((suggestion) => (
                            <button key={suggestion} onClick={() => void send(suggestion)}>
                                {suggestion}
                            </button>
                        ))}
                    </div>
                )}
                {messages.map((message, index) => (
                    <div key={index} className={`bubble ${message.from}`}>
                        <p>{renderBold(message.text)}</p>
                        {message.sources && message.sources.length > 0 && (
                            <ul className="sources">
                                {message.sources.map((source) => (
                                    <li key={source.number} title={source.snippet}>
                                        [{source.number}] {sourceLabel(source)}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                ))}
                {busy && <div className="bubble bot muted">Thinking…</div>}
                <div ref={endRef} />
            </div>

            <form className="assist-input" onSubmit={submit}>
                <input
                    id="assist-message"
                    value={draft}
                    onChange={(e) => setDraft(e.target.value)}
                    maxLength={500}
                    placeholder={mode === 'shop' ? 'What are you looking for?' : 'Ask about a policy'}
                    autoComplete="off"
                />
                <button type="submit" disabled={busy || !draft.trim()}>
                    Send
                </button>
            </form>
        </section>
    )
}