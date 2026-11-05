import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { quotationsApi } from '@/api/quotations'
import { ArrowLeft } from 'lucide-react'

const FIELD = 'w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500'
const LABEL = 'block text-sm font-medium text-slate-700 mb-1'

export default function CreateRFQPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    title:             '',
    description:       '',
    deadline:          '',
    purchaseRequestId: '',
  })
  const [error, setError] = useState('')

  const setF = (k: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(f => ({ ...f, [k]: e.target.value }))

  const create = useMutation({
    mutationFn: () => quotationsApi.createRfq({
      title:             form.title,
      description:       form.description       || undefined,
      deadline:          form.deadline          || undefined,
      purchaseRequestId: form.purchaseRequestId
        ? Number(form.purchaseRequestId)
        : undefined,
    }),
    onSuccess: (rfq) => navigate(`/quotations/${rfq.id}`),
    onError:   ()    => setError('Failed to create RFQ. Please check your inputs.'),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!form.title.trim()) { setError('Title is required.'); return }
    create.mutate()
  }

  return (
    <div className="max-w-2xl space-y-6">
      {/* Header */}
      <div>
        <button onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-3">
          <ArrowLeft size={15} /> Back
        </button>
        <h1 className="text-2xl font-bold text-slate-900">Create RFQ</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          A new Request for Quotation will be saved as a <strong>Draft</strong>. Publish it to invite vendors to submit quotes.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
            {error}
          </div>
        )}

        <div className="bg-white rounded-xl border p-5 space-y-4">
          <h2 className="font-semibold text-slate-700">RFQ Details</h2>

          {/* Title */}
          <div>
            <label className={LABEL}>Title <span className="text-red-500">*</span></label>
            <input
              type="text"
              required
              placeholder="e.g. Office Supplies Q3 2025"
              value={form.title}
              onChange={setF('title')}
              className={FIELD}
            />
          </div>

          {/* Description */}
          <div>
            <label className={LABEL}>Description</label>
            <textarea
              rows={4}
              placeholder="Describe the requirements, specifications, and evaluation criteria…"
              value={form.description}
              onChange={setF('description')}
              className={FIELD}
            />
          </div>

          {/* Deadline + PR ID */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={LABEL}>
                Submission Deadline
                <span className="text-xs text-slate-400 ml-1">(optional)</span>
              </label>
              <input
                type="datetime-local"
                value={form.deadline}
                onChange={setF('deadline')}
                className={FIELD}
              />
            </div>
            <div>
              <label className={LABEL}>
                Purchase Request ID
                <span className="text-xs text-slate-400 ml-1">(optional)</span>
              </label>
              <input
                type="number"
                min="1"
                placeholder="e.g. 10"
                value={form.purchaseRequestId}
                onChange={setF('purchaseRequestId')}
                className={FIELD}
              />
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={create.isPending}
            className="bg-blue-600 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {create.isPending ? 'Creating…' : 'Create RFQ (Draft)'}
          </button>
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="border text-sm font-medium px-5 py-2 rounded-lg hover:bg-slate-50 text-slate-600"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
