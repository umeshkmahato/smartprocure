import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { purchaseRequestsApi } from '@/api/purchaseRequests'
import StatusBadge from '@/components/ui/StatusBadge'
import { Button } from '@/components/ui/button'
import { formatCurrency, formatDate } from '@/lib/utils'
import { ArrowLeft, FileSearch, Send, XCircle, CalendarDays, Building2, User, DollarSign, FileText, Info } from 'lucide-react'

export default function PurchaseRequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const prId = Number(id)

  const { data: pr, isLoading } = useQuery({
    queryKey: ['pr', prId],
    queryFn: () => purchaseRequestsApi.getById(prId),
    enabled: !isNaN(prId),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['pr', prId] })

  const submit = useMutation({ mutationFn: () => purchaseRequestsApi.submit(prId), onSuccess: invalidate })
  const cancel = useMutation({ mutationFn: () => purchaseRequestsApi.cancel(prId), onSuccess: invalidate })

  if (isLoading) return (
    <div className="flex items-center justify-center h-48">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
    </div>
  )
  if (!pr) return (
    <div className="rounded-xl border border-red-200 bg-red-50 dark:bg-red-950 dark:border-red-800 p-6 text-sm text-red-600 dark:text-red-400">
      Purchase request not found.
    </div>
  )

  const canSubmit = pr.status === 'DRAFT'
  const canCancel = pr.status === 'DRAFT' || pr.status === 'SUBMITTED'

  return (
    <div className="max-w-5xl space-y-6">
      {/* Back */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 transition-colors"
      >
        <ArrowLeft size={15} /> Back to Purchase Requests
      </button>

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-50 dark:bg-blue-950 border border-blue-100 dark:border-blue-900">
            <FileSearch className="h-6 w-6 text-blue-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">{pr.prNumber}</h1>
            <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">{pr.title}</p>
          </div>
        </div>
        <StatusBadge status={pr.status} />
      </div>

      {/* Action bar */}
      {(canSubmit || canCancel) && (
        <div className="flex flex-wrap gap-3">
          {canSubmit && (
            <Button onClick={() => submit.mutate()} loading={submit.isPending}>
              <Send className="h-4 w-4" /> Submit for Approval
            </Button>
          )}
          {canCancel && (
            <Button variant="danger-outline" onClick={() => cancel.mutate()} loading={cancel.isPending}>
              <XCircle className="h-4 w-4" /> Cancel Request
            </Button>
          )}
        </div>
      )}

      {/* Info grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        {/* Request Details */}
        <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 space-y-4">
          <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">Request Details</h2>
          <dl className="space-y-3 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="flex items-center gap-2 text-slate-500 dark:text-slate-400 shrink-0">
                <User className="h-3.5 w-3.5" /> Requested By
              </dt>
              <dd className="font-medium text-slate-800 dark:text-slate-200 text-right">{pr.requestedByName}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="flex items-center gap-2 text-slate-500 dark:text-slate-400 shrink-0">
                <Building2 className="h-3.5 w-3.5" /> Department
              </dt>
              <dd className="font-medium text-slate-800 dark:text-slate-200 text-right">{pr.department ?? '—'}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="flex items-center gap-2 text-slate-500 dark:text-slate-400 shrink-0">
                <CalendarDays className="h-3.5 w-3.5" /> Required Date
              </dt>
              <dd className="font-medium text-slate-800 dark:text-slate-200 text-right">{formatDate(pr.requiredDate)}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="flex items-center gap-2 text-slate-500 dark:text-slate-400 shrink-0">
                <CalendarDays className="h-3.5 w-3.5" /> Created
              </dt>
              <dd className="text-slate-600 dark:text-slate-400 text-right">{formatDate(pr.createdAt)}</dd>
            </div>
          </dl>
        </div>

        {/* Financial & Status */}
        <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 space-y-4">
          <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">Financial &amp; Status</h2>
          <dl className="space-y-3 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="flex items-center gap-2 text-slate-500 dark:text-slate-400 shrink-0">
                <DollarSign className="h-3.5 w-3.5" /> Estimated Total
              </dt>
              <dd className="font-bold text-lg text-slate-900 dark:text-slate-100">
                {formatCurrency(pr.estimatedTotal, pr.currency)}
              </dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500 dark:text-slate-400">Currency</dt>
              <dd className="font-medium text-slate-800 dark:text-slate-200">{pr.currency}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500 dark:text-slate-400">Status</dt>
              <dd><StatusBadge status={pr.status} /></dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500 dark:text-slate-400">Items</dt>
              <dd className="font-medium text-slate-800 dark:text-slate-200">{pr.items?.length ?? 0} line items</dd>
            </div>
          </dl>
        </div>
      </div>

      {/* Description / Justification */}
      {(pr.description || pr.justification) && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          {pr.description && (
            <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
              <div className="flex items-center gap-2 mb-2">
                <FileText className="h-3.5 w-3.5 text-slate-400" />
                <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">Description</h2>
              </div>
              <p className="text-sm text-slate-700 dark:text-slate-300 whitespace-pre-line leading-relaxed">{pr.description}</p>
            </div>
          )}
          {pr.justification && (
            <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
              <div className="flex items-center gap-2 mb-2">
                <Info className="h-3.5 w-3.5 text-slate-400" />
                <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">Justification</h2>
              </div>
              <p className="text-sm text-slate-700 dark:text-slate-300 whitespace-pre-line leading-relaxed">{pr.justification}</p>
            </div>
          )}
        </div>
      )}

      {/* Line items table */}
      {pr.items?.length > 0 && (
        <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
            <h2 className="font-semibold text-slate-800 dark:text-slate-200">Line Items</h2>
            <span className="text-xs text-slate-400">{pr.items.length} items</span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 text-xs uppercase">
                <tr>
                  {['Item', 'Description', 'Qty', 'Unit', 'Unit Price', 'Total'].map(h => (
                    <th key={h} className="px-5 py-3 text-left font-semibold tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {pr.items.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-5 py-3.5 font-medium text-slate-800 dark:text-slate-200">{item.itemName}</td>
                    <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400 max-w-[200px] truncate">{item.description ?? '—'}</td>
                    <td className="px-5 py-3.5 text-slate-700 dark:text-slate-300">{item.quantity}</td>
                    <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">{item.unit ?? '—'}</td>
                    <td className="px-5 py-3.5 text-slate-700 dark:text-slate-300">{formatCurrency(item.estimatedUnitPrice, pr.currency)}</td>
                    <td className="px-5 py-3.5 font-semibold text-slate-900 dark:text-slate-100">{formatCurrency(item.estimatedTotal, pr.currency)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="bg-slate-50 dark:bg-slate-800/50 border-t border-slate-100 dark:border-slate-800">
                <tr>
                  <td colSpan={5} className="px-5 py-3 text-right text-sm font-semibold text-slate-700 dark:text-slate-300">Estimated Total</td>
                  <td className="px-5 py-3 font-bold text-slate-900 dark:text-slate-100">{formatCurrency(pr.estimatedTotal, pr.currency)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
