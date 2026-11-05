import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { quotationsApi } from '@/api/quotations'
import StatusBadge from '@/components/ui/StatusBadge'
import { formatCurrency, formatDate, formatDateTime } from '@/lib/utils'
import { ArrowLeft, Send, X, Trophy, CheckCircle } from 'lucide-react'
import type { Quotation } from '@/types'

export default function QuotationDetailPage() {
  const { id }   = useParams<{ id: string }>()
  const navigate  = useNavigate()
  const qc        = useQueryClient()
  const rfqId     = Number(id)

  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [confirmSelect, setConfirmSelect] = useState(false)

  /* ── Queries ─────────────────────────────────────────────────────────────── */
  const { data: rfq, isLoading: rfqLoading } = useQuery({
    queryKey: ['rfq', rfqId],
    queryFn:  () => quotationsApi.getRfqById(rfqId),
    enabled:  !isNaN(rfqId),
  })

  const { data: quotations, isLoading: quotationsLoading } = useQuery({
    queryKey: ['rfq-quotations', rfqId],
    queryFn:  () => quotationsApi.listQuotations(rfqId),
    enabled:  !isNaN(rfqId),
  })

  const { data: comparison, refetch: refetchComparison, isFetching: comparing } = useQuery({
    queryKey: ['rfq-compare', rfqId],
    queryFn:  () => quotationsApi.compareQuotations(rfqId),
    enabled:  false,          // only fetch on demand
  })

  /* ── Mutations ───────────────────────────────────────────────────────────── */
  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['rfq', rfqId] })
    qc.invalidateQueries({ queryKey: ['rfq-quotations', rfqId] })
    qc.invalidateQueries({ queryKey: ['rfqs'] })
  }

  const publish = useMutation({
    mutationFn: () => quotationsApi.publishRfq(rfqId),
    onSuccess:  invalidate,
  })

  const close = useMutation({
    mutationFn: () => quotationsApi.closeRfq(rfqId),
    onSuccess:  invalidate,
  })

  const selectWinner = useMutation({
    mutationFn: (quotationId: number) => quotationsApi.selectQuotation(rfqId, quotationId),
    onSuccess:  () => { invalidate(); setConfirmSelect(false); setSelectedId(null) },
  })

  /* ── Helpers ─────────────────────────────────────────────────────────────── */
  const displayList: Quotation[] = comparison ?? quotations ?? []
  const lowestAmount = displayList.length
    ? Math.min(...displayList.map(q => q.totalAmount))
    : null

  if (rfqLoading) return <p className="p-8 text-slate-400 text-sm">Loading…</p>
  if (!rfq)       return <p className="p-8 text-red-500 text-sm">RFQ not found.</p>

  return (
    <div className="space-y-6 max-w-5xl">
      {/* ── Header ─────────────────────────────────────────────────────────── */}
      <div>
        <button onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-3">
          <ArrowLeft size={15} /> Back
        </button>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">{rfq.rfqNumber}</h1>
            <p className="text-slate-600 text-sm mt-0.5 max-w-xl">{rfq.title}</p>
          </div>
          <StatusBadge status={rfq.status} />
        </div>
      </div>

      {/* ── Action bar ─────────────────────────────────────────────────────── */}
      <div className="flex flex-wrap gap-2">
        {rfq.status === 'DRAFT' && (
          <button onClick={() => publish.mutate()} disabled={publish.isPending}
            className="inline-flex items-center gap-1.5 text-sm font-medium bg-blue-600 text-white hover:bg-blue-700 px-4 py-2 rounded-lg disabled:opacity-50">
            <Send size={14} /> Publish RFQ
          </button>
        )}
        {rfq.status === 'PUBLISHED' && (
          <>
            <button onClick={() => refetchComparison()} disabled={comparing}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-teal-50 text-teal-700 hover:bg-teal-100 px-4 py-2 rounded-lg disabled:opacity-50">
              <Trophy size={14} /> {comparing ? 'Comparing…' : 'Compare Quotations'}
            </button>
            <button onClick={() => close.mutate()} disabled={close.isPending}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-slate-100 text-slate-600 hover:bg-slate-200 px-4 py-2 rounded-lg disabled:opacity-50">
              <X size={14} /> Close RFQ
            </button>
          </>
        )}
      </div>

      {/* ── RFQ Details ────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl border p-5 space-y-3">
          <h2 className="text-xs font-semibold uppercase text-slate-500 tracking-wide">RFQ Info</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">RFQ Number</dt>
              <dd className="font-medium">{rfq.rfqNumber}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Status</dt>
              <dd><StatusBadge status={rfq.status} /></dd>
            </div>
            {rfq.deadline && (
              <div className="flex justify-between">
                <dt className="text-slate-500">Deadline</dt>
                <dd>{formatDateTime(rfq.deadline)}</dd>
              </div>
            )}
            {rfq.purchaseRequestId && (
              <div className="flex justify-between">
                <dt className="text-slate-500">Purchase Request</dt>
                <dd>
                  <Link to={`/purchase-requests/${rfq.purchaseRequestId}`}
                    className="text-blue-600 hover:underline">
                    PR-{rfq.purchaseRequestId}
                  </Link>
                </dd>
              </div>
            )}
            <div className="flex justify-between">
              <dt className="text-slate-500">Created</dt>
              <dd className="text-slate-500">{formatDate(rfq.createdAt)}</dd>
            </div>
          </dl>
        </div>

        {rfq.description && (
          <div className="bg-white rounded-xl border p-5">
            <h2 className="text-xs font-semibold uppercase text-slate-500 tracking-wide mb-2">Description</h2>
            <p className="text-sm text-slate-700 whitespace-pre-line">{rfq.description}</p>
          </div>
        )}
      </div>

      {/* ── Confirm Select Winner Dialog ────────────────────────────────────── */}
      {confirmSelect && selectedId && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-3">
          <p className="text-sm font-medium text-amber-800">
            ⚠️ Select this quotation as the winner? This action will close the RFQ and reject all other bids.
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => selectWinner.mutate(selectedId)}
              disabled={selectWinner.isPending}
              className="text-sm font-medium bg-amber-600 text-white hover:bg-amber-700 px-4 py-1.5 rounded-lg disabled:opacity-50">
              {selectWinner.isPending ? 'Selecting…' : 'Confirm Selection'}
            </button>
            <button onClick={() => { setConfirmSelect(false); setSelectedId(null) }}
              className="text-sm text-slate-600 hover:text-slate-800 px-3 py-1.5">
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* ── Quotations Table ────────────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b flex items-center justify-between">
          <h2 className="font-semibold text-slate-800">
            Vendor Quotations
            <span className="ml-2 text-xs font-normal text-slate-400">
              {displayList.length} received
              {comparison ? ' — sorted by price (comparison mode)' : ''}
            </span>
          </h2>
        </div>

        {quotationsLoading ? (
          <p className="p-6 text-sm text-slate-400">Loading quotations…</p>
        ) : !displayList.length ? (
          <p className="p-6 text-sm text-slate-400">
            No quotations received yet.
            {rfq.status === 'DRAFT' && ' Publish the RFQ first to invite vendors.'}
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
              <tr>
                <th className="px-5 py-3 text-left font-medium">Vendor</th>
                <th className="px-5 py-3 text-left font-medium">Status</th>
                <th className="px-5 py-3 text-left font-medium">Total Amount</th>
                <th className="px-5 py-3 text-left font-medium">Valid Until</th>
                <th className="px-5 py-3 text-left font-medium">Submitted</th>
                {rfq.status === 'PUBLISHED' && (
                  <th className="px-5 py-3 text-left font-medium">Action</th>
                )}
              </tr>
            </thead>
            <tbody className="divide-y">
              {displayList.map((q) => {
                const isLowest   = lowestAmount !== null && q.totalAmount === lowestAmount && displayList.length > 1
                const isSelected = q.status === 'SELECTED'
                return (
                  <tr key={q.id}
                    className={
                      isSelected ? 'bg-green-50'
                      : isLowest  ? 'bg-teal-50'
                      : 'hover:bg-slate-50'
                    }>
                    <td className="px-5 py-3 font-medium text-slate-800">
                      {q.vendorName}
                      {isLowest && !isSelected && (
                        <span className="ml-2 text-xs bg-teal-100 text-teal-700 rounded-full px-2 py-0.5">
                          Lowest
                        </span>
                      )}
                      {isSelected && (
                        <span className="ml-2 text-xs bg-green-100 text-green-700 rounded-full px-2 py-0.5 inline-flex items-center gap-1">
                          <CheckCircle size={10} /> Winner
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-3"><StatusBadge status={q.status} /></td>
                    <td className="px-5 py-3 font-semibold">
                      {formatCurrency(q.totalAmount, q.currency)}
                    </td>
                    <td className="px-5 py-3 text-slate-500">
                      {q.validUntil ? formatDate(q.validUntil) : '—'}
                    </td>
                    <td className="px-5 py-3 text-slate-400">{formatDate(q.createdAt)}</td>
                    {rfq.status === 'PUBLISHED' && (
                      <td className="px-5 py-3">
                        {q.status === 'SUBMITTED' && (
                          <button
                            onClick={() => { setSelectedId(q.id); setConfirmSelect(true) }}
                            className="text-xs font-medium text-amber-700 bg-amber-50 hover:bg-amber-100 px-3 py-1 rounded-lg">
                            Select Winner
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Notes summary row */}
      {displayList.some(q => q.notes) && (
        <div className="bg-white rounded-xl border p-5 space-y-3">
          <h2 className="text-xs font-semibold uppercase text-slate-500 tracking-wide">Vendor Notes</h2>
          {displayList.filter(q => q.notes).map(q => (
            <div key={q.id} className="text-sm">
              <span className="font-medium text-slate-700">{q.vendorName}:</span>{' '}
              <span className="text-slate-600">{q.notes}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
