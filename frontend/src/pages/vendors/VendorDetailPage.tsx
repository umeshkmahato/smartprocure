import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { vendorsApi } from '@/api/vendors'
import StatusBadge from '@/components/ui/StatusBadge'
import { Button } from '@/components/ui/button'
import { formatDate } from '@/lib/utils'
import {
  ArrowLeft, Building2, Mail, Phone, MapPin, Globe,
  Hash, Tag, Coins, Star, ShieldCheck,
  ShieldOff, ShieldX,
} from 'lucide-react'

function InfoRow({ icon: Icon, label, value }: {
  icon: React.ElementType; label: string; value?: string | number | null
}) {
  return (
    <div className="flex items-start gap-3 py-3 border-b border-slate-100 dark:border-slate-800 last:border-0">
      <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
        <Icon className="h-3.5 w-3.5 text-slate-500 dark:text-slate-400" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-slate-400 dark:text-slate-500">{label}</p>
        <p className="text-sm font-medium text-slate-800 dark:text-slate-200 truncate">{value ?? '—'}</p>
      </div>
    </div>
  )
}

function StarRating({ rating }: { rating?: number | null }) {
  if (rating == null) return <span className="text-sm text-slate-400">Not rated</span>
  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map(i => (
        <Star key={i} className={`h-4 w-4 ${
          i <= Math.round(rating)
            ? 'fill-amber-400 text-amber-400'
            : 'fill-slate-200 text-slate-200 dark:fill-slate-700 dark:text-slate-700'
        }`} />
      ))}
      <span className="ml-1 text-sm font-medium text-slate-700 dark:text-slate-300">{rating.toFixed(1)}</span>
    </div>
  )
}

export default function VendorDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const vendorId = Number(id)

  const { data: vendor, isLoading } = useQuery({
    queryKey: ['vendor', vendorId],
    queryFn: () => vendorsApi.getById(vendorId),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['vendor', vendorId] })

  const approve   = useMutation({ mutationFn: () => vendorsApi.approve(vendorId),   onSuccess: invalidate })
  const suspend   = useMutation({ mutationFn: () => vendorsApi.suspend(vendorId, 'Suspended by admin'), onSuccess: invalidate })
  const blacklist = useMutation({ mutationFn: () => vendorsApi.blacklist(vendorId, 'Blacklisted by admin'), onSuccess: invalidate })

  if (isLoading) return (
    <div className="flex items-center justify-center h-48">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
    </div>
  )
  if (!vendor) return (
    <div className="rounded-xl border border-red-200 bg-red-50 dark:bg-red-950 dark:border-red-800 p-6 text-sm text-red-600 dark:text-red-400">
      Vendor not found.
    </div>
  )

  const statusColors: Record<string, string> = {
    APPROVED: 'bg-emerald-50 border-emerald-200 dark:bg-emerald-950 dark:border-emerald-800',
    PENDING:  'bg-amber-50  border-amber-200  dark:bg-amber-950  dark:border-amber-800',
    SUSPENDED:'bg-orange-50 border-orange-200 dark:bg-orange-950 dark:border-orange-800',
    BLACKLISTED:'bg-red-50  border-red-200    dark:bg-red-950    dark:border-red-800',
  }

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Back */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 transition-colors"
      >
        <ArrowLeft size={15} /> Back to Vendors
      </button>

      {/* Hero card */}
      <div className={`rounded-xl border p-6 ${statusColors[vendor.status] ?? 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800'}`}>
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 shadow-sm">
              <Building2 className="h-7 w-7 text-blue-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">{vendor.name}</h1>
              <div className="flex flex-wrap items-center gap-3 mt-1">
                <span className="flex items-center gap-1 text-sm text-slate-500 dark:text-slate-400">
                  <Mail className="h-3.5 w-3.5" />{vendor.email}
                </span>
                {vendor.phone && (
                  <span className="flex items-center gap-1 text-sm text-slate-500 dark:text-slate-400">
                    <Phone className="h-3.5 w-3.5" />{vendor.phone}
                  </span>
                )}
              </div>
            </div>
          </div>
          <StatusBadge status={vendor.status} />
        </div>

        {/* Stats row */}
        <div className="mt-5 grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: 'Category',  value: vendor.category ?? '—' },
            { label: 'Currency',  value: vendor.currency  ?? '—' },
            { label: 'Country',   value: vendor.country   ?? '—' },
            { label: 'Registered',value: formatDate(vendor.createdAt) },
          ].map(s => (
            <div key={s.label} className="rounded-lg bg-white/60 dark:bg-slate-800/60 border border-white/80 dark:border-slate-700/60 px-3 py-2.5">
              <p className="text-xs text-slate-400 dark:text-slate-500">{s.label}</p>
              <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-0.5 truncate">{s.value}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Detail grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        {/* Contact & Location */}
        <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
          <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-1">Contact &amp; Location</h2>
          <InfoRow icon={Mail}   label="Email"   value={vendor.email} />
          <InfoRow icon={Phone}  label="Phone"   value={vendor.phone} />
          <InfoRow icon={MapPin} label="Address" value={[vendor.address, vendor.city].filter(Boolean).join(', ')} />
          <InfoRow icon={Globe}  label="Country" value={vendor.country} />
        </div>

        {/* Business Info */}
        <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
          <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-1">Business Info</h2>
          <InfoRow icon={Hash}  label="Tax ID"              value={vendor.taxId} />
          <InfoRow icon={Hash}  label="Registration Number" value={vendor.registrationNumber} />
          <InfoRow icon={Tag}   label="Category"            value={vendor.category} />
          <InfoRow icon={Coins} label="Currency"            value={vendor.currency} />
          <div className="flex items-start gap-3 pt-3">
            <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
              <Star className="h-3.5 w-3.5 text-slate-500 dark:text-slate-400" />
            </div>
            <div>
              <p className="text-xs text-slate-400 dark:text-slate-500">Rating</p>
              <div className="mt-1"><StarRating rating={vendor.rating} /></div>
            </div>
          </div>
        </div>
      </div>

      {/* Actions */}
      {(vendor.status === 'PENDING' || vendor.status === 'APPROVED' || vendor.status === 'SUSPENDED') && (
        <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
          <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-4">Actions</h2>
          <div className="flex flex-wrap gap-3">
            {vendor.status === 'PENDING' && (
              <Button
                variant="success"
                onClick={() => approve.mutate()}
                loading={approve.isPending}
              >
                <ShieldCheck className="h-4 w-4" /> Approve Vendor
              </Button>
            )}
            {vendor.status === 'APPROVED' && (
              <Button
                variant="outline"
                className="border-orange-300 text-orange-700 hover:bg-orange-50 dark:border-orange-700 dark:text-orange-400"
                onClick={() => suspend.mutate()}
                loading={suspend.isPending}
              >
                <ShieldOff className="h-4 w-4" /> Suspend Vendor
              </Button>
            )}
            <Button
              variant="danger-outline"
              onClick={() => blacklist.mutate()}
              loading={blacklist.isPending}
            >
              <ShieldX className="h-4 w-4" /> Blacklist Vendor
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
