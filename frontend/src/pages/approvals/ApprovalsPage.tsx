import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { approvalsApi } from '@/api/approvals'
import StatusBadge from '@/components/ui/StatusBadge'
import SortableHeader from '@/components/ui/SortableHeader'
import Pagination from '@/components/ui/Pagination'
import type { SortState } from '@/types'
import { formatDateTime } from '@/lib/utils'
import { CheckCircle, XCircle } from 'lucide-react'

export default function ApprovalsPage() {
  const qc = useQueryClient()
  const [status,  setStatus]  = useState('')
  const [page,    setPage]    = useState(0)
  const [sort,    setSort]    = useState<SortState>({ field: 'createdAt', dir: 'desc' })
  const [comment, setComment] = useState<Record<number, string>>({})

  const handleSort = (field: string) => {
    setSort(prev => ({ field, dir: prev.field === field && prev.dir === 'desc' ? 'asc' : 'desc' }))
    setPage(0)
  }

  const { data, isLoading } = useQuery({
    queryKey: ['approvals', status, page, sort],
    queryFn: () => approvalsApi.list({
      status: status || undefined,
      page, size: 20,
      sort: sort.field, dir: sort.dir,
    }),
  })

  const approve = useMutation({
    mutationFn: ({ id, comments }: { id: number; comments?: string }) =>
      approvalsApi.approve(id, comments),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['approvals'] }),
  })

  const reject = useMutation({
    mutationFn: ({ id, comments }: { id: number; comments: string }) =>
      approvalsApi.reject(id, comments),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['approvals'] }),
  })

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Approvals</h1>
          <p className="text-slate-500 text-sm mt-0.5">{data?.totalElements ?? 0} approval steps</p>
        </div>
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option value="">All statuses</option>
          {['PENDING','APPROVED','REJECTED','DELEGATED'].map(s => <option key={s}>{s}</option>)}
        </select>
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        {isLoading ? <p className="p-6 text-sm text-slate-400">Loading…</p>
          : !data?.content.length ? <p className="p-6 text-sm text-slate-400">No approval steps found.</p>
          : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50">
              <tr>
                <SortableHeader label="PR Number" field="prNumber"    currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Title</th>
                <SortableHeader label="Approver"  field="approverName" currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Level"     field="level"        currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Status"    field="status"       currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Actioned"  field="actionedAt"   currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Comments</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.content.map((step) => (
                <tr key={step.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3">
                    <Link to={`/purchase-requests/${step.purchaseRequestId}`}
                      className="text-blue-600 hover:underline font-medium">{step.prNumber}</Link>
                  </td>
                  <td className="px-4 py-3 text-slate-700 max-w-[180px] truncate">{step.prTitle}</td>
                  <td className="px-4 py-3 text-slate-600">{step.approverName}</td>
                  <td className="px-4 py-3 text-center">{step.level}</td>
                  <td className="px-4 py-3"><StatusBadge status={step.status} /></td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{formatDateTime(step.actionedAt)}</td>
                  <td className="px-4 py-3 max-w-[160px]">
                    {step.status === 'PENDING' ? (
                      <input placeholder="Comment (optional)"
                        className="border rounded px-2 py-1 text-xs w-full focus:outline-none focus:ring-1 focus:ring-blue-400"
                        value={comment[step.id] ?? ''}
                        onChange={e => setComment(c => ({ ...c, [step.id]: e.target.value }))}
                      />
                    ) : (
                      <span className="text-slate-500 text-xs">{step.comments ?? '—'}</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {step.status === 'PENDING' && (
                      <div className="flex gap-2">
                        <button onClick={() => approve.mutate({ id: step.id, comments: comment[step.id] })}
                          disabled={approve.isPending}
                          title="Approve"
                          className="text-green-600 hover:text-green-800 disabled:opacity-40">
                          <CheckCircle size={18} />
                        </button>
                        <button onClick={() => reject.mutate({ id: step.id, comments: comment[step.id] ?? 'Rejected' })}
                          disabled={reject.isPending || !comment[step.id]}
                          title="Reject (comment required)"
                          className="text-red-500 hover:text-red-700 disabled:opacity-40">
                          <XCircle size={18} />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {data && (
        <Pagination
          page={data.number}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          onPageChange={setPage}
        />
      )}
    </div>
  )
}
