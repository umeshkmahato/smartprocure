import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { quotationsApi } from '@/api/quotations'
import StatusBadge from '@/components/ui/StatusBadge'
import SortableHeader from '@/components/ui/SortableHeader'
import Pagination from '@/components/ui/Pagination'
import type { SortState } from '@/types'
import { formatDate } from '@/lib/utils'
import { Plus } from 'lucide-react'

export default function QuotationsPage() {
  const qc = useQueryClient()
  const [status, setStatus] = useState('')
  const [page,   setPage]   = useState(0)
  const [sort,   setSort]   = useState<SortState>({ field: 'createdAt', dir: 'desc' })

  const handleSort = (field: string) => {
    setSort(prev => ({ field, dir: prev.field === field && prev.dir === 'desc' ? 'asc' : 'desc' }))
    setPage(0)
  }

  const { data, isLoading } = useQuery({
    queryKey: ['rfqs', status, page, sort],
    queryFn: () => quotationsApi.listRfqs({
      status: status || undefined,
      page, size: 20,
      sort: sort.field, dir: sort.dir,
    }),
  })

  const publish = useMutation({
    mutationFn: (id: number) => quotationsApi.publishRfq(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rfqs'] }),
  })
  const close = useMutation({
    mutationFn: (id: number) => quotationsApi.closeRfq(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['rfqs'] }),
  })

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Quotations / RFQs</h1>
          <p className="text-slate-500 text-sm mt-0.5">{data?.totalElements ?? 0} RFQs total</p>
        </div>
        <div className="flex items-center gap-3">
          <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}
            className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All statuses</option>
            {['DRAFT','PUBLISHED','CLOSED','AWARDED'].map(s => <option key={s}>{s}</option>)}
          </select>
          <Link to="/quotations/new"
            className="inline-flex items-center gap-1.5 bg-blue-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-700">
            <Plus size={15} /> New RFQ
          </Link>
        </div>
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        {isLoading ? <p className="p-6 text-sm text-slate-400">Loading…</p>
          : !data?.content.length ? <p className="p-6 text-sm text-slate-400">No RFQs found.</p>
          : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50">
              <tr>
                <SortableHeader label="RFQ Number" field="rfqNumber" currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Title"      field="title"     currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Status"     field="status"    currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Deadline"   field="deadline"  currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Created"    field="createdAt" currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <th className="px-5 py-3 text-left text-xs font-medium uppercase text-slate-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.content.map((rfq) => (
                <tr key={rfq.id} className="hover:bg-slate-50">
                  <td className="px-5 py-3">
                    <Link to={`/quotations/${rfq.id}`}
                      className="font-medium text-blue-600 hover:underline">
                      {rfq.rfqNumber}
                    </Link>
                  </td>
                  <td className="px-5 py-3 text-slate-800">
                    <Link to={`/quotations/${rfq.id}`} className="hover:text-blue-600">
                      {rfq.title}
                    </Link>
                  </td>
                  <td className="px-5 py-3"><StatusBadge status={rfq.status} /></td>
                  <td className="px-5 py-3 text-slate-500">{formatDate(rfq.deadline)}</td>
                  <td className="px-5 py-3 text-slate-400">{formatDate(rfq.createdAt)}</td>
                  <td className="px-5 py-3">
                    <div className="flex gap-2">
                      <Link to={`/quotations/${rfq.id}`}
                        className="text-xs font-medium text-blue-600 hover:underline mr-2">
                        View
                      </Link>
                      {rfq.status === 'DRAFT' && (
                        <button onClick={() => publish.mutate(rfq.id)}
                          className="text-xs font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 px-2.5 py-1 rounded-md">
                          Publish
                        </button>
                      )}
                      {rfq.status === 'PUBLISHED' && (
                        <button onClick={() => close.mutate(rfq.id)}
                          className="text-xs font-medium bg-slate-100 text-slate-700 hover:bg-slate-200 px-2.5 py-1 rounded-md">
                          Close
                        </button>
                      )}
                    </div>
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
