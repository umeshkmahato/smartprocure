import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { Plus, Search, X, Receipt } from 'lucide-react'
import { invoicesApi } from '@/api/invoices'
import StatusBadge from '@/components/ui/StatusBadge'
import Pagination from '@/components/ui/Pagination'
import SortableHeader from '@/components/ui/SortableHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card } from '@/components/ui/card'
import { TableSkeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/ui/empty-state'
import { ErrorState } from '@/components/ui/error-state'
import { formatDate, formatCurrency } from '@/lib/utils'

const PAGE_SIZE = 10

export default function InvoicesPage() {
  const navigate = useNavigate()
  const [page, setPage]              = useState(0)
  const [keyword, setKeyword]        = useState('')
  const [appliedKeyword, setApplied] = useState('')
  const [sortField, setSortField]    = useState('createdAt')
  const [sortDir, setSortDir]        = useState<'asc'|'desc'>('desc')
  const [statusFilter, setStatus]    = useState('')

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['invoices', page, appliedKeyword, sortField, sortDir, statusFilter],
    queryFn: () => invoicesApi.list({
      page, size: PAGE_SIZE,
      keyword: appliedKeyword || undefined,
      sort: sortField, dir: sortDir,
      status: statusFilter || undefined,
    }),
  })

  const handleSort = (field: string) => {
    if (sortField === field) setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    else { setSortField(field); setSortDir('asc') }
    setPage(0)
  }

  const handleSearch = (e: React.FormEvent) => { e.preventDefault(); setApplied(keyword); setPage(0) }
  const clearSearch  = () => { setKeyword(''); setApplied(''); setPage(0) }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Invoices</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
            {data ? `${data.totalElements} invoice${data.totalElements !== 1 ? 's' : ''}` : 'Manage and verify supplier invoices'}
          </p>
        </div>
        <Button onClick={() => navigate('/invoices/new')} size="sm">
          <Plus className="h-4 w-4" /> New Invoice
        </Button>
      </div>

      <Card>
        <div className="flex flex-wrap gap-3 p-4">
          <form onSubmit={handleSearch} className="flex gap-2 flex-1 min-w-[220px]">
            <div className="relative flex-1">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400 pointer-events-none" />
              <Input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="Search invoices…" className="pl-8" />
            </div>
            {appliedKeyword && (
              <Button type="button" variant="ghost" size="icon" onClick={clearSearch}><X className="h-4 w-4"/></Button>
            )}
            <Button type="submit" size="sm">Search</Button>
          </form>
          <select value={statusFilter} onChange={e => { setStatus(e.target.value); setPage(0) }}
            className="h-9 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200">
            <option value="">All statuses</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="UNDER_REVIEW">Under Review</option>
            <option value="VERIFIED">Verified</option>
            <option value="PAID">Paid</option>
            <option value="DISPUTED">Disputed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </Card>

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 dark:border-slate-800 bg-slate-50/60 dark:bg-slate-800/40">
                {([['invoiceNumber','Invoice #'],['vendorName','Vendor'],['poNumber','PO Number'],['status','Status'],['totalAmount','Amount'],['invoiceDate','Date']] as [string,string][]).map(([f,l]) => (
                  <SortableHeader key={f} field={f} label={l} currentSort={sortField} currentDir={sortDir} onSort={handleSort} />
                ))}
              </tr>
            </thead>
            {isLoading ? (
              <TableSkeleton rows={8} cols={6} />
            ) : isError ? (
              <tbody><tr><td colSpan={6}><ErrorState onRetry={refetch} /></td></tr></tbody>
            ) : !data?.content.length ? (
              <tbody><tr><td colSpan={6}>
                <EmptyState icon={Receipt} title="No invoices found"
                  description={appliedKeyword ? 'Try adjusting your search.' : 'No invoices submitted yet.'}
                  action={{ label: 'New Invoice', onClick: () => navigate('/invoices/new') }} />
              </td></tr></tbody>
            ) : (
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800/50">
                {data.content.map(inv => (
                  <tr key={inv.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/30 transition-colors">
                    <td className="px-5 py-3.5">
                      <Link to={`/invoices/${inv.id}`} className="font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400">{inv.invoiceNumber}</Link>
                    </td>
                    <td className="px-5 py-3.5 text-slate-600 dark:text-slate-400">{inv.vendorName}</td>
                    <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">{inv.poNumber}</td>
                    <td className="px-5 py-3.5"><StatusBadge status={inv.status} /></td>
                    <td className="px-5 py-3.5 font-medium text-slate-800 dark:text-slate-200">{formatCurrency(inv.totalAmount, inv.currency)}</td>
                    <td className="px-5 py-3.5 text-slate-400 text-xs">{formatDate(inv.invoiceDate)}</td>
                  </tr>
                ))}
              </tbody>
            )}
          </table>
        </div>
        {data && data.totalPages > 1 && (
          <div className="border-t border-slate-100 dark:border-slate-800 px-5 py-3">
            <Pagination page={page} totalPages={data.totalPages} totalElements={data.totalElements} size={PAGE_SIZE} onPageChange={setPage} />
          </div>
        )}
      </Card>
    </div>
  )
}
