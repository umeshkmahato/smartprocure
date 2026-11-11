import type { Rfq, Quotation, PageResponse } from '@/types'
import { delay, paginate, nextId, now, daysAgo, daysFromNow, matchesKeyword } from './utils'

let rfqs: Rfq[] = [
  { id: 1, rfqNumber: 'RFQ-2024-001', title: 'Laptop Supply — Q1 Batch',          description: 'Seeking quotes for 20 laptops for engineering team.',       status: 'AWARDED',    deadline: daysAgo(10),     purchaseRequestId: 1, createdAt: daysAgo(40), updatedAt: daysAgo(5)  },
  { id: 2, rfqNumber: 'RFQ-2024-002', title: 'Office Furniture Procurement',       description: 'Ergonomic desks and chairs for 15 workstations.',           status: 'CLOSED',     deadline: daysAgo(5),      purchaseRequestId: 2, createdAt: daysAgo(30), updatedAt: daysAgo(5)  },
  { id: 3, rfqNumber: 'RFQ-2024-003', title: 'Cloud Hosting Services',             description: 'Annual cloud infrastructure — AWS / Azure comparison.',     status: 'PUBLISHED',  deadline: daysFromNow(7),  purchaseRequestId: 4, createdAt: daysAgo(15), updatedAt: daysAgo(2)  },
  { id: 4, rfqNumber: 'RFQ-2024-004', title: 'Security Software Licences',         description: 'Endpoint protection and SIEM platform bids.',               status: 'PUBLISHED',  deadline: daysFromNow(5),  purchaseRequestId: 9, createdAt: daysAgo(12), updatedAt: daysAgo(1)  },
  { id: 5, rfqNumber: 'RFQ-2024-005', title: 'Catering Services — Annual Tender',  description: 'Head office daily catering for 250 covers, 250 days.',      status: 'PUBLISHED',  deadline: daysFromNow(14), purchaseRequestId: 8, createdAt: daysAgo(10), updatedAt: daysAgo(1)  },
  { id: 6, rfqNumber: 'RFQ-2024-006', title: 'Server Room CRAC Units',             description: 'Three replacement 20kW CRAC units with installation.',      status: 'CLOSED',     deadline: daysAgo(2),      purchaseRequestId: 7, createdAt: daysAgo(20), updatedAt: daysAgo(2)  },
  { id: 7, rfqNumber: 'RFQ-2024-007', title: 'HR Software Implementation',         description: 'HRMS upgrade including payroll and performance modules.',   status: 'DRAFT',      deadline: daysFromNow(30), purchaseRequestId: 11, createdAt: daysAgo(3), updatedAt: daysAgo(1)  },
  { id: 8, rfqNumber: 'RFQ-2024-008', title: 'Marketing Print Materials',          description: 'Brochures, banners and branded merchandise for trade show.', status: 'DRAFT',     deadline: daysFromNow(10), purchaseRequestId: 3, createdAt: daysAgo(1),  updatedAt: daysAgo(1)  },
]

