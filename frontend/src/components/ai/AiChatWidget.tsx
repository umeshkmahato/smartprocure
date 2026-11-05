import { useEffect, useRef, useState } from 'react'
import { Bot, MessageSquare, Minus, Send, Trash2, X } from 'lucide-react'
import { useAIChat } from '@/hooks/useAI'
import { cn } from '@/lib/utils'

/**
 * Floating AI assistant chat widget.
 *
 * Mounts in AppLayout so it is available on every protected page.
 * Renders as a bottom-right FAB; clicking it opens a compact chat
 * window with full conversation history.
 */
export default function AiChatWidget() {
  const [open,      setOpen]      = useState(false)
  const [minimised, setMinimised] = useState(false)
  const [input,     setInput]     = useState('')

  const { messages, loading, error, send, clear } = useAIChat()

  const bottomRef  = useRef<HTMLDivElement>(null)
  const inputRef   = useRef<HTMLTextAreaElement>(null)

  // Scroll to latest message whenever messages change
  useEffect(() => {
    if (open && !minimised) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages, open, minimised])

  // Focus input when window opens
  useEffect(() => {
    if (open && !minimised) {
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [open, minimised])

  const handleSend = () => {
    if (!input.trim() || loading) return
    send(input)
    setInput('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const unreadDot = !open && messages.length > 0

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-3">
      {/* ── Chat window ──────────────────────────────────────────────────────── */}
      {open && (
        <div
          className={cn(
            'w-[360px] rounded-2xl border border-slate-200 bg-white shadow-2xl flex flex-col overflow-hidden transition-all duration-200',
            minimised ? 'h-[48px]' : 'h-[520px]',
          )}
        >
          {/* Title bar */}
          <div className="flex items-center gap-2.5 px-4 py-3 bg-slate-900 text-white shrink-0">
            <Bot size={16} className="text-blue-400 shrink-0" />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold leading-none">SmartProcure Assistant</p>
              {!minimised && (
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Powered by Ollama · llama3.2
                </p>
              )}
            </div>
            <div className="flex items-center gap-1 shrink-0">
              {!minimised && messages.length > 0 && (
                <button
                  onClick={clear}
                  title="Clear conversation"
                  className="p-1 rounded hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
                >
                  <Trash2 size={13} />
                </button>
              )}
              <button
                onClick={() => setMinimised((m) => !m)}
                title={minimised ? 'Expand' : 'Minimise'}
                className="p-1 rounded hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
              >
                <Minus size={13} />
              </button>
              <button
                onClick={() => setOpen(false)}
                title="Close"
                className="p-1 rounded hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
              >
                <X size={13} />
              </button>
            </div>
          </div>

          {/* Body — hidden when minimised */}
          {!minimised && (
            <>
              {/* Messages */}
              <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
                {messages.length === 0 ? (
                  <WelcomeMessage />
                ) : (
                  messages.map((msg, i) => (
                    <MessageBubble key={i} role={msg.role} content={msg.content} />
                  ))
                )}

                {/* Typing indicator */}
                {loading && (
                  <div className="flex items-center gap-2">
                    <BotAvatar />
                    <div className="flex gap-1 px-3 py-2 rounded-2xl rounded-tl-none bg-slate-100">
                      {[0, 1, 2].map((i) => (
                        <span
                          key={i}
                          className="w-1.5 h-1.5 rounded-full bg-slate-400 animate-bounce"
                          style={{ animationDelay: `${i * 150}ms` }}
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Error inline */}
                {error && !loading && (
                  <div className="text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                    {error}
                  </div>
                )}

                <div ref={bottomRef} />
              </div>

              {/* Input */}
              <div className="shrink-0 border-t px-3 py-2.5 flex items-end gap-2 bg-slate-50">
                <textarea
                  ref={inputRef}
                  rows={1}
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask anything about procurement…"
                  disabled={loading}
                  data-ai-input
                  className="flex-1 resize-none rounded-xl border bg-white px-3 py-2 text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60 max-h-28 overflow-y-auto"
                  style={{ lineHeight: '1.4' }}
                />
                <button
                  onClick={handleSend}
                  disabled={loading || !input.trim()}
                  className="shrink-0 w-9 h-9 rounded-xl bg-blue-600 text-white flex items-center justify-center hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                  title="Send (Enter)"
                >
                  <Send size={15} />
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── FAB toggle ───────────────────────────────────────────────────────── */}
      <button
        onClick={() => { setOpen((o) => !o); setMinimised(false) }}
        className="relative w-14 h-14 rounded-full bg-slate-900 text-white shadow-xl flex items-center justify-center hover:bg-slate-800 active:scale-95 transition-all duration-150"
        title="SmartProcure AI Assistant"
        aria-label="Toggle AI chat"
      >
        {open ? <X size={22} /> : <MessageSquare size={22} />}
        {unreadDot && (
          <span className="absolute top-0.5 right-0.5 w-3 h-3 rounded-full bg-blue-500 border-2 border-white" />
        )}
      </button>
    </div>
  )
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function BotAvatar() {
  return (
    <div className="shrink-0 w-6 h-6 rounded-full bg-slate-800 flex items-center justify-center">
      <Bot size={12} className="text-blue-400" />
    </div>
  )
}

function MessageBubble({ role, content }: { role: 'user' | 'assistant'; content: string }) {
  const isUser = role === 'user'
  return (
    <div className={cn('flex items-end gap-2', isUser ? 'flex-row-reverse' : 'flex-row')}>
      {!isUser && <BotAvatar />}
      <div
        className={cn(
          'max-w-[82%] rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed whitespace-pre-wrap break-words',
          isUser
            ? 'bg-blue-600 text-white rounded-br-none'
            : 'bg-slate-100 text-slate-800 rounded-bl-none',
        )}
      >
        {content}
      </div>
    </div>
  )
}

function WelcomeMessage() {
  const prompts = [
    'How do I evaluate a new vendor?',
    'What is a 3-way match for invoices?',
    'Find vendors for IT hardware in Asia',
    'Explain multi-level approval workflows',
  ]
  return (
    <div className="space-y-3 py-2">
      <div className="flex items-end gap-2">
        <BotAvatar />
        <div className="bg-slate-100 text-slate-800 rounded-2xl rounded-bl-none px-3.5 py-2.5 text-sm leading-relaxed max-w-[82%]">
          👋 Hi! I'm your SmartProcure AI assistant. Ask me anything about
          procurement, vendors, purchase orders, or approvals.
        </div>
      </div>
      <div className="pl-8 space-y-1.5">
        <p className="text-[11px] text-slate-400 font-medium uppercase tracking-wide pl-0.5">
          Try asking…
        </p>
        {prompts.map((p) => (
          <SuggestionChip key={p} text={p} />
        ))}
      </div>
    </div>
  )
}

function SuggestionChip({ text }: { text: string }) {
  // We just display suggestions — clicking them is a nice-to-have handled
  // by the parent via a copy of the hook; kept simple here.
  return (
    <button
      className="block w-full text-left rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600 hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 transition-colors"
      onClick={() => {
        const ta = document.querySelector<HTMLTextAreaElement>('[data-ai-input]')
        if (ta) {
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
            window.HTMLTextAreaElement.prototype,
            'value',
          )?.set
          nativeInputValueSetter?.call(ta, text)
          ta.dispatchEvent(new Event('input', { bubbles: true }))
          ta.focus()
        }
      }}
    >
      {text}
    </button>
  )
}
