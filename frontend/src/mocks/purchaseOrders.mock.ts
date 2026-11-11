import type { PurchaseOrder, CreatePoRequest, PageResponse } from '@/types'
import { delay, paginate, nextId, now, daysAgo, daysFromNow, matchesKeyword } from './utils'

let pos: PurchaseOrder[] = [
  {
    id: 1, poNumber: 'PO-2024-001', vendorId: 1, vendorName: 'TechSupply Co.',
    status: 'CLOSED', currency: 'USD', totalAmount: 46500,
    deliveryAddress: '123 HQ Building, San Francisco, USA',
    deliveryDate: daysAgo(10), notes: 'Delivered and closed.',
    purchaseRequestId: 1, quotationId: 1,
    items: [
      { id: 1, itemName: 'MacBook Pro 14"', description: 'M3 Pro, 18GB RAM, 512GB SSD', quantity: 15, unitPrice: 2200, totalPrice: 33000, deliveredQty: 15 },
      { id: 2, itemName: 'Dell XPS 15',     description: 'i9, 32GB RAM, 1TB SSD',       quantity: 5,  unitPrice: 3000, totalPrice: 15000, deliveredQty: 5  },
    ],
    createdAt: daysAgo(35), updatedAt: daysAgo(5),
  },
  {
    id: 2, poNumber: 'PO-2024-002', vendorId: 2, vendorName: 'GlobalParts Ltd.',
    status: 'INVOICED', currency: 'GBP', totalAmount: 21000,
    deliveryAddress: '45 Finance Floor, London, UK',
    deliveryDate: daysAgo(3), notes: 'Goods received. Invoice under review.',
    purchaseRequestId: 2, quotationId: 2,
    items: [
      { id: 3, itemName: 'Standing Desk',   description: 'Electric height-adjustable', quantity: 15, unitPrice: 800, totalPrice: 12000, deliveredQty: 15 },
      { id: 4, itemName: 'Ergonomic Chair', description: 'Lumbar support, mesh back',  quantity: 15, unitPrice: 600, totalPrice: 9000,  deliveredQty: 15 },
    ],
    createdAt: daysAgo(28), updatedAt: daysAgo(3),
  },
  {
    id: 3, poNumber: 'PO-2024-003', vendorId: 4, vendorName: 'MedEquip Solutions',
    status: 'DELIVERED', currency: 'AED', totalAmount: 18400,
    deliveryAddress: 'Block 5, Healthcare City, Dubai',
    deliveryDate: daysAgo(1), notes: 'All items delivered. Awaiting invoice.',
    purchaseRequestId: 6,
    items: [
      { id: 5, itemName: 'First Aid Kits',    description: 'Large wall-mount kit',   quantity: 20, unitPrice: 350, totalPrice: 7000,  deliveredQty: 20 },
      { id: 6, itemName: 'PPE Supply Bundle', description: 'Gloves, masks, goggles', quantity: 50, unitPrice: 228, totalPrice: 11400, deliveredQty: 50 },
    ],
    createdAt: daysAgo(20), updatedAt: daysAgo(1),
  },
  {
    id: 4, poNumber: 'PO-2024-004', vendorId: 8, vendorName: 'DataVault Consulting',
    status: 'PARTIALLY_DELIVERED', currency: 'USD', totalAmount: 72000,
    deliveryAddress: '55 Tech Park, Austin, USA',
    deliveryDate: daysFromNow(5), notes: 'AWS instances provisioned. Datadog and GitHub pending.',
    purchaseRequestId: 4, quotationId: 3,
    items: [
      { id: 7, itemName: 'AWS Reserved Instances', description: '12-month, m5.4xlarge × 4', quantity: 4,  unitPrice: 9000,  totalPrice: 36000, deliveredQty: 4  },
      { id: 8, itemName: 'Datadog Monitoring',     description: 'Pro plan, 50 hosts',        quantity: 1,  unitPrice: 24000, totalPrice: 24000, deliveredQty: 0  },
      { id: 9, itemName: 'GitHub Enterprise',      description: 'Annual, 100 seats',          quantity: 1,  unitPrice: 12000, totalPrice: 12000, deliveredQty: 0  },
    ],
    createdAt: daysAgo(14), updatedAt: daysAgo(2),
  },
  {
    id: 5, poNumber: 'PO-2024-005', vendorId: 1, vendorName: 'TechSupply Co.',
    status: 'ACKNOWLEDGED', currency: 'USD', totalAmount: 54000,
    deliveryAddress: '300 HQ, Seattle, USA',
    deliveryDate: daysFromNow(7), notes: 'Vendor acknowledged. Delivery scheduled.',
    purchaseRequestId: 9, quotationId: 4,
    items: [
      { id: 10, itemName: 'CrowdStrike Falcon', description: 'Enterprise, 500 endpoints', quantity: 500, unitPrice: 60,    totalPrice: 30000, deliveredQty: 0 },
      { id: 11, itemName: 'Splunk SIEM',        description: 'Annual, 10GB/day ingest',   quantity: 1,   unitPrice: 24000, totalPrice: 24000, deliveredQty: 0 },
    ],
    createdAt: daysAgo(10), updatedAt: daysAgo(3),
  },
  {
    id: 6, poNumber: 'PO-2024-006', vendorId: 11, vendorName: 'Nexus HR Solutions',
    status: 'ACKNOWLEDGED', currency: 'SGD', totalAmount: 28800,
    deliveryAddress: '10 Marina Boulevard, Singapore',
    deliveryDate: daysFromNow(10), notes: 'Contract signed, service start date confirmed.',
    purchaseRequestId: 12,
    items: [
      { id: 12, itemName: 'Office Cleaning Service', description: 'Daily, 3 locations × 12 months', quantity: 36, unitPrice: 800, totalPrice: 28800, deliveredQty: 0 },
    ],
    createdAt: daysAgo(8), updatedAt: daysAgo(2),
  },
  {
    id: 7, poNumber: 'PO-2024-007', vendorId: 6, vendorName: 'BuildRight Contractors',
    status: 'ISSUED', currency: 'USD', totalAmount: 92000,
    deliveryAddress: 'Server Room B, 890 Construction Blvd, Chicago',
    deliveryDate: daysFromNow(14), notes: 'PO issued. Awaiting vendor acknowledgement.',
    purchaseRequestId: 7, quotationId: 6,
    items: [
      { id: 13, itemName: 'CRAC Unit 20kW',    description: 'Liebert DS, downflow',    quantity: 3, unitPrice: 25000, totalPrice: 75000, deliveredQty: 0 },
      { id: 14, itemName: 'Installation Kit',  description: 'Cables, brackets, labour', quantity: 3, unitPrice: 4000,  totalPrice: 12000, deliveredQty: 0 },
      { id: 15, itemName: 'Extended Warranty', description: '5-year on-site warranty',  quantity: 3, unitPrice: 1667,  totalPrice: 5000,  deliveredQty: 0 },
    ],
    createdAt: daysAgo(5), updatedAt: daysAgo(1),
  },
  {
    id: 8, poNumber: 'PO-2024-008', vendorId: 3, vendorName: 'SwiftLogistics Inc.',
    status: 'ISSUED', currency: 'SGD', totalAmount: 36000,
    deliveryAddress: '100 Operations Depot, Singapore',
    deliveryDate: daysFromNow(21), notes: 'PO issued for logistics services.',
    purchaseRequestId: 10,
    items: [
      { id: 16, itemName: 'Logistics Services', description: 'Monthly freight — 12 months', quantity: 12, unitPrice: 3000, totalPrice: 36000, deliveredQty: 0 },
    ],
    createdAt: daysAgo(4), updatedAt: daysAgo(1),
  },
  {
    id: 9, poNumber: 'PO-2024-009', vendorId: 7, vendorName: 'PrimePrint Agency',
    status: 'DRAFT', currency: 'GBP', totalAmount: 8750,
    deliveryAddress: '12 Creative Quarter, Birmingham',
    deliveryDate: daysFromNow(10), notes: 'Draft PO for review before issuance.',
    purchaseRequestId: 3,
    items: [
      { id: 17, itemName: 'Printed Brochures', description: 'A4 gloss, 1000 qty',  quantity: 1000, unitPrice: 2.50, totalPrice: 2500, deliveredQty: 0 },
      { id: 18, itemName: 'Pull-up Banners',   description: '2m × 0.8m',           quantity: 10,   unitPrice: 150,  totalPrice: 1500, deliveredQty: 0 },
      { id: 19, itemName: 'Branded Notebooks', description: 'A5, hardcover',       quantity: 300,  unitPrice: 15.8, totalPrice: 4750, deliveredQty: 0 },
    ],
    createdAt: daysAgo(2), updatedAt: daysAgo(1),
  },
  {
    id: 10, poNumber: 'PO-2024-010', vendorId: 5, vendorName: 'AgroFresh Supplies',
    status: 'DRAFT', currency: 'EUR', totalAmount: 115000,
    deliveryAddress: 'Head Office Canteen, Berlin',
    deliveryDate: daysFromNow(30), notes: 'Pending legal review before issuance.',
    purchaseRequestId: 8, quotationId: 5,
    items: [
      { id: 20, itemName: 'Daily Catering Service', description: '250 covers × 250 days', quantity: 250, unitPrice: 460, totalPrice: 115000, deliveredQty: 0 },
    ],
    createdAt: daysAgo(3), updatedAt: daysAgo(1),
  },
  {
    id: 11, poNumber: 'PO-2024-011', vendorId: 8, vendorName: 'DataVault Consulting',
    status: 'CANCELLED', currency: 'USD', totalAmount: 25000,
    deliveryAddress: 'Various Office Locations',
    deliveryDate: daysAgo(20), notes: 'Cancelled — scope changed after budget review.',
    purchaseRequestId: 13,
    items: [
      { id: 21, itemName: 'HP LaserJet Pro MFP', description: 'A3, colour, duplex', quantity: 10, unitPrice: 2500, totalPrice: 25000, deliveredQty: 0 },
    ],
    createdAt: daysAgo(50), updatedAt: daysAgo(40),
  },
  {
    id: 12, poNumber: 'PO-2024-012', vendorId: 12, vendorName: 'SafeGuard Security',
    status: 'ISSUED', currency: 'AED', totalAmount: 42000,
    deliveryAddress: 'All Office Locations, UAE',
    deliveryDate: daysFromNow(15), notes: 'Fleet maintenance contracts — PO issued.',
    purchaseRequestId: 10,
    items: [
      { id: 22, itemName: 'Full Service — Sedan', description: 'Oil, filters, brake check',  quantity: 8, unitPrice: 2500, totalPrice: 20000, deliveredQty: 0 },
      { id: 23, itemName: 'Full Service — SUV',   description: 'Oil, filters, tyre rotation', quantity: 4, unitPrice: 5500, totalPrice: 22000, deliveredQty: 0 },
    ],
    createdAt: daysAgo(6), updatedAt: daysAgo(1),
  },
]

