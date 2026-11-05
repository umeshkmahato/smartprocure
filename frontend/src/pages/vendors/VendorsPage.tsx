import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { vendorsApi } from '@/api/vendors'
import StatusBadge from '@/components/ui/StatusBadge'
import SortableHeader from '@/components/ui/SortableHeader'
import Pagination from '@/components/ui/Pagination'
import type { SortState } from '@/types'
import { Plus, Search } from 'lucide-react'
import AiRecommendations from '@/components/ai/AiRecommendations'

export default function VendorsPage() {
  const qc = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [status,  setStatus]  = useState('')
  const [page,    setPage]    = useState(0)
  const [sort,    setSort]    = useState<SortState>({ field: 'name', dir: 'asc' })

  const handleSort = (field: string) => {
    setSort(prev => ({ field, dir: prev.field === field && prev.dir === 'asc' ? 'desc' : 'asc' }))
    setPage(0)
  }

  const { data, isLoading } = useQuery({
    queryKey: ['vendors', keyword, status, page, sort],
    queryFn: () => vendorsApi.list({
      keyword: keyword || undefined,
      status:  status  || undefined,
      page, size: 20,
      sort: sort.field, dir: sort.dir,
    }),
  })

  const approve = useMutation({
    mutationFn: (id: number) => vendorsApi.approve(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vendors'] }),
  })

  return (
    <div className="space-y-5">
      <AiRecommendations />

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Vendors</h1>
          <p className="text-slate-500 text-sm mt-0.5">{data?.totalElements ?? 0} vendors registered</p>
        </div>
        <Link to="/vendors/new" className="inline-flex items-center gap-1.5 bg-blue-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-700">
          <Plus size={15} /> Add Vendor
        </Link>
      </div>

      {/* Filters */}
      <div className="flex gap-3 flex-wrap">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input placeholder="Search vendors…" value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(0) }}
            className="pl-9 pr-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-60" />
        </div>
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option value="">All statuses</option>
          {['PENDING','APPROVED','SUSPENDED','BLACKLISTED'].map(s => <option key={s}>{s}</option>)}
        </select>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border overflow-hidden">
        {isLoading ? (
          <p className="p-6 text-sm text-slate-400">Loading…</p>
        ) : !data?.content.length ? (
          <p className="p-6 text-sm text-slate-400">No vendors found.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50">
              <tr>
                <SortableHeader label="Name"     field="name"      currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Email"    field="email"     currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Country"  field="country"   currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Category" field="category"  currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Status"   field="status"    currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <th className="px-5 py-3 text-left text-xs font-medium uppercase text-slate-500">Rating</th>
                <th className="px-5 py-3 text-left text-xs font-medium uppercase text-slate-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.content.map((v) => (
                <tr key={v.id} className="hover:bg-slate-50">
                  <td className="px-5 py-3">
                    <Link to={`/vendors/${v.id}`} className="font-medium text-blue-600 hover:underline">{v.name}</Link>
                  </td>
                  <td className="px-5 py-3 text-slate-600">{v.email}</td>
                  <td className="px-5 py-3 text-slate-600">{v.country ?? '—'}</td>
                  <td className="px-5 py-3 text-slate-600">{v.category ?? '—'}</td>
                  <td className="px-5 py-3"><StatusBadge status={v.status} /></td>
                  <td className="px-5 py-3 text-slate-600">{v.rating != null ? `${v.rating}/5` : '—'}</td>
                  <td className="px-5 py-3">
                    {v.status === 'PENDING' && (
                      <button onClick={() => approve.mutate(v.id)}
                        className="text-xs font-medium text-green-700 bg-green-50 hover:bg-green-100 px-2.5 py-1 rounded-md">
                        Approve
                      </button>
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
