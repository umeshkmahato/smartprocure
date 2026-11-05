import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { CheckSquare, ThumbsUp, ThumbsDown } from 'lucide-react'
import { approvalsApi } from '@/api/approvals'
import StatusBadge from '@/components/ui/StatusBadge'
import Pagination from '@/components/ui/Pagination'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { TableSkeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/ui/empty-state'
import { ErrorState } from '@/components/ui/error-state'
import { toast } from '@/hooks/useToast'
import { formatDate } from '@/lib/utils'

const PAGE_SIZE = 10

export default function ApprovalsPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [tab, setTab]   = useState<'pending'|'all'>('pending')
  const [comments, setComments] = useState<Record<number, string>>({})

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['approvals', tab, page],
    queryFn: () => tab === 'pending'
      ? approvalsApi.myPending()
      : approvalsApi.list({ page, size: PAGE_SIZE }),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['approvals'] })

  const approveMut = useMutation({
    mutationFn: ({ id, c }: { id: number; c: string }) => approvalsApi.approve(id, c),
    onSuccess: () => { invalidate(); toast({ title: 'Request approved', variant: 'success' }) },
    onError:   () => toast({ title: 'Approval failed', variant: 'danger' }),
  })

  const rejectMut = useMutation({
    mutationFn: ({ id, c }: { id: number; c: string }) => approvalsApi.reject(id, c),
    onSuccess: () => { invalidate(); toast({ title: 'Request rejected', variant: 'warning' }) },
    onError:   () => toast({ title: 'Rejection failed', variant: 'danger' }),
  })

  const rawData = data as any
  const items: any[] = rawData?.content ?? (Array.isArray(rawData) ? rawData : [])
  const totalPages: number = rawData?.totalPages ?? 1
  const totalElements: number = rawData?.totalElements ?? items.length

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Approvals</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Review and action pending approval requests</p>
        </div>
      </div>

      <div className="flex gap-1 border-b border-slate-200 dark:border-slate-800">
        {(['pending', 'all'] as const).map(t => (
          <button key={t} onClick={() => { setTab(t); setPage(0) }}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${tab === t
              ? 'border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'}`}>
            {t === 'pending' ? 'My Pending' : 'All Approvals'}
          </button>
        ))}
      </div>

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 dark:border-slate-800 bg-slate-50/60 dark:bg-slate-800/40">
                {['PR Number', 'Title', 'Requester', 'Status', 'Date', 'Actions'].map(h => (
                  <th key={h} className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-400">{h}</th>
                ))}
              </tr>
            </thead>
            {isLoading ? (
              <TableSkeleton rows={6} cols={6} />
            ) : isError ? (
              <tbody><tr><td colSpan={6}><ErrorState onRetry={refetch} /></td></tr></tbody>
            ) : !items.length ? (
              <tbody><tr><td colSpan={6}>
                <EmptyState icon={CheckSquare} title="No approvals found" description="You have no pending approval requests." />
              </td></tr></tbody>
            ) : (
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800/50">
                {items.map((a: any) => (
                  <tr key={a.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-5 py-3.5 font-semibold text-slate-800 dark:text-slate-200">{a.prNumber ?? a.id}</td>
                    <td className="px-5 py-3.5 text-slate-600 dark:text-slate-400 max-w-[180px] truncate">{a.prTitle ?? '—'}</td>
                    <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">{a.approverName ?? '—'}</td>
                    <td className="px-5 py-3.5"><StatusBadge status={a.status} /></td>
                    <td className="px-5 py-3.5 text-slate-400 text-xs">{formatDate(a.createdAt)}</td>
                    <td className="px-5 py-3.5">
                      {a.status === 'PENDING' && (
                        <div className="flex items-center gap-2">
                          <input type="text" placeholder="Comment…"
                            value={comments[a.id] ?? ''}
                            onChange={e => setComments(p => ({ ...p, [a.id]: e.target.value }))}
                            className="h-7 w-28 rounded border border-slate-300 px-2 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
                          />
                          <Button size="icon-sm" variant="success"
                            onClick={() => approveMut.mutate({ id: a.id, c: comments[a.id] ?? '' })}
                            disabled={approveMut.isPending} aria-label="Approve">
                            <ThumbsUp className="h-3.5 w-3.5" />
                          </Button>
                          <Button size="icon-sm" variant="danger-outline"
                            onClick={() => rejectMut.mutate({ id: a.id, c: comments[a.id] ?? '' })}
                            disabled={rejectMut.isPending} aria-label="Reject">
                            <ThumbsDown className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            )}
          </table>
        </div>
        {totalPages > 1 && (
          <div className="border-t border-slate-100 dark:border-slate-800 px-5 py-3">
            <Pagination page={page} totalPages={totalPages} totalElements={totalElements} size={PAGE_SIZE} onPageChange={setPage} />
          </div>
        )}
      </Card>
    </div>
  )
}
