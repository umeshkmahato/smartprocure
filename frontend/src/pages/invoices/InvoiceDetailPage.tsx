import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { invoicesApi } from '@/api/invoices'
import StatusBadge from '@/components/ui/StatusBadge'
import { formatCurrency, formatDate, formatDateTime } from '@/lib/utils'
import { ArrowLeft, CheckCircle, XCircle, Eye, DollarSign, Ban } from 'lucide-react'

export default function InvoiceDetailPage() {
  const { id }  = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc       = useQueryClient()
  const invoiceId = Number(id)

  const [disputeReason, setDisputeReason] = useState('')
  const [showDisputeBox, setShowDisputeBox] = useState(false)

  const { data: inv, isLoading, isError } = useQuery({
    queryKey: ['invoice', invoiceId],
    queryFn: () => invoicesApi.getById(invoiceId),
    enabled: !isNaN(invoiceId),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['invoice', invoiceId] })

  const startReview = useMutation({ mutationFn: () => invoicesApi.startReview(invoiceId), onSuccess: invalidate })
  const verify      = useMutation({ mutationFn: () => invoicesApi.verify(invoiceId),      onSuccess: invalidate })
  const markPaid    = useMutation({ mutationFn: () => invoicesApi.markPaid(invoiceId),    onSuccess: invalidate })
  const cancel      = useMutation({ mutationFn: () => invoicesApi.cancel(invoiceId),      onSuccess: invalidate })
  const dispute     = useMutation({
    mutationFn: () => invoicesApi.dispute(invoiceId, disputeReason),
    onSuccess: () => { invalidate(); setShowDisputeBox(false); setDisputeReason('') },
  })

  if (isLoading) return <p className="p-8 text-slate-400 text-sm">Loading…</p>
  if (isError || !inv) return <p className="p-8 text-red-500 text-sm">Invoice not found.</p>

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Back + header */}
      <div>
        <button onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-3">
          <ArrowLeft size={15} /> Back
        </button>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">{inv.invoiceNumber}</h1>
            <p className="text-slate-500 text-sm mt-0.5">
              Invoice for PO{' '}
              <Link to={`/purchase-orders/${inv.purchaseOrderId}`}
                className="text-blue-600 hover:underline font-medium">{inv.poNumber}</Link>
            </p>
          </div>
          <StatusBadge status={inv.status} />
        </div>
      </div>

      {/* Action bar */}
      {(inv.status === 'SUBMITTED' || inv.status === 'UNDER_REVIEW' || inv.status === 'VERIFIED') && (
        <div className="flex flex-wrap gap-2">
          {inv.status === 'SUBMITTED' && (
            <button onClick={() => startReview.mutate()} disabled={startReview.isPending}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 px-3 py-1.5 rounded-lg disabled:opacity-50">
              <Eye size={14} /> Start Review
            </button>
          )}
          {inv.status === 'UNDER_REVIEW' && (
            <>
              <button onClick={() => verify.mutate()} disabled={verify.isPending}
                className="inline-flex items-center gap-1.5 text-sm font-medium bg-green-50 text-green-700 hover:bg-green-100 px-3 py-1.5 rounded-lg disabled:opacity-50">
                <CheckCircle size={14} /> Verify
              </button>
              <button onClick={() => setShowDisputeBox(v => !v)}
                className="inline-flex items-center gap-1.5 text-sm font-medium bg-orange-50 text-orange-700 hover:bg-orange-100 px-3 py-1.5 rounded-lg">
                <XCircle size={14} /> Dispute
              </button>
            </>
          )}
          {inv.status === 'VERIFIED' && (
            <button onClick={() => markPaid.mutate()} disabled={markPaid.isPending}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-emerald-50 text-emerald-700 hover:bg-emerald-100 px-3 py-1.5 rounded-lg disabled:opacity-50">
              <DollarSign size={14} /> Mark Paid
            </button>
          )}
          <button onClick={() => cancel.mutate()} disabled={cancel.isPending}
            className="inline-flex items-center gap-1.5 text-sm font-medium bg-red-50 text-red-700 hover:bg-red-100 px-3 py-1.5 rounded-lg disabled:opacity-50">
            <Ban size={14} /> Cancel
          </button>
        </div>
      )}

      {/* Dispute reason input */}
      {showDisputeBox && (
        <div className="bg-orange-50 border border-orange-200 rounded-xl p-4 space-y-3">
          <p className="text-sm font-medium text-orange-800">Dispute reason (required)</p>
          <textarea
            value={disputeReason}
            onChange={e => setDisputeReason(e.target.value)}
            rows={3}
            placeholder="Describe the dispute reason…"
            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-400"
          />
          <div className="flex gap-2">
            <button onClick={() => dispute.mutate()} disabled={dispute.isPending || !disputeReason.trim()}
              className="text-sm font-medium bg-orange-600 text-white hover:bg-orange-700 px-4 py-1.5 rounded-lg disabled:opacity-50">
              Submit Dispute
            </button>
            <button onClick={() => setShowDisputeBox(false)}
              className="text-sm text-slate-500 hover:text-slate-700 px-3 py-1.5">
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Detail grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl border p-5 space-y-4">
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">Invoice Info</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Invoice Number</dt>
              <dd className="font-medium text-slate-800">{inv.invoiceNumber}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Vendor</dt>
              <dd className="font-medium text-slate-800">{inv.vendorName}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Purchase Order</dt>
              <dd>
                <Link to={`/purchase-orders/${inv.purchaseOrderId}`}
                  className="text-blue-600 hover:underline font-medium">{inv.poNumber}</Link>
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Total Amount</dt>
              <dd className="font-semibold text-slate-900">{formatCurrency(inv.totalAmount, inv.currency)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Invoice Date</dt>
              <dd>{formatDate(inv.invoiceDate)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Due Date</dt>
              <dd>{formatDate(inv.dueDate) ?? '—'}</dd>
            </div>
          </dl>
        </div>

        <div className="bg-white rounded-xl border p-5 space-y-4">
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">Status & Timestamps</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between items-center">
              <dt className="text-slate-500">Status</dt>
              <dd><StatusBadge status={inv.status} /></dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Created</dt>
              <dd className="text-slate-600">{formatDateTime(inv.createdAt)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Verified At</dt>
              <dd className="text-slate-600">{formatDateTime(inv.verifiedAt) ?? '—'}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Paid At</dt>
              <dd className="text-slate-600">{formatDateTime(inv.paidAt) ?? '—'}</dd>
            </div>
            {inv.documentUrl && (
              <div className="flex justify-between">
                <dt className="text-slate-500">Document</dt>
                <dd>
                  <a href={inv.documentUrl} target="_blank" rel="noreferrer"
                    className="text-blue-600 hover:underline text-xs">View Document</a>
                </dd>
              </div>
            )}
          </dl>
        </div>
      </div>

      {/* Notes / Dispute Reason */}
      {(inv.notes || inv.disputeReason) && (
        <div className="bg-white rounded-xl border p-5 space-y-3">
          {inv.notes && (
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500 mb-1">Notes</p>
              <p className="text-sm text-slate-700">{inv.notes}</p>
            </div>
          )}
          {inv.disputeReason && (
            <div>
              <p className="text-xs font-semibold uppercase text-orange-600 mb-1">Dispute Reason</p>
              <p className="text-sm text-orange-700 bg-orange-50 rounded-lg p-3">{inv.disputeReason}</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