type ListParams = { status?: string; vendorId?: number; keyword?: string; page?: number; size?: number; sort?: string; dir?: string }

export const mockPurchaseOrdersApi = {
  list: async (params: ListParams = {}): Promise<PageResponse<PurchaseOrder>> => {
    await delay()
    const { status, vendorId, keyword, page, size, sort, dir } = params
    return paginate(pos, {
      page, size, sort, dir,
      filter: (po) => {
        if (status && po.status !== status) return false
        if (vendorId && po.vendorId !== vendorId) return false
        return matchesKeyword(po, keyword, ['poNumber', 'vendorName'])
      },
    })
  },

  getById: async (id: number): Promise<PurchaseOrder> => {
    await delay()
    const po = pos.find((p) => p.id === id)
    if (!po) throw new Error(`Purchase order ${id} not found`)
    return { ...po, items: po.items.map((i) => ({ ...i })) }
  },

  getByPoNumber: async (poNumber: string): Promise<PurchaseOrder> => {
    await delay()
    const po = pos.find((p) => p.poNumber === poNumber)
    if (!po) throw new Error(`PO ${poNumber} not found`)
    return { ...po }
  },

  create: async (req: CreatePoRequest): Promise<PurchaseOrder> => {
    await delay()
    const id = nextId(pos)
    const total = req.items.reduce((s, i) => s + i.quantity * i.unitPrice, 0)
    const newPo: PurchaseOrder = {
      id,
      poNumber: `PO-2024-${String(id).padStart(3, '0')}`,
      vendorId: req.vendorId,
      vendorName: 'Unknown Vendor',
      status: 'DRAFT',
      currency: req.currency ?? 'USD',
      totalAmount: total,
      deliveryAddress: req.deliveryAddress,
      deliveryDate: req.deliveryDate,
      paymentTerms: req.paymentTerms,
      notes: req.notes,
      purchaseRequestId: req.purchaseRequestId,
      quotationId: req.quotationId,
      items: req.items.map((item, i) => ({
        id: i + 1,
        itemName: item.itemName,
        description: item.description,
        unit: item.unit,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        totalPrice: item.quantity * item.unitPrice,
        deliveredQty: 0,
        notes: item.notes,
      })),
      createdAt: now(),
      updatedAt: now(),
    }
    pos = [newPo, ...pos]
    return newPo
  },

  update: async (id: number, req: unknown): Promise<PurchaseOrder> => {
    await delay()
    pos = pos.map((p) => p.id === id ? { ...p, ...(req as Partial<PurchaseOrder>), updatedAt: now() } : p)
    return pos.find((p) => p.id === id)!
  },

  issue: async (id: number): Promise<PurchaseOrder> => {
    await delay()
    pos = pos.map((p) => p.id === id ? { ...p, status: 'ISSUED', updatedAt: now() } : p)
    return pos.find((p) => p.id === id)!
  },

  acknowledge: async (id: number): Promise<PurchaseOrder> => {
    await delay()
    pos = pos.map((p) => p.id === id ? { ...p, status: 'ACKNOWLEDGED', updatedAt: now() } : p)
    return pos.find((p) => p.id === id)!
  },

  deliver: async (id: number, lines: { itemId: number; deliveredQty: number }[]): Promise<PurchaseOrder> => {
    await delay()
    pos = pos.map((p) => {
      if (p.id !== id) return p
      const updatedItems = p.items.map((item) => {
        const line = lines.find((l) => l.itemId === item.id)
        return line ? { ...item, deliveredQty: item.deliveredQty + line.deliveredQty } : item
      })
      const allDelivered = updatedItems.every((i) => i.deliveredQty >= i.quantity)
      return {
        ...p,
        items: updatedItems,
        status: allDelivered ? 'DELIVERED' : 'PARTIALLY_DELIVERED',
        updatedAt: now(),
      }
    })
    return pos.find((p) => p.id === id)!
  },

  cancel: async (id: number): Promise<PurchaseOrder> => {
    await delay()
    pos = pos.map((p) => p.id === id ? { ...p, status: 'CANCELLED', updatedAt: now() } : p)
    return pos.find((p) => p.id === id)!
  },

  close: async (id: number): Promise<PurchaseOrder> => {
    await delay()
    pos = pos.map((p) => p.id === id ? { ...p, status: 'CLOSED', updatedAt: now() } : p)
    return pos.find((p) => p.id === id)!
  },
}
