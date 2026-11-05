import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { vendorsApi } from '@/api/vendors'
import StatusBadge from '@/components/ui/StatusBadge'
import { formatDate } from '@/lib/utils'
import { ArrowLeft } from 'lucide-react'

export default function VendorDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: vendor, isLoading } = useQuery({
    queryKey: ['vendor', id],
    queryFn: () => vendorsApi.getById(Number(id)),
  })

  const approve   = useMutation({ mutationFn: () => vendorsApi.approve(Number(id)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vendor', id] }) })
  const suspend   = useMutation({ mutationFn: () => vendorsApi.suspend(Number(id), 'Suspended by admin'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vendor', id] }) })
  const blacklist = useMutation({ mutationFn: () => vendorsApi.blacklist(Number(id), 'Blacklisted by admin'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['vendor', id] }) })

  if (isLoading) return <p className="p-6 text-sm text-slate-400">Loading…</p>
  if (!vendor)   return <p className="p-6 text-sm text-red-500">Vendor not found.</p>

  const Row = ({ label, value }: { label: string; value?: string | number | null }) => (
    <div className="flex gap-4 py-3 border-b last:border-0">
      <span className="w-44 text-sm text-slate-500 shrink-0">{label}</span>
      <span className="text-sm text-slate-900 font-medium">{value ?? '—'}</span>
    </div>
  )

  return (
    <div className="space-y-5 max-w-3xl">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft size={15} /> Back
      </button>

      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{vendor.name}</h1>
          <p className="text-slate-500 text-sm mt-0.5">{vendor.email}</p>
        </div>
        <StatusBadge status={vendor.status} />
      </div>

      <div className="bg-white rounded-xl border p-5">
        <h2 className="font-semibold text-slate-700 mb-3">Details</h2>
        <Row label="Phone"               value={vendor.phone} />
        <Row label="Address"             value={vendor.address} />
        <Row label="City"                value={vendor.city} />
        <Row label="Country"             value={vendor.country} />
        <Row label="Tax ID"              value={vendor.taxId} />
        <Row label="Registration Number" value={vendor.registrationNumber} />
        <Row label="Category"            value={vendor.category} />
        <Row label="Currency"            value={vendor.currency} />
        <Row label="Rating"              value={vendor.rating != null ? `${vendor.rating} / 5` : undefined} />
        <Row label="Registered"          value={formatDate(vendor.createdAt)} />
      </div>

      {/* Actions */}
      <div className="flex gap-3">
        {vendor.status === 'PENDING' && (
          <button onClick={() => approve.mutate()}
            className="bg-green-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-green-700">
            Approve
          </button>
        )}
        {vendor.status === 'APPROVED' && (
          <button onClick={() => suspend.mutate()}
            className="bg-orange-500 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-orange-600">
            Suspend
          </button>
        )}
        {vendor.status !== 'BLACKLISTED' && (
          <button onClick={() => blacklist.mutate()}
            className="bg-red-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-red-700">
            Blacklist
          </button>
        )}
      </div>
    </div>
  )
}
