import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { invoicesApi } from '@/api/invoices'
import type { CreateInvoiceRequest } from '@/types'
import { ArrowLeft } from 'lucide-react'

const FIELD = 'px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-full'
const LABEL = 'block text-sm font-medium text-slate-700 mb-1'

export default function CreateInvoicePage() {
  const navigate = useNavigate()

  const [form, setForm] = useState<Partial<CreateInvoiceRequest>>({
    currency: 'USD',
  })
  const [error, setError] = useState('')

  const set = (k: keyof CreateInvoiceRequest, v: string | number) =>
    setForm(f => ({ ...f, [k]: v }))

  const create = useMutation({
    mutationFn: (req: CreateInvoiceRequest) => invoicesApi.create(req),
    onSuccess: (inv) => navigate(`/invoices/${inv.id}`),
    onError: () => setError('Failed to create invoice. Please check your inputs.'),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!form.purchaseOrderId || !form.invoiceNumber || !form.totalAmount || !form.invoiceDate) {
      setError('Purchase Order ID, Invoice Number, Total Amount, and Invoice Date are required.')
      return
    }
    create.mutate(form as CreateInvoiceRequest)
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <button onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-3">
          <ArrowLeft size={15} /> Back
        </button>
        <h1 className="text-2xl font-bold text-slate-900">New Invoice</h1>
        <p className="text-sm text-slate-500 mt-0.5">Submit an invoice against an existing Purchase Order.</p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl border p-6 space-y-5">
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
            {error}
          </div>
        )}

        {/* PO ID */}
        <div>
          <label className={LABEL}>Purchase Order ID <span className="text-red-500">*</span></label>
          <input type="number" placeholder="e.g. 42" required
            value={form.purchaseOrderId ?? ''}
            onChange={e => set('purchaseOrderId', Number(e.target.value))}
            className={FIELD} />
          <p className="text-xs text-slate-400 mt-1">Enter the numeric ID of the Purchase Order this invoice applies to.</p>
        </div>

        {/* Invoice Number */}
        <div>
          <label className={LABEL}>Invoice Number <span className="text-red-500">*</span></label>
          <input type="text" placeholder="e.g. INV-2024-00123" required
            value={form.invoiceNumber ?? ''}
            onChange={e => set('invoiceNumber', e.target.value)}
            className={FIELD} />
        </div>

        {/* Amount + Currency */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className={LABEL}>Total Amount <span className="text-red-500">*</span></label>
            <input type="number" step="0.01" min="0" placeholder="0.00" required
              value={form.totalAmount ?? ''}
              onChange={e => set('totalAmount', Number(e.target.value))}
              className={FIELD} />
          </div>
          <div>
            <label className={LABEL}>Currency</label>
            <select value={form.currency ?? 'USD'} onChange={e => set('currency', e.target.value)} className={FIELD}>
              {['USD', 'EUR', 'GBP', 'JPY', 'SGD', 'AUD', 'CAD'].map(c => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Invoice Date + Due Date */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className={LABEL}>Invoice Date <span className="text-red-500">*</span></label>
            <input type="date" required
              value={form.invoiceDate ?? ''}
              onChange={e => set('invoiceDate', e.target.value)}
              className={FIELD} />
          </div>
          <div>
            <label className={LABEL}>Due Date</label>
            <input type="date"
              value={form.dueDate ?? ''}
              onChange={e => set('dueDate', e.target.value)}
              className={FIELD} />
          </div>
        </div>

        {/* Document URL */}
        <div>
          <label className={LABEL}>Document URL</label>
          <input type="url" placeholder="https://…"
            value={form.documentUrl ?? ''}
            onChange={e => set('documentUrl', e.target.value)}
            className={FIELD} />
        </div>

        {/* Notes */}
        <div>
          <label className={LABEL}>Notes</label>
          <textarea rows={3} placeholder="Any additional notes…"
            value={form.notes ?? ''}
            onChange={e => set('notes', e.target.value)}
            className={FIELD} />
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" onClick={() => navigate(-1)}
            className="px-4 py-2 text-sm border rounded-lg hover:bg-slate-50 text-slate-600">
            Cancel
          </button>
          <button type="submit" disabled={create.isPending}
            className="px-5 py-2 text-sm font-medium bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
            {create.isPending ? 'Creating…' : 'Create Invoice'}
          </button>
        </div>
      </form>
    </div>
  )
}
