import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { purchaseRequestsApi } from '@/api/purchaseRequests'
import StatusBadge from '@/components/ui/StatusBadge'
import { formatCurrency, formatDate } from '@/lib/utils'
import { ArrowLeft } from 'lucide-react'

export default function PurchaseRequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: pr, isLoading } = useQuery({
    queryKey: ['pr', id],
    queryFn: () => purchaseRequestsApi.getById(Number(id)),
  })

  const submit = useMutation({
    mutationFn: () => purchaseRequestsApi.submit(Number(id)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pr', id] }),
  })
  const cancel = useMutation({
    mutationFn: () => purchaseRequestsApi.cancel(Number(id)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pr', id] }),
  })

  if (isLoading) return <p className="p-6 text-sm text-slate-400">Loading…</p>
  if (!pr)       return <p className="p-6 text-sm text-red-500">Purchase request not found.</p>

  const Row = ({ label, value }: { label: string; value?: string | number | null }) => (
    <div className="flex gap-4 py-3 border-b last:border-0">
      <span className="w-44 text-sm text-slate-500 shrink-0">{label}</span>
      <span className="text-sm text-slate-900 font-medium">{value ?? '—'}</span>
    </div>
  )

  return (
    <div className="max-w-4xl space-y-5">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft size={15} /> Back
      </button>

      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{pr.prNumber}</h1>
          <p className="text-slate-600 mt-0.5">{pr.title}</p>
        </div>
        <StatusBadge status={pr.status} />
      </div>

      <div className="bg-white rounded-xl border p-5">
        <h2 className="font-semibold text-slate-700 mb-3">Details</h2>
        <Row label="Department"      value={pr.department} />
        <Row label="Requested by"    value={pr.requestedByName} />
        <Row label="Required date"   value={formatDate(pr.requiredDate)} />
        <Row label="Estimated total" value={formatCurrency(pr.estimatedTotal, pr.currency)} />
        <Row label="Description"     value={pr.description} />
        <Row label="Justification"   value={pr.justification} />
        <Row label="Created"         value={formatDate(pr.createdAt)} />
      </div>

      {pr.items?.length > 0 && (
        <div className="bg-white rounded-xl border overflow-hidden">
          <div className="px-5 py-4 border-b">
            <h2 className="font-semibold text-slate-700">Line Items</h2>
          </div>
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
              <tr>{['Item','Description','Qty','Unit','Unit Price','Total'].map(h => (
                <th key={h} className="px-5 py-3 text-left font-medium">{h}</th>
              ))}</tr>
            </thead>
            <tbody className="divide-y">
              {pr.items.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50">
                  <td className="px-5 py-3 font-medium text-slate-800">{item.itemName}</td>
                  <td className="px-5 py-3 text-slate-500">{item.description ?? '—'}</td>
                  <td className="px-5 py-3">{item.quantity}</td>
                  <td className="px-5 py-3 text-slate-500">{item.unit ?? '—'}</td>
                  <td className="px-5 py-3">{formatCurrency(item.estimatedUnitPrice, pr.currency)}</td>
                  <td className="px-5 py-3 font-medium">{formatCurrency(item.estimatedTotal, pr.currency)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="flex gap-3">
        {pr.status === 'DRAFT' && (
          <button onClick={() => submit.mutate()} disabled={submit.isPending}
            className="bg-blue-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50">
            {submit.isPending ? 'Submitting…' : 'Submit for Approval'}
          </button>
        )}
        {(pr.status === 'DRAFT' || pr.status === 'SUBMITTED') && (
          <button onClick={() => cancel.mutate()} disabled={cancel.isPending}
            className="border border-red-300 text-red-600 text-sm font-medium px-4 py-2 rounded-lg hover:bg-red-50 disabled:opacity-50">
            Cancel Request
          </button>
        )}
      </div>
    </div>
  )
}
