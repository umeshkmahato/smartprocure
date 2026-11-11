import type { Invoice, CreateInvoiceRequest, PageResponse } from '@/types'
import { delay, paginate, nextId, now, daysAgo, daysFromNow, matchesKeyword } from './utils'

let invoices: Invoice[] = [
  {
    id: 1, invoiceNumber: 'INV-2024-001', vendorId: 2, vendorName: 'GlobalParts Ltd.',
    poId: 2, poNumber: 'PO-2024-002', status: 'UNDER_REVIEW',
    currency: 'GBP', totalAmount: 21000, taxAmount: 4200, subtotal: 16800,
    invoiceDate: daysAgo(5), dueDate: daysFromNow(25), paidAt: undefined,
    notes: 'Invoice for office furniture delivery — PO-2024-002.',
    items: [
      { id: 1, description: 'Standing Desks × 15',   quantity: 15, unitPrice: 800, totalPrice: 12000 },
      { id: 2, description: 'Ergonomic Chairs × 15', quantity: 15, unitPrice: 600, totalPrice: 9000  },
    ],
    createdAt: daysAgo(5), updatedAt: daysAgo(1),
  },
  {
    id: 2, invoiceNumber: 'INV-2024-002', vendorId: 4, vendorName: 'MedEquip Solutions',
    poId: 3, poNumber: 'PO-2024-003', status: 'SUBMITTED',
    currency: 'AED', totalAmount: 18400, taxAmount: 920, subtotal: 17480,
    invoiceDate: daysAgo(2), dueDate: daysFromNow(28), paidAt: undefined,
    notes: 'Medical supplies restocking invoice.',
    items: [
      { id: 3, description: 'First Aid Kits × 20',    quantity: 20, unitPrice: 350, totalPrice: 7000  },
      { id: 4, description: 'PPE Supply Bundles × 50', quantity: 50, unitPrice: 228, totalPrice: 11400 },
    ],
    createdAt: daysAgo(2), updatedAt: daysAgo(1),
  },
  {
    id: 3, invoiceNumber: 'INV-2024-003', vendorId: 8, vendorName: 'DataVault Consulting',
    poId: 4, poNumber: 'PO-2024-004', status: 'VERIFIED',
    currency: 'USD', totalAmount: 36000, taxAmount: 0, subtotal: 36000,
    invoiceDate: daysAgo(10), dueDate: daysFromNow(20), paidAt: undefined,
    notes: 'Partial invoice — AWS reserved instances only.',
    items: [
      { id: 5, description: 'AWS Reserved Instances × 4', quantity: 4, unitPrice: 9000, totalPrice: 36000 },
    ],
    createdAt: daysAgo(10), updatedAt: daysAgo(3),
  },
  {
    id: 4, invoiceNumber: 'INV-2024-004', vendorId: 1, vendorName: 'TechSupply Co.',
    poId: 1, poNumber: 'PO-2024-001', status: 'PAID',
    currency: 'USD', totalAmount: 46500, taxAmount: 0, subtotal: 46500,
    invoiceDate: daysAgo(30), dueDate: daysAgo(0), paidAt: daysAgo(2),
    notes: 'Full invoice for laptop procurement batch.',
    items: [
      { id: 6, description: 'MacBook Pro 14" × 15', quantity: 15, unitPrice: 2200, totalPrice: 33000 },
      { id: 7, description: 'Dell XPS 15 × 5',      quantity: 5,  unitPrice: 3000, totalPrice: 15000 },
    ],
    createdAt: daysAgo(30), updatedAt: daysAgo(2),
  },
  {
    id: 5, invoiceNumber: 'INV-2024-005', vendorId: 11, vendorName: 'Nexus HR Solutions',
    poId: 6, poNumber: 'PO-2024-006', status: 'PAID',
    currency: 'SGD', totalAmount: 9600, taxAmount: 672, subtotal: 8928,
    invoiceDate: daysAgo(20), dueDate: daysAgo(5), paidAt: daysAgo(3),
    notes: 'Q1 cleaning services — 3 months.',
    items: [
      { id: 8, description: 'Office Cleaning Service × 3 months', quantity: 3, unitPrice: 3200, totalPrice: 9600 },
    ],
    createdAt: daysAgo(20), updatedAt: daysAgo(3),
  },
  {
    id: 6, invoiceNumber: 'INV-2024-006', vendorId: 1, vendorName: 'TechSupply Co.',
    poId: 5, poNumber: 'PO-2024-005', status: 'DISPUTED',
    currency: 'USD', totalAmount: 32000, taxAmount: 0, subtotal: 32000,
    invoiceDate: daysAgo(8), dueDate: daysFromNow(22), paidAt: undefined,
    notes: 'Dispute: invoice amount does not match PO line items. CrowdStrike unit price discrepancy.',
    items: [
      { id: 9,  description: 'CrowdStrike Falcon × 500 endpoints', quantity: 500, unitPrice: 64, totalPrice: 32000 },
    ],
    createdAt: daysAgo(8), updatedAt: daysAgo(1),
  },
  {
    id: 7, invoiceNumber: 'INV-2024-007', vendorId: 6, vendorName: 'BuildRight Contractors',
    poId: 7, poNumber: 'PO-2024-007', status: 'SUBMITTED',
    currency: 'USD', totalAmount: 92000, taxAmount: 9200, subtotal: 82800,
    invoiceDate: daysAgo(1), dueDate: daysFromNow(29), paidAt: undefined,
    notes: 'Server room CRAC unit installation invoice.',
    items: [
      { id: 10, description: 'CRAC Unit 20kW × 3',    quantity: 3, unitPrice: 25000, totalPrice: 75000 },
      { id: 11, description: 'Installation Kit × 3',  quantity: 3, unitPrice: 4000,  totalPrice: 12000 },
      { id: 12, description: 'Extended Warranty × 3', quantity: 3, unitPrice: 1667,  totalPrice: 5000  },
    ],
    createdAt: daysAgo(1), updatedAt: daysAgo(1),
  },
  {
    id: 8, invoiceNumber: 'INV-2024-008', vendorId: 3, vendorName: 'SwiftLogistics Inc.',
    poId: 8, poNumber: 'PO-2024-008', status: 'VERIFIED',
    currency: 'SGD', totalAmount: 9000, taxAmount: 630, subtotal: 8370,
    invoiceDate: daysAgo(7), dueDate: daysFromNow(23), paidAt: undefined,
    notes: 'Q1 logistics services — 3 months.',
    items: [
      { id: 13, description: 'Logistics Services × 3 months', quantity: 3, unitPrice: 3000, totalPrice: 9000 },
    ],
    createdAt: daysAgo(7), updatedAt: daysAgo(2),
  },
  {
    id: 9, invoiceNumber: 'INV-2024-009', vendorId: 12, vendorName: 'SafeGuard Security',
    poId: 12, poNumber: 'PO-2024-012', status: 'CANCELLED',
    currency: 'AED', totalAmount: 42000, taxAmount: 2100, subtotal: 39900,
    invoiceDate: daysAgo(40), dueDate: daysAgo(10), paidAt: undefined,
    notes: 'Cancelled — PO was superseded.',
    items: [
      { id: 14, description: 'Vehicle Full Service — Sedan × 8', quantity: 8, unitPrice: 2500, totalPrice: 20000 },
      { id: 15, description: 'Vehicle Full Service — SUV × 4',   quantity: 4, unitPrice: 5500, totalPrice: 22000 },
    ],
    createdAt: daysAgo(40), updatedAt: daysAgo(38),
  },
  {
    id: 10, invoiceNumber: 'INV-2024-010', vendorId: 5, vendorName: 'AgroFresh Supplies',
    poId: 10, poNumber: 'PO-2024-010', status: 'UNDER_REVIEW',
    currency: 'EUR', totalAmount: 23000, taxAmount: 1150, subtotal: 21850,
    invoiceDate: daysAgo(3), dueDate: daysFromNow(27), paidAt: undefined,
    notes: 'Advance invoice — catering services month 1.',
    items: [
      { id: 16, description: 'Daily Catering Service × 50 days', quantity: 50, unitPrice: 460, totalPrice: 23000 },
    ],
    createdAt: daysAgo(3), updatedAt: daysAgo(1),
  },
]

