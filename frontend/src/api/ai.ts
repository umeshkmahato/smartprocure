import client from './client'
import type { ApiResponse, AiSimilarVendor } from '@/types'

/** Separate timeout for AI calls — Ollama can be slow on first inference. */
const AI_TIMEOUT = 90_000

export const aiApi = {
  /**
   * General-purpose procurement assistant.
   * POST /api/v1/ai/chat
   */
  chat: (message: string) =>
    client
      .post<ApiResponse<string>>('/ai/chat', { message }, { timeout: AI_TIMEOUT })
      .then((r) => r.data.data),

  /**
   * Full RAG pipeline — returns LLM-ranked recommendation text.
   * POST /api/v1/ai/recommend
   */
  recommend: (query: string, topK = 5, filterStatus = 'APPROVED') =>
    client
      .post<ApiResponse<string>>(
        '/ai/recommend',
        { query, topK, filterStatus },
        { timeout: AI_TIMEOUT },
      )
      .then((r) => r.data.data),

  /**
   * Fast vector similarity search — no LLM, returns structured results.
   * GET /api/v1/ai/similar
   */
  similar: (query: string, topK = 5) =>
    client
      .get<ApiResponse<AiSimilarVendor[]>>('/ai/similar', { params: { query, topK } })
      .then((r) => r.data.data),

  /**
   * Admin-only: trigger bulk re-embedding of all APPROVED vendors.
   * POST /api/v1/ai/embed
   */
  embedAll: () =>
    client.post<ApiResponse<string>>('/ai/embed').then((r) => r.data.data),
}
