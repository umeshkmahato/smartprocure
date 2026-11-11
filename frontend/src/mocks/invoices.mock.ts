import type { Invoice, CreateInvoiceRequest, PageResponse } from '@/types'
import { delay, paginate, nextId, now, daysAgo, daysFromNow, matchesKeyword } from './utils'

let invoices: Invoice[] = [
  { id: 1,  invoiceNumber: 'INV-2024-001', vendorId: 2,  vendorName: 'GlobalParts Ltd.',       purchaseOrderId: 2,  poNumber: 'PO-2024-002', status: 'UNDER_REVIEW', currency: 'GBP', totalAmount: 21000, invoiceDate: daysAgo(5),  dueDate: daysFromNow(25), notes: 'Invoice for office furniture delivery — PO-2024-002.',              createdAt: daysAgo(5),  updatedAt: daysAgo(1) },
  { id: 2,  invoiceNumber: 'INV-2024-002', vendorId: 4,  vendorName: 'MedEquip Solutions',      purchaseOrderId: 3,  poNumber: 'PO-2024-003', status: 'SUBMITTED',   currency: 'AED', totalAmount: 18400, invoiceDate: daysAgo(2),  dueDate: daysFromNow(28), notes: 'Medical supplies restocking invoice.',                              createdAt: daysAgo(2),  updatedAt: daysAgo(1) },
  { id: 3,  invoiceNumber: 'INV-2024-003', vendorId: 8,  vendorName: 'DataVault Consulting',    purchaseOrderId: 4,  poNumber: 'PO-2024-004', status: 'VERIFIED',    currency: 'USD', totalAmount: 36000, invoiceDate: daysAgo(10), dueDate: daysFromNow(20), notes: 'Partial invoice — AWS reserved instances only.',                   createdAt: daysAgo(10), updatedAt: daysAgo(3) },
  { id: 4,  invoiceNumber: 'INV-2024-004', vendorId: 1,  vendorName: 'TechSupply Co.',          purchaseOrderId: 1,  poNumber: 'PO-2024-001', status: 'PAID',        currency: 'USD', totalAmount: 46500, invoiceDate: daysAgo(30), dueDate: daysAgo(0),      paidAt: daysAgo(2),  notes: 'Full invoice for laptop procurement batch.',              createdAt: daysAgo(30), updatedAt: daysAgo(2) },
  { id: 5,  invoiceNumber: 'INV-2024-005', vendorId: 11, vendorName: 'Nexus HR Solutions',      purchaseOrderId: 6,  poNumber: 'PO-2024-006', status: 'PAID',        currency: 'SGD', totalAmount: 9600,  invoiceDate: daysAgo(20), dueDate: daysAgo(5),      paidAt: daysAgo(3),  notes: 'Q1 cleaning services — 3 months.',                       createdAt: daysAgo(20), updatedAt: daysAgo(3) },
  { id: 6,  invoiceNumber: 'INV-2024-006', vendorId: 1,  vendorName: 'TechSupply Co.',          purchaseOrderId: 5,  poNumber: 'PO-2024-005', status: 'DISPUTED',    currency: 'USD', totalAmount: 32000, invoiceDate: daysAgo(8),  dueDate: daysFromNow(22), disputeReason: 'Unit price discrepancy on CrowdStrike Falcon line.',      createdAt: daysAgo(8),  updatedAt: daysAgo(1) },
  { id: 7,  invoiceNumber: 'INV-2024-007', vendorId: 6,  vendorName: 'BuildRight Contractors',  purchaseOrderId: 7,  poNumber: 'PO-2024-007', status: 'SUBMITTED',   currency: 'USD', totalAmount: 92000, invoiceDate: daysAgo(1),  dueDate: daysFromNow(29), notes: 'Server room CRAC unit installation invoice.',                      createdAt: daysAgo(1),  updatedAt: daysAgo(1) },
  { id: 8,  invoiceNumber: 'INV-2024-008', vendorId: 3,  vendorName: 'SwiftLogistics Inc.',     purchaseOrderId: 8,  poNumber: 'PO-2024-008', status: 'VERIFIED',    currency: 'SGD', totalAmount: 9000,  invoiceDate: daysAgo(7),  dueDate: daysFromNow(23), notes: 'Q1 logistics services — 3 months.',                                createdAt: daysAgo(7),  updatedAt: daysAgo(2) },
  { id: 9,  invoiceNumber: 'INV-2024-009', vendorId: 12, vendorName: 'SafeGuard Security',      purchaseOrderId: 12, poNumber: 'PO-2024-012', status: 'CANCELLED',   currency: 'AED', totalAmount: 42000, invoiceDate: daysAgo(40), dueDate: daysAgo(10),     notes: 'Cancelled — PO was superseded.',                                   createdAt: daysAgo(40), updatedAt: daysAgo(38) },
  { id: 10, invoiceNumber: 'INV-2024-010', vendorId: 5,  vendorName: 'AgroFresh Supplies',      purchaseOrderId: 10, poNumber: 'PO-2024-010', status: 'UNDER_REVIEW', currency: 'EUR', totalAmount: 23000, invoiceDate: daysAgo(3),  dueDate: daysFromNow(27), notes: 'Advance invoice — catering services month 1.',                     createdAt: daysAgo(3),  updatedAt: daysAgo(1) },
]

type ListParams = {
  status?: string; vendorId?: number; purchaseOrderId?: number
  keyword?: string; page?: number; size?: number; sort?: string; dir?: string
}

export const mockInvoicesApi = {
  list: async (params: ListParams = {}): Promise<PageResponse<Invoice>> => {
    await delay()
    const { status, vendorId, purchaseOrderId, keyword, page, size, sort, dir } = params
    return paginate(invoices, {
      page, size, sort, dir,
      filter: (inv) => {
        if (status          && inv.status          !== status)          return false
        if (vendorId        && inv.vendorId        !== vendorId)        return false
        if (purchaseOrderId && inv.purchaseOrderId !== purchaseOrderId) return false
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
      invoiceNumber: req.invoiceNumber ?? `INV-2024-${String(id).padStart(3, '0')}`,
      vendorId: 0,
      vendorName: 'Unknown Vendor',
      purchaseOrderId: req.purchaseOrderId,
      poNumber: '',
      status: 'SUBMITTED',
      currency: req.currency ?? 'USD',
      totalAmount: req.totalAmount,
      invoiceDate: req.invoiceDate,
      dueDate: req.dueDate,
      documentUrl: req.documentUrl,
      notes: req.notes,
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
