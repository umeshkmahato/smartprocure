import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { purchaseRequestsApi } from '@/api/purchaseRequests'
import { purchaseOrdersApi } from '@/api/purchaseOrders'
import { invoicesApi } from '@/api/invoices'
import { approvalsApi } from '@/api/approvals'
import { formatCurrency } from '@/lib/utils'
import { FileText, ShoppingCart, Receipt, CheckSquare } from 'lucide-react'

function StatCard({ title, value, sub, icon: Icon, to, color }: {
  title: string; value: string | number; sub?: string
  icon: React.ElementType; to: string; color: string
}) {
  return (
    <Link to={to} className="bg-white rounded-xl border p-5 flex items-center gap-4 hover:shadow-md transition-shadow">
      <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${color}`}>
        <Icon size={22} className="text-white" />
      </div>
      <div>
        <p className="text-sm text-slate-500">{title}</p>
        <p className="text-2xl font-bold text-slate-900">{value}</p>
        {sub && <p className="text-xs text-slate-400">{sub}</p>}
      </div>
    </Link>
  )
}

export default function DashboardPage() {
  const { data: prs }       = useQuery({ queryKey: ['prs-dash'],       queryFn: () => purchaseRequestsApi.list({ page: 0, size: 1 }) })
  const { data: pos }       = useQuery({ queryKey: ['pos-dash'],       queryFn: () => purchaseOrdersApi.list({ page: 0, size: 1 }) })
  const { data: invs }      = useQuery({ queryKey: ['invs-dash'],      queryFn: () => invoicesApi.list({ page: 0, size: 1 }) })
  const { data: approvals } = useQuery({ queryKey: ['approvals-dash'], queryFn: () => approvalsApi.myPending() })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
        <p className="text-slate-500 text-sm mt-1">Welcome back — here's what needs your attention.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard title="Purchase Requests" value={prs?.totalElements ?? '—'}
          icon={FileText} to="/purchase-requests" color="bg-blue-500" />
        <StatCard title="Purchase Orders" value={pos?.totalElements ?? '—'}
          icon={ShoppingCart} to="/purchase-orders" color="bg-teal-500" />
        <StatCard title="Invoices" value={invs?.totalElements ?? '—'}
          icon={Receipt} to="/invoices" color="bg-violet-500" />
        <StatCard title="Pending Approvals" value={approvals?.totalElements ?? '—'}
          sub="awaiting your action"
          icon={CheckSquare} to="/approvals" color="bg-orange-500" />
      </div>

      {/* Recent POs */}
      <div className="bg-white rounded-xl border">
        <div className="px-5 py-4 border-b flex items-center justify-between">
          <h2 className="font-semibold text-slate-800">Recent Purchase Orders</h2>
          <Link to="/purchase-orders" className="text-sm text-blue-600 hover:underline">View all</Link>
        </div>
        <RecentPOs />
      </div>

      {/* Recent Invoices */}
      <div className="bg-white rounded-xl border">
        <div className="px-5 py-4 border-b flex items-center justify-between">
          <h2 className="font-semibold text-slate-800">Recent Invoices</h2>
          <Link to="/invoices" className="text-sm text-blue-600 hover:underline">View all</Link>
        </div>
        <RecentInvoices />
      </div>
    </div>
  )
}

function RecentPOs() {
  const { data, isLoading } = useQuery({
    queryKey: ['pos-recent'],
    queryFn: () => purchaseOrdersApi.list({ page: 0, size: 5 }),
  })
  if (isLoading) return <p className="p-5 text-sm text-slate-400">Loading…</p>
  if (!data?.content.length) return <p className="p-5 text-sm text-slate-400">No purchase orders yet.</p>
  return (
    <table className="w-full text-sm">
      <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
        <tr>
          {['PO Number','Vendor','Status','Total','Date'].map(h => (
            <th key={h} className="px-5 py-3 text-left font-medium">{h}</th>
          ))}
        </tr>
      </thead>
      <tbody className="divide-y">
        {data.content.map((po) => (
          <tr key={po.id} className="hover:bg-slate-50">
            <td className="px-5 py-3">
              <Link to={`/purchase-orders/${po.id}`} className="text-blue-600 hover:underline font-medium">{po.poNumber}</Link>
            </td>
            <td className="px-5 py-3 text-slate-600">{po.vendorName}</td>
            <td className="px-5 py-3"><span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs">{po.status}</span></td>
            <td className="px-5 py-3">{formatCurrency(po.totalAmount, po.currency)}</td>
            <td className="px-5 py-3 text-slate-400">{new Date(po.createdAt).toLocaleDateString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function RecentInvoices() {
  const { data, isLoading } = useQuery({
    queryKey: ['invs-recent'],
    queryFn: () => invoicesApi.list({ page: 0, size: 5 }),
  })
  if (isLoading) return <p className="p-5 text-sm text-slate-400">Loading…</p>
  if (!data?.content.length) return <p className="p-5 text-sm text-slate-400">No invoices yet.</p>
  return (
    <table className="w-full text-sm">
      <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
        <tr>
          {['Invoice #','Vendor','PO Number','Status','Amount'].map(h => (
            <th key={h} className="px-5 py-3 text-left font-medium">{h}</th>
          ))}
        </tr>
      </thead>
      <tbody className="divide-y">
        {data.content.map((inv) => (
          <tr key={inv.id} className="hover:bg-slate-50">
            <td className="px-5 py-3">
              <Link to={`/invoices/${inv.id}`} className="text-blue-600 hover:underline font-medium">{inv.invoiceNumber}</Link>
            </td>
            <td className="px-5 py-3 text-slate-600">{inv.vendorName}</td>
            <td className="px-5 py-3 text-slate-600">{inv.poNumber}</td>
            <td className="px-5 py-3"><span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs">{inv.status}</span></td>
            <td className="px-5 py-3">{formatCurrency(inv.totalAmount, inv.currency)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
