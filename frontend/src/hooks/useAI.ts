import { useState, useCallback } from 'react'
import { aiApi } from '@/api/ai'
import type { AiChatMessage, AiSimilarVendor } from '@/types'

// ── Chat ──────────────────────────────────────────────────────────────────────

export function useAIChat() {
  const [messages, setMessages] = useState<AiChatMessage[]>([])
  const [loading,  setLoading]  = useState(false)
  const [error,    setError]    = useState<string | null>(null)

  const send = useCallback(async (text: string) => {
    const trimmed = text.trim()
    if (!trimmed) return

    const userMsg: AiChatMessage = { role: 'user', content: trimmed, timestamp: new Date() }
    setMessages((prev) => [...prev, userMsg])
    setLoading(true)
    setError(null)

    try {
      const reply = await aiApi.chat(trimmed)
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: reply, timestamp: new Date() },
      ])
    } catch {
      setError('The AI assistant is temporarily unavailable. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [])

  const clear = useCallback(() => {
    setMessages([])
    setError(null)
  }, [])

  return { messages, loading, error, send, clear }
}

// ── Vendor Recommendations ────────────────────────────────────────────────────

export function useAIRecommend() {
  const [result,  setResult]  = useState<string | null>(null)
  const [similar, setSimilar] = useState<AiSimilarVendor[]>([])
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState<string | null>(null)

  const recommend = useCallback(async (query: string, filterStatus = 'APPROVED') => {
    const trimmed = query.trim()
    if (!trimmed) return

    setLoading(true)
    setError(null)
    setResult(null)
    setSimilar([])

    try {
      // Run RAG recommendation + vector similarity in parallel
      const [rec, sim] = await Promise.all([
        aiApi.recommend(trimmed, 5, filterStatus),
        aiApi.similar(trimmed, 5),
      ])
      setResult(rec)
      setSimilar(sim ?? [])
    } catch {
      setError('AI recommendation is temporarily unavailable. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [])

  const clear = useCallback(() => {
    setResult(null)
    setSimilar([])
    setError(null)
  }, [])

  return { result, similar, loading, error, recommend, clear }
}
