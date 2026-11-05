import { cn } from '@/lib/utils'

const colorMap: Record<string, string> = {
  // Vendor
  PENDING:            'bg-yellow-100 text-yellow-800',
  APPROVED:           'bg-green-100 text-green-800',
  SUSPENDED:          'bg-orange-100 text-orange-800',
  BLACKLISTED:        'bg-red-100 text-red-800',
  // PR / Invoice shared
  DRAFT:              'bg-slate-100 text-slate-700',
  SUBMITTED:          'bg-blue-100 text-blue-800',
  UNDER_REVIEW:       'bg-purple-100 text-purple-800',
  REJECTED:           'bg-red-100 text-red-800',
  CANCELLED:          'bg-slate-100 text-slate-500',
  // Approval
  DELEGATED:          'bg-indigo-100 text-indigo-800',
  // PO
  ISSUED:             'bg-blue-100 text-blue-800',
  ACKNOWLEDGED:       'bg-cyan-100 text-cyan-800',
  PARTIALLY_DELIVERED:'bg-orange-100 text-orange-800',
  DELIVERED:          'bg-teal-100 text-teal-800',
  INVOICED:           'bg-violet-100 text-violet-800',
  CLOSED:             'bg-slate-100 text-slate-700',
  // Invoice
  VERIFIED:           'bg-green-100 text-green-800',
  DISPUTED:           'bg-red-100 text-red-800',
  PAID:               'bg-emerald-100 text-emerald-800',
  // RFQ
  PUBLISHED:          'bg-blue-100 text-blue-800',
  AWARDED:            'bg-green-100 text-green-800',
  SELECTED:           'bg-green-100 text-green-800',
}

export default function StatusBadge({ status }: { status: string }) {
  return (
    <span className={cn('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
      colorMap[status] ?? 'bg-slate-100 text-slate-700')}>
      {status.replace(/_/g, ' ')}
    </span>
  )
}
