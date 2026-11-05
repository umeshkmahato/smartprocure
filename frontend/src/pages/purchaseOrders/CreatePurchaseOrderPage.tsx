import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { purchaseOrdersApi } from '@/api/purchaseOrders'
import { vendorsApi } from '@/api/vendors'
import type { CreatePoRequest } from '@/types'
import { ArrowLeft, Plus, Trash2 } from 'lucide-react'

interface LineItem {
  itemName: string
  description: string
  quantity: number
  unit: string
  unitPrice: number
  notes: string
}

const FIELD  = 'w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500'
const LABEL  = 'block text-sm font-medium text-slate-700 mb-1'
const BLANK_ITEM: LineItem = { itemName: '', description: '', quantity: 1, unit: '', unitPrice: 0, notes: '' }

export default function CreatePurchaseOrderPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState({
    vendorId:          '',
    purchaseRequestId: '',
    quotationId:       '',
    currency:          'USD',
    deliveryDate:      '',
    deliveryAddress:   '',
    paymentTerms:      '',
    notes:             '',
  })
  const [items, setItems] = useState<LineItem[]>([{ ...BLANK_ITEM }])
  const [error, setError] = useState('')

  /* ── Vendor dropdown ─────────────────────────────────────────────────────── */
  const { data: vendors } = useQuery({
    queryKey: ['vendors-dropdown'],
    queryFn:  () => vendorsApi.list({ status: 'APPROVED', size: 200, sort: 'name', dir: 'asc' }),
    staleTime: 60_000,
  })

  /* ── Helpers ─────────────────────────────────────────────────────────────── */
  const setF = (k: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
      setForm(f => ({ ...f, [k]: e.target.value }))

  const setItem = (idx: number, k: keyof LineItem) =>
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const val = k === 'quantity' || k === 'unitPrice' ? Number(e.target.value) : e.target.value
      setItems(prev => prev.map((it, i) => i === idx ? { ...it, [k]: val } : it))
    }

  const addItem    = () => setItems(prev => [...prev, { ...BLANK_ITEM }])
  const removeItem = (idx: number) => setItems(prev => prev.filter((_, i) => i !== idx))

  const total = items.reduce((s, it) => s + it.quantity * it.unitPrice, 0)

  /* ── Submit ──────────────────────────────────────────────────────────────── */
  const create = useMutation({
    mutationFn: (req: CreatePoRequest) => purchaseOrdersApi.create(req),
    onSuccess:  (po) => navigate(`/purchase-orders/${po.id}`),
    onError:    () => setError('Failed to create purchase order. Please check your inputs.'),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!form.vendorId) { setError('Please select a vendor.'); return }
    if (items.some(it => !it.itemName || it.quantity <= 0 || it.unitPrice < 0)) {
      setError('Every line item must have a name, quantity > 0, and unit price ≥ 0.')
      return
    }

    const req: CreatePoRequest = {
      vendorId:          Number(form.vendorId),
      purchaseRequestId: form.purchaseRequestId ? Number(form.purchaseRequestId) : undefined,
      quotationId:       form.quotationId       ? Number(form.quotationId)       : undefined,
      currency:          form.currency,
      deliveryDate:      form.deliveryDate       || undefined,
      deliveryAddress:   form.deliveryAddress    || undefined,
      paymentTerms:      form.paymentTerms       || undefined,
      notes:             form.notes              || undefined,
      items: items.map(it => ({
        itemName:    it.itemName,
        description: it.description || undefined,
        quantity:    it.quantity,
        unit:        it.unit        || undefined,
        unitPrice:   it.unitPrice,
        notes:       it.notes       || undefined,
      })),
    }
    create.mutate(req)
  }

  return (
    <div className="max-w-4xl space-y-6">
      {/* Header */}
      <div>
        <button onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-3">
          <ArrowLeft size={15} /> Back
        </button>
        <h1 className="text-2xl font-bold text-slate-900">New Purchase Order</h1>
        <p className="text-sm text-slate-500 mt-0.5">Create a draft PO, then issue it to the vendor.</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
            {error}
          </div>
        )}

        {/* ── Order Details ──────────────────────────────────────────────── */}
        <div className="bg-white rounded-xl border p-5 space-y-4">
          <h2 className="font-semibold text-slate-700">Order Details</h2>

          {/* Vendor */}
          <div>
            <label className={LABEL}>Vendor <span className="text-red-500">*</span></label>
            <select required value={form.vendorId} onChange={setF('vendorId')} className={FIELD}>
              <option value="">— Select vendor —</option>
              {vendors?.content.map(v => (
                <option key={v.id} value={v.id}>{v.name}</option>
              ))}
            </select>
          </div>

          {/* PR + Quotation IDs */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={LABEL}>Purchase Request ID <span className="text-xs text-slate-400">(optional)</span></label>
              <input type="number" min="1" placeholder="e.g. 10"
                value={form.purchaseRequestId} onChange={setF('purchaseRequestId')} className={FIELD} />
            </div>
            <div>
              <label className={LABEL}>Quotation ID <span className="text-xs text-slate-400">(optional)</span></label>
              <input type="number" min="1" placeholder="e.g. 5"
                value={form.quotationId} onChange={setF('quotationId')} className={FIELD} />
            </div>
          </div>

          {/* Currency + Delivery Date */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={LABEL}>Currency</label>
              <select value={form.currency} onChange={setF('currency')} className={FIELD}>
                {['USD', 'EUR', 'GBP', 'JPY', 'SGD', 'AUD', 'CAD'].map(c => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={LABEL}>Delivery Date</label>
              <input type="date" value={form.deliveryDate} onChange={setF('deliveryDate')} className={FIELD} />
            </div>
          </div>

          {/* Delivery Address + Payment Terms */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={LABEL}>Delivery Address</label>
              <input type="text" placeholder="123 Main St…"
                value={form.deliveryAddress} onChange={setF('deliveryAddress')} className={FIELD} />
            </div>
            <div>
              <label className={LABEL}>Payment Terms</label>
              <input type="text" placeholder="e.g. Net 30"
                value={form.paymentTerms} onChange={setF('paymentTerms')} className={FIELD} />
            </div>
          </div>

          {/* Notes */}
          <div>
            <label className={LABEL}>Notes</label>
            <textarea rows={2} placeholder="Additional notes…"
              value={form.notes} onChange={setF('notes')} className={FIELD} />
          </div>
        </div>

        {/* ── Line Items ─────────────────────────────────────────────────── */}
        <div className="bg-white rounded-xl border p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-slate-700">Line Items</h2>
            <button type="button" onClick={addItem}
              className="inline-flex items-center gap-1 text-sm text-blue-600 hover:text-blue-800">
              <Plus size={14} /> Add item
            </button>
          </div>

          <div className="space-y-3">
            {items.map((item, idx) => (
              <div key={idx} className="grid grid-cols-12 gap-2 items-end border rounded-lg p-3 bg-slate-50">
                {/* Item name */}
                <div className="col-span-3">
                  <label className="block text-xs text-slate-500 mb-1">Item name *</label>
                  <input placeholder="Item name" value={item.itemName}
                    onChange={setItem(idx, 'itemName')} className={FIELD} required />
                </div>
                {/* Description */}
                <div className="col-span-3">
                  <label className="block text-xs text-slate-500 mb-1">Description</label>
                  <input placeholder="Optional" value={item.description}
                    onChange={setItem(idx, 'description')} className={FIELD} />
                </div>
                {/* Qty */}
                <div className="col-span-1">
                  <label className="block text-xs text-slate-500 mb-1">Qty *</label>
                  <input type="number" min="1" value={item.quantity}
                    onChange={setItem(idx, 'quantity')} className={FIELD} required />
                </div>
                {/* Unit */}
                <div className="col-span-1">
                  <label className="block text-xs text-slate-500 mb-1">Unit</label>
                  <input placeholder="pcs" value={item.unit}
                    onChange={setItem(idx, 'unit')} className={FIELD} />
                </div>
                {/* Unit Price */}
                <div className="col-span-2">
                  <label className="block text-xs text-slate-500 mb-1">Unit Price *</label>
                  <input type="number" min="0" step="0.01" value={item.unitPrice}
                    onChange={setItem(idx, 'unitPrice')} className={FIELD} required />
                </div>
                {/* Line total */}
                <div className="col-span-1 text-sm text-right text-slate-600 font-medium pb-2">
                  {new Intl.NumberFormat('en-US', { style: 'currency', currency: form.currency })
                    .format(item.quantity * item.unitPrice)}
                </div>
                {/* Remove */}
                <div className="col-span-1 flex justify-end pb-1">
                  {items.length > 1 && (
                    <button type="button" onClick={() => removeItem(idx)}
                      className="text-red-400 hover:text-red-600 p-1">
                      <Trash2 size={15} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Grand total */}
          <div className="flex justify-end text-sm font-semibold text-slate-700 border-t pt-3">
            Grand Total:&nbsp;
            <span className="text-blue-700 ml-1">
              {new Intl.NumberFormat('en-US', { style: 'currency', currency: form.currency }).format(total)}
            </span>
          </div>
        </div>

        {/* ── Actions ────────────────────────────────────────────────────── */}
        <div className="flex gap-3">
          <button type="submit" disabled={create.isPending}
            className="bg-blue-600 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50">
            {create.isPending ? 'Creating…' : 'Create Purchase Order'}
          </button>
          <button type="button" onClick={() => navigate(-1)}
            className="border text-sm font-medium px-5 py-2 rounded-lg hover:bg-slate-50 text-slate-600">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
