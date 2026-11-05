import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { purchaseOrdersApi } from '@/api/purchaseOrders'
import StatusBadge from '@/components/ui/StatusBadge'
import { formatCurrency, formatDate, formatDateTime } from '@/lib/utils'
import { ArrowLeft, Send, CheckCircle, Ban, XCircle } from 'lucide-react'

export default function PurchaseOrderDetailPage() {
  const { id }  = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc       = useQueryClient()
  const poId     = Number(id)

  const { data: po, isLoading, isError } = useQuery({
    queryKey: ['po', poId],
    queryFn:  () => purchaseOrdersApi.getById(poId),
    enabled:  !isNaN(poId),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['po', poId] })

  const issue       = useMutation({ mutationFn: () => purchaseOrdersApi.issue(poId),       onSuccess: invalidate })
  const acknowledge = useMutation({ mutationFn: () => purchaseOrdersApi.acknowledge(poId), onSuccess: invalidate })
  const cancel      = useMutation({ mutationFn: () => purchaseOrdersApi.cancel(poId),      onSuccess: invalidate })
  const close       = useMutation({ mutationFn: () => purchaseOrdersApi.close(poId),       onSuccess: invalidate })

  if (isLoading) return <p className="p-8 text-slate-400 text-sm">Loading…</p>
  if (isError || !po) return <p className="p-8 text-red-500 text-sm">Purchase Order not found.</p>

  const canIssue       = po.status === 'DRAFT'
  const canAcknowledge = po.status === 'ISSUED'
  const canClose       = po.status === 'DELIVERED' || po.status === 'INVOICED'
  const canCancel      = po.status === 'DRAFT' || po.status === 'ISSUED'

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Back + header */}
      <div>
        <button onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-3">
          <ArrowLeft size={15} /> Back
        </button>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">{po.poNumber}</h1>
            <p className="text-slate-500 text-sm mt-0.5">
              Vendor: <span className="font-medium text-slate-700">{po.vendorName}</span>
              {po.purchaseRequestId && (
                <> · PR: <Link to={`/purchase-requests/${po.purchaseRequestId}`}
                  className="text-blue-600 hover:underline">View PR</Link></>
              )}
            </p>
          </div>
          <StatusBadge status={po.status} />
        </div>
      </div>

      {/* Action bar */}
      {(canIssue || canAcknowledge || canClose || canCancel) && (
        <div className="flex flex-wrap gap-2">
          {canIssue && (
            <button onClick={() => issue.mutate()} disabled={issue.isPending}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 px-3 py-1.5 rounded-lg disabled:opacity-50">
              <Send size={14} /> Issue to Vendor
            </button>
          )}
          {canAcknowledge && (
            <button onClick={() => acknowledge.mutate()} disabled={acknowledge.isPending}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-green-50 text-green-700 hover:bg-green-100 px-3 py-1.5 rounded-lg disabled:opacity-50">
              <CheckCircle size={14} /> Acknowledge
            </button>
          )}
          {canClose && (
            <button onClick={() => close.mutate()} disabled={close.isPending}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-slate-100 text-slate-700 hover:bg-slate-200 px-3 py-1.5 rounded-lg disabled:opacity-50">
              <XCircle size={14} /> Close PO
            </button>
          )}
          {canCancel && (
            <button onClick={() => cancel.mutate()} disabled={cancel.isPending}
              className="inline-flex items-center gap-1.5 text-sm font-medium bg-red-50 text-red-700 hover:bg-red-100 px-3 py-1.5 rounded-lg disabled:opacity-50">
              <Ban size={14} /> Cancel
            </button>
          )}
        </div>
      )}

      {/* Detail grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        {/* PO Info */}
        <div className="bg-white rounded-xl border p-5 space-y-4">
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">Order Info</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">PO Number</dt>
              <dd className="font-medium text-slate-800">{po.poNumber}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Vendor</dt>
              <dd className="font-medium text-slate-800">{po.vendorName}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Total Amount</dt>
              <dd className="font-semibold text-slate-900">{formatCurrency(po.totalAmount, po.currency)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Currency</dt>
              <dd>{po.currency}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Delivery Date</dt>
              <dd>{formatDate(po.deliveryDate)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Delivery Address</dt>
              <dd className="text-right max-w-[200px] text-slate-700">{po.deliveryAddress ?? '—'}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Payment Terms</dt>
              <dd>{po.paymentTerms ?? '—'}</dd>
            </div>
          </dl>
        </div>

        {/* Timestamps */}
        <div className="bg-white rounded-xl border p-5 space-y-4">
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">Status & Timeline</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between items-center">
              <dt className="text-slate-500">Status</dt>
              <dd><StatusBadge status={po.status} /></dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Created</dt>
              <dd className="text-slate-600">{formatDateTime(po.createdAt)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Issued At</dt>
              <dd className="text-slate-600">{formatDateTime(po.issuedAt)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Acknowledged</dt>
              <dd className="text-slate-600">{formatDateTime(po.acknowledgedAt)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Delivered</dt>
              <dd className="text-slate-600">{formatDateTime(po.deliveredAt)}</dd>
            </div>
            {po.issuedByName && (
              <div className="flex justify-between">
                <dt className="text-slate-500">Issued By</dt>
                <dd className="font-medium">{po.issuedByName}</dd>
              </div>
            )}
          </dl>
        </div>
      </div>

      {/* Notes */}
      {po.notes && (
        <div className="bg-white rounded-xl border p-5">
          <p className="text-xs font-semibold uppercase text-slate-500 mb-2">Notes</p>
          <p className="text-sm text-slate-700">{po.notes}</p>
        </div>
      )}

      {/* Line items */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b">
          <h2 className="text-sm font-semibold text-slate-700">Line Items</h2>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
            <tr>
              {['Item', 'Description', 'Qty', 'Unit', 'Unit Price', 'Total', 'Delivered'].map(h => (
                <th key={h} className="px-5 py-3 text-left font-medium">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y">
            {po.items.map((item) => (
              <tr key={item.id} className="hover:bg-slate-50">
                <td className="px-5 py-3 font-medium text-slate-800">{item.itemName}</td>
                <td className="px-5 py-3 text-slate-500 max-w-[200px] truncate">{item.description ?? '—'}</td>
                <td className="px-5 py-3">{item.quantity}</td>
                <td className="px-5 py-3 text-slate-500">{item.unit ?? '—'}</td>
                <td className="px-5 py-3">{formatCurrency(item.unitPrice, po.currency)}</td>
                <td className="px-5 py-3 font-medium">{formatCurrency(item.totalPrice, po.currency)}</td>
                <td className="px-5 py-3">
                  <span className={item.deliveredQty >= item.quantity ? 'text-green-600 font-medium' : 'text-slate-500'}>
                    {item.deliveredQty} / {item.quantity}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot className="bg-slate-50">
            <tr>
              <td colSpan={5} className="px-5 py-3 text-right text-sm font-semibold text-slate-700">Total</td>
              <td className="px-5 py-3 font-bold text-slate-900">{formatCurrency(po.totalAmount, po.currency)}</td>
              <td />
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  )
}
