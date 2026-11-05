import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { purchaseRequestsApi } from '@/api/purchaseRequests'
import { ArrowLeft, Plus, Trash2 } from 'lucide-react'

interface LineItem { itemName: string; quantity: number; estimatedUnitPrice: number; unit: string; notes: string }

export default function CreatePurchaseRequestPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', department: '', justification: '', requiredDate: '', currency: 'USD' })
  const [items, setItems] = useState<LineItem[]>([{ itemName: '', quantity: 1, estimatedUnitPrice: 0, unit: '', notes: '' }])
  const [error, setError] = useState('')

  const create = useMutation({
    mutationFn: () => purchaseRequestsApi.create({ ...form, items }),
    onSuccess: (pr) => navigate(`/purchase-requests/${pr.id}`),
    onError: () => setError('Failed to create purchase request.'),
  })

  const setF = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  const setItem = (i: number, k: keyof LineItem) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setItems(items.map((it, idx) => idx === i ? { ...it, [k]: k === 'quantity' || k === 'estimatedUnitPrice' ? Number(e.target.value) : e.target.value } : it))

  const addItem    = () => setItems(i => [...i, { itemName: '', quantity: 1, estimatedUnitPrice: 0, unit: '', notes: '' }])
  const removeItem = (i: number) => setItems(items.filter((_, idx) => idx !== i))

  const total = items.reduce((sum, it) => sum + it.quantity * it.estimatedUnitPrice, 0)

  const inp = 'w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500'

  return (
    <div className="max-w-3xl space-y-5">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft size={15} /> Back
      </button>
      <h1 className="text-2xl font-bold text-slate-900">New Purchase Request</h1>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">{error}</div>}

      <div className="bg-white rounded-xl border p-5 space-y-4">
        <h2 className="font-semibold text-slate-700">Request Details</h2>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Title *</label>
          <input className={inp} value={form.title} onChange={setF('title')} required />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Department</label>
            <input className={inp} value={form.department} onChange={setF('department')} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Required Date</label>
            <input type="date" className={inp} value={form.requiredDate} onChange={setF('requiredDate')} />
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Description</label>
          <textarea rows={2} className={inp} value={form.description} onChange={setF('description')} />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Justification</label>
          <textarea rows={2} className={inp} value={form.justification} onChange={setF('justification')} />
        </div>
      </div>

      <div className="bg-white rounded-xl border p-5 space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-slate-700">Line Items</h2>
          <button onClick={addItem} className="inline-flex items-center gap-1 text-sm text-blue-600 hover:text-blue-800">
            <Plus size={14} /> Add item
          </button>
        </div>
        {items.map((item, i) => (
          <div key={i} className="grid grid-cols-12 gap-2 items-end border-b pb-3 last:border-0">
            <div className="col-span-4">
              <label className="block text-xs text-slate-500 mb-1">Item name *</label>
              <input className={inp} value={item.itemName} onChange={setItem(i, 'itemName')} />
            </div>
            <div className="col-span-2">
              <label className="block text-xs text-slate-500 mb-1">Qty *</label>
              <input type="number" min="1" className={inp} value={item.quantity} onChange={setItem(i, 'quantity')} />
            </div>
            <div className="col-span-2">
              <label className="block text-xs text-slate-500 mb-1">Unit</label>
              <input className={inp} value={item.unit} onChange={setItem(i, 'unit')} />
            </div>
            <div className="col-span-3">
              <label className="block text-xs text-slate-500 mb-1">Est. Unit Price *</label>
              <input type="number" min="0" step="0.01" className={inp} value={item.estimatedUnitPrice} onChange={setItem(i, 'estimatedUnitPrice')} />
            </div>
            <div className="col-span-1 flex justify-end">
              {items.length > 1 && (
                <button onClick={() => removeItem(i)} className="text-red-400 hover:text-red-600 p-1">
                  <Trash2 size={15} />
                </button>
              )}
            </div>
          </div>
        ))}
        <div className="text-right text-sm font-semibold text-slate-700">
          Estimated Total: <span className="text-blue-700">{new Intl.NumberFormat('en-US',{style:'currency',currency:form.currency}).format(total)}</span>
        </div>
      </div>

      <div className="flex gap-3">
        <button onClick={() => create.mutate()} disabled={create.isPending || !form.title}
          className="bg-blue-600 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50">
          {create.isPending ? 'Creating…' : 'Create Request'}
        </button>
        <button onClick={() => navigate(-1)} className="border text-sm font-medium px-5 py-2 rounded-lg hover:bg-slate-50">
          Cancel
        </button>
      </div>
    </div>
  )
}