type ListParams = {
  status?: string; vendorId?: number; poId?: number
  keyword?: string; page?: number; size?: number; sort?: string; dir?: string
}

export const mockInvoicesApi = {
  list: async (params: ListParams = {}): Promise<PageResponse<Invoice>> => {
    await delay()
    const { status, vendorId, poId, keyword, page, size, sort, dir } = params
    return paginate(invoices, {
      page, size, sort, dir,
      filter: (inv) => {
        if (status   && inv.status   !== status)   return false
        if (vendorId && inv.vendorId !== vendorId) return false
        if (poId     && inv.poId     !== poId)     return false
        return matchesKeyword(inv, keyword, ['invoiceNumber', 'vendorName', 'poNumber'])
      },
    })
  },

  getById: async (id: number): Promise<Invoice> => {
    await delay()
    const inv = invoices.find((i) => i.id === id)
    if (!inv) throw new Error(`Invoice ${id} not found`)
    return { ...inv, items: inv.items.map((i) => ({ ...i })) }
  },

  create: async (req: CreateInvoiceRequest): Promise<Invoice> => {
    await delay()
    const id = nextId(invoices)
    const newInv: Invoice = {
      id,
      invoiceNumber: `INV-2024-${String(id).padStart(3, '0')}`,
      vendorId: req.vendorId,
      vendorName: req.vendorName ?? 'Unknown Vendor',
      poId: req.poId,
      poNumber: req.poNumber ?? '',
      status: 'SUBMITTED',
      currency: req.currency ?? 'USD',
      subtotal: req.subtotal ?? 0,
      taxAmount: req.taxAmount ?? 0,
      totalAmount: req.totalAmount ?? 0,
      invoiceDate: req.invoiceDate ?? now(),
      dueDate: req.dueDate,
      paidAt: undefined,
      notes: req.notes,
      items: (req.items ?? []).map((item, i) => ({
        id: i + 1,
        description: item.description,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        totalPrice: item.quantity * item.unitPrice,
      })),
      createdAt: now(),
      updatedAt: now(),
    }
    invoices = [newInv, ...invoices]
    return newInv
  },

  update: async (id: number, req: unknown): Promise<Invoice> => {
    await delay()
    invoices = invoices.map((i) => i.id === id ? { ...i, ...(req as Partial<Invoice>), updatedAt: now() } : i)
    return invoices.find((i) => i.id === id)!
  },

  startReview: async (id: number): Promise<Invoice> => {
    await delay()
    invoices = invoices.map((i) => i.id === id ? { ...i, status: 'UNDER_REVIEW', updatedAt: now() } : i)
    return invoices.find((i) => i.id === id)!
  },

  verify: async (id: number): Promise<Invoice> => {
    await delay()
    invoices = invoices.map((i) => i.id === id ? { ...i, status: 'VERIFIED', updatedAt: now() } : i)
    return invoices.find((i) => i.id === id)!
  },

  dispute: async (id: number, reason: string): Promise<Invoice> => {
    await delay()
    invoices = invoices.map((i) =>
      i.id === id ? { ...i, status: 'DISPUTED', notes: reason, updatedAt: now() } : i,
    )
    return invoices.find((i) => i.id === id)!
  },

  markPaid: async (id: number): Promise<Invoice> => {
    await delay()
    invoices = invoices.map((i) =>
      i.id === id ? { ...i, status: 'PAID', paidAt: now(), updatedAt: now() } : i,
    )
    return invoices.find((i) => i.id === id)!
  },

  cancel: async (id: number): Promise<Invoice> => {
    await delay()
    invoices = invoices.map((i) => i.id === id ? { ...i, status: 'CANCELLED', updatedAt: now() } : i)
    return invoices.find((i) => i.id === id)!
  },
}