let quotations: Quotation[] = [
  // RFQ 1 — AWARDED
  { id: 1,  rfqId: 1, rfqNumber: 'RFQ-2024-001', vendorId: 1,  vendorName: 'TechSupply Co.',      status: 'SELECTED',     totalAmount: 46500,  currency: 'USD', validUntil: daysAgo(5),     notes: 'Best value — includes 2yr warranty.',          createdAt: daysAgo(35), updatedAt: daysAgo(8)  },
  { id: 2,  rfqId: 1, rfqNumber: 'RFQ-2024-001', vendorId: 9,  vendorName: 'CloudNine Networks',  status: 'REJECTED',     totalAmount: 51200,  currency: 'USD', validUntil: daysAgo(5),     notes: 'Higher price, 1yr warranty only.',             createdAt: daysAgo(34), updatedAt: daysAgo(8)  },
  { id: 3,  rfqId: 1, rfqNumber: 'RFQ-2024-001', vendorId: 15, vendorName: 'FutureTech Innovations', status: 'REJECTED',  totalAmount: 49800,  currency: 'USD', validUntil: daysAgo(5),     notes: 'New vendor — pending approval at bid time.',   createdAt: daysAgo(33), updatedAt: daysAgo(8)  },
  // RFQ 2 — CLOSED
  { id: 4,  rfqId: 2, rfqNumber: 'RFQ-2024-002', vendorId: 6,  vendorName: 'BuildRight Contractors', status: 'UNDER_REVIEW', totalAmount: 21000, currency: 'USD', validUntil: daysFromNow(10), notes: 'Competitive pricing.',                        createdAt: daysAgo(25), updatedAt: daysAgo(5)  },
  { id: 5,  rfqId: 2, rfqNumber: 'RFQ-2024-002', vendorId: 12, vendorName: 'SafeGuard Security',  status: 'SUBMITTED',    totalAmount: 24500,  currency: 'USD', validUntil: daysFromNow(10), notes: 'Premium quality, includes assembly.',          createdAt: daysAgo(24), updatedAt: daysAgo(5)  },
  // RFQ 3 — PUBLISHED
  { id: 6,  rfqId: 3, rfqNumber: 'RFQ-2024-003', vendorId: 1,  vendorName: 'TechSupply Co.',      status: 'SUBMITTED',    totalAmount: 68000,  currency: 'USD', validUntil: daysFromNow(14), notes: 'AWS reserved instances — best available rate.', createdAt: daysAgo(10), updatedAt: daysAgo(2)  },
  { id: 7,  rfqId: 3, rfqNumber: 'RFQ-2024-003', vendorId: 8,  vendorName: 'DataVault Consulting', status: 'SUBMITTED',   totalAmount: 71500,  currency: 'USD', validUntil: daysFromNow(14), notes: 'Azure alternative — includes managed services.', createdAt: daysAgo(9), updatedAt: daysAgo(1)  },
  // RFQ 4 — PUBLISHED
  { id: 8,  rfqId: 4, rfqNumber: 'RFQ-2024-004', vendorId: 1,  vendorName: 'TechSupply Co.',      status: 'SUBMITTED',    totalAmount: 52000,  currency: 'USD', validUntil: daysFromNow(10), notes: 'CrowdStrike + Splunk bundle deal.',            createdAt: daysAgo(8),  updatedAt: daysAgo(1)  },
  // RFQ 5 — PUBLISHED
  { id: 9,  rfqId: 5, rfqNumber: 'RFQ-2024-005', vendorId: 5,  vendorName: 'AgroFresh Supplies',  status: 'SUBMITTED',    totalAmount: 115000, currency: 'EUR', validUntil: daysFromNow(21), notes: 'Full-service catering with allergen management.', createdAt: daysAgo(7), updatedAt: daysAgo(1)  },
  { id: 10, rfqId: 5, rfqNumber: 'RFQ-2024-005', vendorId: 10, vendorName: 'EcoPackaging Co.',    status: 'SUBMITTED',    totalAmount: 108000, currency: 'EUR', validUntil: daysFromNow(21), notes: 'Sustainable menus, eco packaging.',            createdAt: daysAgo(6),  updatedAt: daysAgo(1)  },
  // RFQ 6 — CLOSED
  { id: 11, rfqId: 6, rfqNumber: 'RFQ-2024-006', vendorId: 6,  vendorName: 'BuildRight Contractors', status: 'UNDER_REVIEW', totalAmount: 92000, currency: 'USD', validUntil: daysFromNow(5), notes: 'Liebert units, includes 2yr maintenance.',    createdAt: daysAgo(15), updatedAt: daysAgo(2)  },
  { id: 12, rfqId: 6, rfqNumber: 'RFQ-2024-006', vendorId: 4,  vendorName: 'MedEquip Solutions',  status: 'SUBMITTED',    totalAmount: 98500,  currency: 'USD', validUntil: daysFromNow(5), notes: 'Vertiv units — extended warranty included.',  createdAt: daysAgo(14), updatedAt: daysAgo(2)  },
]

