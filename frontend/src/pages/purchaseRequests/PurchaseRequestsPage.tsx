import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { purchaseRequestsApi } from '@/api/purchaseRequests'
import StatusBadge from '@/components/ui/StatusBadge'
import SortableHeader from '@/components/ui/SortableHeader'
import Pagination from '@/components/ui/Pagination'
import type { SortState } from '@/types'
import { formatCurrency, formatDate } from '@/lib/utils'
import { Plus, Search } from 'lucide-react'

export default function PurchaseRequestsPage() {
  const [keyword, setKeyword] = useState('')
  const [status,  setStatus]  = useState('')
  const [page,    setPage]    = useState(0)
  const [sort,    setSort]    = useState<SortState>({ field: 'createdAt', dir: 'desc' })

  const handleSort = (field: string) => {
    setSort(prev => ({ field, dir: prev.field === field && prev.dir === 'desc' ? 'asc' : 'desc' }))
    setPage(0)
  }

  const { data, isLoading } = useQuery({
    queryKey: ['prs', keyword, status, page, sort],
    queryFn: () => purchaseRequestsApi.list({
      keyword: keyword || undefined,
      status:  status  || undefined,
      page, size: 20,
      sort: sort.field, dir: sort.dir,
    }),
  })

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Purchase Requests</h1>
          <p className="text-slate-500 text-sm mt-0.5">{data?.totalElements ?? 0} requests total</p>
        </div>
        <Link to="/purchase-requests/new"
          className="inline-flex items-center gap-1.5 bg-blue-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-700">
          <Plus size={15} /> New Request
        </Link>
      </div>

      <div className="flex gap-3 flex-wrap">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input placeholder="Search requests…" value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0) }}
            className="pl-9 pr-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-60" />
        </div>
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option value="">All statuses</option>
          {['DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED','CANCELLED'].map(s => <option key={s}>{s}</option>)}
        </select>
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        {isLoading ? <p className="p-6 text-sm text-slate-400">Loading…</p>
          : !data?.content.length ? <p className="p-6 text-sm text-slate-400">No purchase requests found.</p>
          : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50">
              <tr>
                <SortableHeader label="PR Number"      field="prNumber"       currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Title"          field="title"          currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <th className="px-5 py-3 text-left text-xs font-medium uppercase text-slate-500">Department</th>
                <SortableHeader label="Status"         field="status"         currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Est. Total"     field="estimatedTotal" currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Required Date"  field="requiredDate"   currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Created"        field="createdAt"      currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.content.map((pr) => (
                <tr key={pr.id} className="hover:bg-slate-50">
                  <td className="px-5 py-3">
                    <Link to={`/purchase-requests/${pr.id}`} className="font-medium text-blue-600 hover:underline">{pr.prNumber}</Link>
                  </td>
                  <td className="px-5 py-3 text-slate-800 font-medium">{pr.title}</td>
                  <td className="px-5 py-3 text-slate-500">{pr.department ?? '—'}</td>
                  <td className="px-5 py-3"><StatusBadge status={pr.status} /></td>
                  <td className="px-5 py-3">{formatCurrency(pr.estimatedTotal, pr.currency)}</td>
                  <td className="px-5 py-3 text-slate-500">{formatDate(pr.requiredDate)}</td>
                  <td className="px-5 py-3 text-slate-400">{formatDate(pr.createdAt)}</td>
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
