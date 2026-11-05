import { useState } from 'react'
import { Sparkles, Search, X, ExternalLink } from 'lucide-react'
import { useAIRecommend } from '@/hooks/useAI'
import { cn } from '@/lib/utils'
import { Link } from 'react-router-dom'

/**
 * AI-powered vendor recommendation panel.
 *
 * Renders a search bar that accepts a natural-language requirement,
 * fires both the RAG recommendation (LLM text) and the vector
 * similarity search (structured vendor cards) in parallel, then
 * displays the results inline.
 *
 * Usage: drop this anywhere a vendor-discovery surface is needed
 * (VendorsPage, DashboardPage, etc.).
 */
export default function AiRecommendations() {
  const [query,        setQuery]        = useState('')
  const [filterStatus, setFilterStatus] = useState('APPROVED')
  const { result, similar, loading, error, recommend, clear } = useAIRecommend()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (query.trim()) recommend(query, filterStatus)
  }

  const handleClear = () => {
    setQuery('')
    clear()
  }

  return (
    <div className="bg-white rounded-xl border overflow-hidden">
      {/* Header */}
      <div className="px-5 py-4 border-b flex items-center gap-2.5">
        <div className="w-7 h-7 rounded-md bg-violet-100 flex items-center justify-center">
          <Sparkles size={14} className="text-violet-600" />
        </div>
        <div>
          <h2 className="font-semibold text-slate-800 leading-none">AI Vendor Recommendations</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Describe your requirement — the AI will find the best-fit vendors.
          </p>
        </div>
      </div>

      {/* Search bar */}
      <div className="px-5 pt-4 pb-3">
        <form onSubmit={handleSubmit} className="flex gap-2">
          <div className="relative flex-1">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder='e.g. "reliable IT hardware supplier in Asia with fast delivery"'
              className="w-full pl-9 pr-3 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-violet-500"
              disabled={loading}
            />
          </div>
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-violet-500"
            disabled={loading}
          >
            <option value="APPROVED">Approved</option>
            <option value="">All statuses</option>
            <option value="PENDING">Pending</option>
          </select>
          <button
            type="submit"
            disabled={loading || !query.trim()}
            className="inline-flex items-center gap-1.5 bg-violet-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <Sparkles size={14} />
            {loading ? 'Thinking…' : 'Recommend'}
          </button>
          {(result || error) && (
            <button
              type="button"
              onClick={handleClear}
              className="p-2 rounded-lg border text-slate-500 hover:bg-slate-50 transition-colors"
              title="Clear results"
            >
              <X size={15} />
            </button>
          )}
        </form>
      </div>

      {/* Loading skeleton */}
      {loading && (
        <div className="px-5 pb-5 space-y-3">
          <div className="h-3 bg-slate-100 rounded animate-pulse w-3/4" />
          <div className="h-3 bg-slate-100 rounded animate-pulse w-full" />
          <div className="h-3 bg-slate-100 rounded animate-pulse w-5/6" />
          <div className="h-3 bg-slate-100 rounded animate-pulse w-2/3" />
        </div>
      )}

      {/* Error */}
      {error && !loading && (
        <div className="mx-5 mb-5 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Results */}
      {!loading && result && (
        <div className="px-5 pb-5 space-y-5">
          {/* LLM narrative */}
          <div className="rounded-lg bg-violet-50 border border-violet-100 px-4 py-4">
            <p className="text-xs font-semibold text-violet-700 uppercase tracking-wide mb-2">
              AI Recommendation
            </p>
            <p className="text-sm text-slate-700 whitespace-pre-line leading-relaxed">{result}</p>
          </div>

          {/* Structurally similar vendors from vector store */}
          {similar.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
                Semantically Similar Vendors
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3">
                {similar.map((v) => (
                  <div
                    key={v.vendorId}
                    className="rounded-lg border p-3.5 hover:border-violet-200 hover:bg-violet-50/40 transition-colors"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="font-medium text-sm text-slate-800 leading-snug">
                        {v.vendorName}
                      </span>
                      <Link
                        to={`/vendors/${v.vendorId}`}
                        className="shrink-0 text-violet-500 hover:text-violet-700"
                        title="View vendor"
                      >
                        <ExternalLink size={13} />
                      </Link>
                    </div>
                    <div className="mt-1.5 flex flex-wrap gap-1.5 text-xs text-slate-500">
                      {v.category && (
                        <span className="rounded-full bg-slate-100 px-2 py-0.5">{v.category}</span>
                      )}
                      {v.country && (
                        <span className="rounded-full bg-slate-100 px-2 py-0.5">{v.country}</span>
                      )}
                    </div>
                    <div className="mt-2">
                      <ScoreBar score={v.score} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

/** Thin colour-coded similarity score bar. */
function ScoreBar({ score }: { score: number }) {
  const pct  = Math.round(score * 100)
  const colour = pct >= 80
    ? 'bg-green-500'
    : pct >= 60
      ? 'bg-teal-500'
      : pct >= 40
        ? 'bg-yellow-500'
        : 'bg-slate-300'

  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-1.5 rounded-full bg-slate-100 overflow-hidden">
        <div
          className={cn('h-full rounded-full transition-all', colour)}
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="text-xs text-slate-400 tabular-nums">{pct}%</span>
    </div>
  )
}