type RfqListParams  = { status?: string; search?: string; page?: number; size?: number; sort?: string; dir?: string }

export const mockQuotationsApi = {
  listRfqs: async (params: RfqListParams = {}): Promise<PageResponse<Rfq>> => {
    await delay()
    const { status, search, page, size, sort, dir } = params
    return paginate(rfqs, {
      page, size, sort, dir,
      filter: (r) => {
        if (status && r.status !== status) return false
        return matchesKeyword(r, search, ['rfqNumber', 'title', 'description'])
      },
    })
  },

  getRfqById: async (id: number): Promise<Rfq> => {
    await delay()
    const r = rfqs.find((r) => r.id === id)
    if (!r) throw new Error(`RFQ ${id} not found`)
    return { ...r }
  },

  createRfq: async (req: unknown): Promise<Rfq> => {
    await delay()
    const r = req as Partial<Rfq>
    const id = nextId(rfqs)
    const newRfq: Rfq = {
      id,
      rfqNumber: `RFQ-2024-${String(id).padStart(3, '0')}`,
      title: r.title ?? 'New RFQ',
      description: r.description,
      status: 'DRAFT',
      deadline: r.deadline,
      purchaseRequestId: r.purchaseRequestId,
      createdAt: now(),
      updatedAt: now(),
    }
    rfqs = [newRfq, ...rfqs]
    return newRfq
  },

  publishRfq: async (id: number): Promise<Rfq> => {
    await delay()
    rfqs = rfqs.map((r) => r.id === id ? { ...r, status: 'PUBLISHED', updatedAt: now() } : r)
    return rfqs.find((r) => r.id === id)!
  },

  closeRfq: async (id: number): Promise<Rfq> => {
    await delay()
    rfqs = rfqs.map((r) => r.id === id ? { ...r, status: 'CLOSED', updatedAt: now() } : r)
    return rfqs.find((r) => r.id === id)!
  },

  listQuotations: async (rfqId: number): Promise<Quotation[]> => {
    await delay()
    return quotations.filter((q) => q.rfqId === rfqId).map((q) => ({ ...q }))
  },

  submitQuotation: async (rfqId: number, req: unknown): Promise<Quotation> => {
    await delay()
    const r = req as Partial<Quotation>
    const rfq = rfqs.find((x) => x.id === rfqId)
    const newQ: Quotation = {
      id: nextId(quotations),
      rfqId,
      rfqNumber: rfq?.rfqNumber ?? `RFQ-${rfqId}`,
      vendorId: r.vendorId ?? 1,
      vendorName: r.vendorName ?? 'Unknown Vendor',
      status: 'SUBMITTED',
      totalAmount: r.totalAmount ?? 0,
      currency: r.currency ?? 'USD',
      validUntil: r.validUntil,
      notes: r.notes,
      createdAt: now(),
      updatedAt: now(),
    }
    quotations = [newQ, ...quotations]
    return newQ
  },

  compareQuotations: async (rfqId: number): Promise<Quotation[]> => {
    await delay()
    return quotations.filter((q) => q.rfqId === rfqId).map((q) => ({ ...q }))
  },

  selectQuotation: async (rfqId: number, quotationId: number): Promise<Quotation> => {
    await delay()
    quotations = quotations.map((q) =>
      q.rfqId === rfqId
        ? { ...q, status: q.id === quotationId ? 'SELECTED' : 'REJECTED', updatedAt: now() }
        : q,
    )
    rfqs = rfqs.map((r) => r.id === rfqId ? { ...r, status: 'AWARDED', updatedAt: now() } : r)
    return quotations.find((q) => q.id === quotationId)!
  },
}
