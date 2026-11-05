import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import {
  FileText, ShoppingCart, Receipt, CheckSquare,
  Building2, TrendingUp, TrendingDown, ArrowRight, Clock,
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { KpiSkeleton, Skeleton, TableSkeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/ui/empty-state'
import { purchaseRequestsApi } from '@/api/purchaseRequests'
import { purchaseOrdersApi } from '@/api/purchaseOrders'
import { invoicesApi } from '@/api/invoices'
import { approvalsApi } from '@/api/approvals'
import { vendorsApi } from '@/api/vendors'
import { formatCurrency, cn } from '@/lib/utils'

// ── Demo trend data — replace with real backend analytics API ─────────────────
const activityTrend = [
  { month: 'Aug', orders: 12, invoices: 8  },
  { month: 'Sep', orders: 19, invoices: 14 },
  { month: 'Oct', orders: 15, invoices: 11 },
  { month: 'Nov', orders: 23, invoices: 18 },
  { month: 'Dec', orders: 28, invoices: 22 },
  { month: 'Jan', orders: 32, invoices: 26 },
]

const statusDist = [
  { name: 'Draft',     value: 14 },
  { name: 'Pending',   value: 28 },
  { name: 'Approved',  value: 42 },
  { name: 'Rejected',  value: 7  },
  { name: 'Completed', value: 55 },
]

// ── KPI Card ──────────────────────────────────────────────────────────────────
interface KpiCardProps {
  title: string
  value: string | number
  subtitle?: string
  icon: React.ElementType
  iconColor: string
  iconBg: string
  trend?: { value: number; label: string }
  to: string
}

function KpiCard({ title, value, subtitle, icon: Icon, iconColor, iconBg, trend, to }: KpiCardProps) {
  const isPositive = (trend?.value ?? 0) >= 0
  return (
    <Link to={to} className="group block">
      <Card className="transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 cursor-pointer">
        <CardContent className="p-5">
          <div className="flex items-start justify-between mb-4">
            <div className={cn('flex h-10 w-10 items-center justify-center rounded-lg', iconBg)}>
              <Icon className={cn('h-5 w-5', iconColor)} />
            </div>
            {trend && (
              <span className={cn(
                'flex items-center gap-1 text-xs font-semibold rounded-full px-2 py-0.5',
                isPositive
                  ? 'text-emerald-700 bg-emerald-50 dark:text-emerald-300 dark:bg-emerald-900/30'
                  : 'text-red-700 bg-red-50 dark:text-red-300 dark:bg-red-900/30',
              )}>
                {isPositive
                  ? <TrendingUp className="h-3 w-3" />
                  : <TrendingDown className="h-3 w-3" />}
                {Math.abs(trend.value)}%
              </span>
            )}
          </div>
          <p className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">{value}</p>
          <p className="text-sm font-medium text-slate-600 dark:text-slate-400 mt-0.5">{title}</p>
          {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
          {trend && <p className="text-xs text-slate-400 mt-1">{trend.label}</p>}
        </CardContent>
      </Card>
    </Link>
  )
}

// ── Status badge ──────────────────────────────────────────────────────────────
function SBadge({ status }: { status: string }) {
  const s = status.toUpperCase()
  if (['APPROVED','COMPLETED','PAID','VERIFIED'].includes(s)) return <Badge variant="success">{status}</Badge>
  if (['PENDING','SUBMITTED','UNDER_REVIEW'].includes(s))     return <Badge variant="warning">{status}</Badge>
  if (['REJECTED','CANCELLED','OVERDUE'].includes(s))         return <Badge variant="danger">{status}</Badge>
  return <Badge variant="secondary">{status}</Badge>
}

// ── Recent POs ────────────────────────────────────────────────────────────────
function RecentPOs() {
  const { data, isLoading } = useQuery({
    queryKey: ['pos-recent'],
    queryFn: () => purchaseOrdersApi.list({ page: 0, size: 5 }),
  })
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-slate-100 dark:border-slate-800">
            {['PO Number','Vendor','Status','Total','Date'].map(h => (
              <th key={h} className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-400">{h}</th>
            ))}
          </tr>
        </thead>
        {isLoading ? <TableSkeleton rows={4} cols={5} /> : !data?.content.length ? (
          <tbody><tr><td colSpan={5}><EmptyState icon={ShoppingCart} title="No purchase orders yet" /></td></tr></tbody>
        ) : (
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800/50">
            {data.content.map(po => (
              <tr key={po.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                <td className="px-5 py-3.5">
                  <Link to={`/purchase-orders/${po.id}`} className="font-semibold text-blue-600 hover:text-blue-700">{po.poNumber}</Link>
                </td>
                <td className="px-5 py-3.5 text-slate-600 dark:text-slate-400">{po.vendorName}</td>
                <td className="px-5 py-3.5"><SBadge status={po.status} /></td>
                <td className="px-5 py-3.5 font-medium text-slate-800 dark:text-slate-200">{formatCurrency(po.totalAmount, po.currency)}</td>
                <td className="px-5 py-3.5 text-slate-400 text-xs">{new Date(po.createdAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        )}
      </table>
    </div>
  )
}

// ── Recent Invoices ───────────────────────────────────────────────────────────
function RecentInvoices() {
  const { data, isLoading } = useQuery({
    queryKey: ['invs-recent'],
    queryFn: () => invoicesApi.list({ page: 0, size: 5 }),
  })
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-slate-100 dark:border-slate-800">
            {['Invoice #','Vendor','PO Number','Status','Amount'].map(h => (
              <th key={h} className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-400">{h}</th>
            ))}
          </tr>
        </thead>
        {isLoading ? <TableSkeleton rows={4} cols={5} /> : !data?.content.length ? (
          <tbody><tr><td colSpan={5}><EmptyState icon={Receipt} title="No invoices yet" /></td></tr></tbody>
        ) : (
          <tbody className="divide-y divide-slate-50 dark:divide-slate-800/50">
            {data.content.map(inv => (
              <tr key={inv.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                <td className="px-5 py-3.5">
                  <Link to={`/invoices/${inv.id}`} className="font-semibold text-blue-600 hover:text-blue-700">{inv.invoiceNumber}</Link>
                </td>
                <td className="px-5 py-3.5 text-slate-600 dark:text-slate-400">{inv.vendorName}</td>
                <td className="px-5 py-3.5 text-slate-500">{inv.poNumber}</td>
                <td className="px-5 py-3.5"><SBadge status={inv.status} /></td>
                <td className="px-5 py-3.5 font-medium text-slate-800 dark:text-slate-200">{formatCurrency(inv.totalAmount, inv.currency)}</td>
              </tr>
            ))}
          </tbody>
        )}
      </table>
    </div>
  )
}

// ── Pending Approvals ─────────────────────────────────────────────────────────
function PendingApprovalsList() {
  const { data, isLoading } = useQuery({
    queryKey: ['approvals-recent'],
    queryFn: () => approvalsApi.myPending(),
  })
  if (isLoading) return (
    <div className="p-5 space-y-3">
      {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-12 w-full rounded-lg" />)}
    </div>
  )
  const items = (data as any)?.content ?? []
  if (!items.length) return <EmptyState icon={CheckSquare} title="No pending approvals" description="You're all caught up." className="py-10" />
  return (
    <ul className="divide-y divide-slate-50 dark:divide-slate-800/50">
      {items.slice(0, 5).map((a: any) => (
        <li key={a.id} className="flex items-center gap-3 px-5 py-3.5 hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
          <Clock className="h-4 w-4 text-amber-500 shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-slate-800 dark:text-slate-200 truncate">{a.referenceNumber ?? a.id}</p>
            <p className="text-xs text-slate-400">{a.type ?? 'Approval request'}</p>
          </div>
          <SBadge status={a.status ?? 'PENDING'} />
        </li>
      ))}
    </ul>
  )
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const { data: prs,      isLoading: lPrs  } = useQuery({ queryKey: ['prs-dash'],      queryFn: () => purchaseRequestsApi.list({ page: 0, size: 1 }) })
  const { data: pos,      isLoading: lPos  } = useQuery({ queryKey: ['pos-dash'],      queryFn: () => purchaseOrdersApi.list({ page: 0, size: 1 }) })
  const { data: invs,     isLoading: lInvs } = useQuery({ queryKey: ['invs-dash'],     queryFn: () => invoicesApi.list({ page: 0, size: 1 }) })
  const { data: approvals,isLoading: lAppr } = useQuery({ queryKey: ['approvals-dash'],queryFn: () => approvalsApi.myPending() })
  const { data: vendors,  isLoading: lVend } = useQuery({ queryKey: ['vendors-dash'],  queryFn: () => vendorsApi.list({ page: 0, size: 1 }) })

  const kpiLoading = lPrs || lPos || lInvs || lAppr || lVend

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Executive Dashboard</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Platform overview — procurement, finance &amp; operations</p>
        </div>
        <Button variant="outline" size="sm" asChild>
          <Link to="/purchase-requests/new">New Request</Link>
        </Button>
      </div>

      {/* KPI Cards */}
      {kpiLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
          {[...Array(5)].map((_, i) => <KpiSkeleton key={i} />)}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
          <KpiCard title="Total Vendors"       value={vendors?.totalElements ?? 0}
            icon={Building2}    iconBg="bg-blue-50 dark:bg-blue-900/30"   iconColor="text-blue-600 dark:text-blue-400"
            trend={{ value: 12, label: 'vs last month' }} to="/vendors" />
          <KpiCard title="Purchase Requests"  value={prs?.totalElements ?? 0}
            icon={FileText}     iconBg="bg-violet-50 dark:bg-violet-900/30" iconColor="text-violet-600 dark:text-violet-400"
            trend={{ value: 8,  label: 'vs last month' }} to="/purchase-requests" />
          <KpiCard title="Purchase Orders"    value={pos?.totalElements ?? 0}
            icon={ShoppingCart} iconBg="bg-teal-50 dark:bg-teal-900/30"   iconColor="text-teal-600 dark:text-teal-400"
            trend={{ value: -3, label: 'vs last month' }} to="/purchase-orders" />
          <KpiCard title="Invoices"           value={invs?.totalElements ?? 0}
            icon={Receipt}      iconBg="bg-orange-50 dark:bg-orange-900/30" iconColor="text-orange-600 dark:text-orange-400"
            trend={{ value: 5,  label: 'vs last month' }} to="/invoices" />
          <KpiCard title="Pending Approvals"  value={(approvals as any)?.totalElements ?? 0}
            subtitle="Awaiting your action"
            icon={CheckSquare}  iconBg="bg-amber-50 dark:bg-amber-900/30"  iconColor="text-amber-600 dark:text-amber-400"
            to="/approvals" />
        </div>
      )}

      {/* Charts */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
        <Card className="xl:col-span-2">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <CardTitle>Business Activity Trend</CardTitle>
              <span className="text-xs text-amber-700 bg-amber-50 dark:bg-amber-900/30 dark:text-amber-400 px-2 py-0.5 rounded-full font-medium border border-amber-200 dark:border-amber-800">
                Demo data — connect analytics API
              </span>
            </div>
          </CardHeader>
          <CardContent className="pt-0">
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={activityTrend} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="gOrders" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.15}/>
                    <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="gInvoices" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.15}/>
                    <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false}/>
                <XAxis dataKey="month" tick={{ fontSize:12, fill:'#94a3b8' }} axisLine={false} tickLine={false}/>
                <YAxis tick={{ fontSize:12, fill:'#94a3b8' }} axisLine={false} tickLine={false}/>
                <Tooltip contentStyle={{ borderRadius:'8px', border:'1px solid #e2e8f0', fontSize:'12px' }} cursor={{ stroke:'#e2e8f0' }}/>
                <Area type="monotone" dataKey="orders"   stroke="#3B82F6" strokeWidth={2} fill="url(#gOrders)"   name="Orders"   dot={false}/>
                <Area type="monotone" dataKey="invoices" stroke="#8B5CF6" strokeWidth={2} fill="url(#gInvoices)" name="Invoices" dot={false}/>
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-4">
            <CardTitle>Order Status Distribution</CardTitle>
          </CardHeader>
          <CardContent className="pt-0">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={statusDist} layout="vertical" margin={{ top:0, right:4, left:20, bottom:0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" horizontal={false}/>
                <XAxis type="number" tick={{ fontSize:12, fill:'#94a3b8' }} axisLine={false} tickLine={false}/>
                <YAxis type="category" dataKey="name" tick={{ fontSize:12, fill:'#94a3b8' }} axisLine={false} tickLine={false} width={65}/>
                <Tooltip contentStyle={{ borderRadius:'8px', border:'1px solid #e2e8f0', fontSize:'12px' }} cursor={{ fill:'#f8fafc' }}/>
                <Bar dataKey="value" name="Count" radius={[0,4,4,0]} fill="#3B82F6" fillOpacity={0.85}/>
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      {/* Tables row */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
        <Card className="xl:col-span-2">
          <CardHeader className="pb-0">
            <div className="flex items-center justify-between">
              <CardTitle>Recent Purchase Orders</CardTitle>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/purchase-orders" className="flex items-center gap-1 text-xs">View all <ArrowRight className="h-3.5 w-3.5"/></Link>
              </Button>
            </div>
          </CardHeader>
          <CardContent className="p-0 mt-3"><RecentPOs /></CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-0">
            <div className="flex items-center justify-between">
              <CardTitle>Pending Approvals</CardTitle>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/approvals" className="flex items-center gap-1 text-xs">View all <ArrowRight className="h-3.5 w-3.5"/></Link>
              </Button>
            </div>
          </CardHeader>
          <CardContent className="p-0 mt-3"><PendingApprovalsList /></CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="pb-0">
          <div className="flex items-center justify-between">
            <CardTitle>Recent Invoices</CardTitle>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/invoices" className="flex items-center gap-1 text-xs">View all <ArrowRight className="h-3.5 w-3.5"/></Link>
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-0 mt-3"><RecentInvoices /></CardContent>
      </Card>
    </div>
  )
}
