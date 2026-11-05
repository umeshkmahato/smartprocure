import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { purchaseOrdersApi } from '@/api/purchaseOrders'
import StatusBadge from '@/components/ui/StatusBadge'
import SortableHeader from '@/components/ui/SortableHeader'
import Pagination from '@/components/ui/Pagination'
import type { SortState } from '@/types'
import { formatCurrency, formatDate } from '@/lib/utils'
import { Plus, Search } from 'lucide-react'

export default function PurchaseOrdersPage() {
  const [keyword, setKeyword] = useState('')
  const [status,  setStatus]  = useState('')
  const [page,    setPage]    = useState(0)
  const [sort,    setSort]    = useState<SortState>({ field: 'createdAt', dir: 'desc' })

  const handleSort = (field: string) => {
    setSort(prev => ({ field, dir: prev.field === field && prev.dir === 'desc' ? 'asc' : 'desc' }))
    setPage(0)
  }

  const { data, isLoading } = useQuery({
    queryKey: ['pos', keyword, status, page, sort],
    queryFn: () => purchaseOrdersApi.list({
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
          <h1 className="text-2xl font-bold text-slate-900">Purchase Orders</h1>
          <p className="text-slate-500 text-sm mt-0.5">{data?.totalElements ?? 0} orders total</p>
        </div>
        <Link to="/purchase-orders/new"
          className="inline-flex items-center gap-1.5 bg-blue-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-700">
          <Plus size={15} /> New PO
        </Link>
      </div>

      <div className="flex gap-3 flex-wrap">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input placeholder="Search POs…" value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0) }}
            className="pl-9 pr-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-60" />
        </div>
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option value="">All statuses</option>
          {['DRAFT','ISSUED','ACKNOWLEDGED','PARTIALLY_DELIVERED','DELIVERED','INVOICED','CLOSED','CANCELLED'].map(s =>
            <option key={s}>{s}</option>)}
        </select>
      </div>

      <div className="bg-white rounded-xl border overflow-hidden">
        {isLoading ? <p className="p-6 text-sm text-slate-400">Loading…</p>
          : !data?.content.length ? <p className="p-6 text-sm text-slate-400">No purchase orders found.</p>
          : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50">
              <tr>
                <SortableHeader label="PO Number"     field="poNumber"     currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <th className="px-5 py-3 text-left text-xs font-medium uppercase text-slate-500">Vendor</th>
                <SortableHeader label="Status"        field="status"       currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Total"         field="totalAmount"  currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Delivery Date" field="deliveryDate" currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <SortableHeader label="Issued At"     field="issuedAt"     currentSort={sort.field} currentDir={sort.dir} onSort={handleSort} />
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.content.map((po) => (
                <tr key={po.id} className="hover:bg-slate-50">
                  <td className="px-5 py-3">
                    <Link to={`/purchase-orders/${po.id}`} className="font-medium text-blue-600 hover:underline">
                      {po.poNumber}
                    </Link>
                  </td>
                  <td className="px-5 py-3 text-slate-700">{po.vendorName}</td>
                  <td className="px-5 py-3"><StatusBadge status={po.status} /></td>
                  <td className="px-5 py-3 font-medium">{formatCurrency(po.totalAmount, po.currency)}</td>
                  <td className="px-5 py-3 text-slate-500">{formatDate(po.deliveryDate)}</td>
                  <td className="px-5 py-3 text-slate-400">{formatDate(po.issuedAt)}</td>
                  <td className="px-5 py-3">
                    <Link to={`/purchase-orders/${po.id}`}
                      className="text-xs font-medium text-blue-600 hover:underline">View</Link>
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
