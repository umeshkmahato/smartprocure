import type { PurchaseRequest, PageResponse } from '@/types'
import { delay, paginate, nextId, now, daysAgo, daysFromNow, matchesKeyword } from './utils'

let prs: PurchaseRequest[] = [
  {
    id: 1, prNumber: 'PR-2024-001', title: 'Laptop Procurement — Q1 2024',
    description: 'Purchase 20 high-performance laptops for the engineering team.',
    justification: 'Current laptops are over 4 years old and cannot run modern dev tools efficiently.',
    status: 'DRAFT', department: 'IT', currency: 'USD',
    requiredDate: daysFromNow(30), estimatedTotal: 48000,
    requestedById: 1, requestedByName: 'Alice Johnson',
    items: [
      { id: 1, itemName: 'MacBook Pro 14"', description: 'M3 Pro, 18GB RAM, 512GB SSD', quantity: 15, unit: 'unit', estimatedUnitPrice: 2200, estimatedTotal: 33000 },
      { id: 2, itemName: 'Dell XPS 15',     description: 'i9, 32GB RAM, 1TB SSD',       quantity: 5,  unit: 'unit', estimatedUnitPrice: 3000, estimatedTotal: 15000 },
    ],
    createdAt: daysAgo(5), updatedAt: daysAgo(1),
  },
  {
    id: 2, prNumber: 'PR-2024-002', title: 'Office Furniture — Finance Floor',
    description: 'Ergonomic desks and chairs for the newly renovated finance floor.',
    justification: 'Renovation project requires complete furniture replacement for 15 workstations.',
    status: 'DRAFT', department: 'Facilities', currency: 'USD',
    requiredDate: daysFromNow(45), estimatedTotal: 22500,
    requestedById: 2, requestedByName: 'Bob Martinez',
    items: [
      { id: 3, itemName: 'Standing Desk',    description: 'Electric height-adjustable, 160cm', quantity: 15, unit: 'unit', estimatedUnitPrice: 800,  estimatedTotal: 12000 },
      { id: 4, itemName: 'Ergonomic Chair',  description: 'Lumbar support, mesh back',         quantity: 15, unit: 'unit', estimatedUnitPrice: 700,  estimatedTotal: 10500 },
    ],
    createdAt: daysAgo(3), updatedAt: daysAgo(1),
  },
  {
    id: 3, prNumber: 'PR-2024-003', title: 'Marketing Materials — Brand Refresh',
    description: 'Printed brochures, banners, and branded merchandise for trade show.',
    status: 'DRAFT', department: 'Marketing', currency: 'GBP',
    requiredDate: daysFromNow(14), estimatedTotal: 8750,
    requestedById: 3, requestedByName: 'Carol White',
    items: [
      { id: 5, itemName: 'Printed Brochures', description: 'A4 gloss, full colour, 1000 qty', quantity: 1000, unit: 'pcs', estimatedUnitPrice: 2.50, estimatedTotal: 2500 },
      { id: 6, itemName: 'Pull-up Banners',   description: '2m × 0.8m, with carry case',      quantity: 10,   unit: 'unit', estimatedUnitPrice: 150, estimatedTotal: 1500 },
      { id: 7, itemName: 'Branded Notebooks', description: 'A5, hardcover, 100 pages',         quantity: 300,  unit: 'pcs', estimatedUnitPrice: 15.8, estimatedTotal: 4750 },
    ],
    createdAt: daysAgo(1), updatedAt: daysAgo(1),
  },
  {
    id: 4, prNumber: 'PR-2024-004', title: 'Cloud Infrastructure Upgrade',
    description: 'Annual cloud hosting and licensing costs for SaaS platforms.',
    justification: 'Current plan exceeds capacity — upgrade required before Q2 peak.',
    status: 'SUBMITTED', department: 'IT', currency: 'USD',
    requiredDate: daysFromNow(10), estimatedTotal: 72000,
    requestedById: 1, requestedByName: 'Alice Johnson',
    items: [
      { id: 8,  itemName: 'AWS Reserved Instances', description: '12-month, m5.4xlarge × 4', quantity: 4,  unit: 'instance', estimatedUnitPrice: 9000, estimatedTotal: 36000 },
      { id: 9,  itemName: 'Datadog Monitoring',     description: 'Pro plan, 50 hosts',        quantity: 1,  unit: 'licence',  estimatedUnitPrice: 24000, estimatedTotal: 24000 },
      { id: 10, itemName: 'GitHub Enterprise',      description: 'Annual, 100 seats',          quantity: 1,  unit: 'licence',  estimatedUnitPrice: 12000, estimatedTotal: 12000 },
    ],
    createdAt: daysAgo(10), updatedAt: daysAgo(2),
  },
  {
    id: 5, prNumber: 'PR-2024-005', title: 'Staff Training Programme — Q2',
    description: 'External training courses for HR and Operations teams.',
    status: 'SUBMITTED', department: 'HR', currency: 'GBP',
    requiredDate: daysFromNow(20), estimatedTotal: 15600,
    requestedById: 4, requestedByName: 'Diana Lee',
    items: [
      { id: 11, itemName: 'Leadership Workshop',   description: '2-day, 20 participants', quantity: 20, unit: 'person', estimatedUnitPrice: 450, estimatedTotal: 9000 },
      { id: 12, itemName: 'Compliance E-Learning', description: 'Annual licence, 50 users', quantity: 50, unit: 'licence', estimatedUnitPrice: 132, estimatedTotal: 6600 },
    ],
    createdAt: daysAgo(8), updatedAt: daysAgo(1),
  },
  {
    id: 6, prNumber: 'PR-2024-006', title: 'Medical Supplies Restock',
    description: 'Monthly restock of first-aid and medical supplies for all offices.',
    status: 'SUBMITTED', department: 'Facilities', currency: 'AED',
    requiredDate: daysFromNow(7), estimatedTotal: 18400,
    requestedById: 5, requestedByName: 'Ethan Clarke',
    items: [
      { id: 13, itemName: 'First Aid Kits',       description: 'Large wall-mount kit',  quantity: 20, unit: 'unit', estimatedUnitPrice: 350,  estimatedTotal: 7000 },
      { id: 14, itemName: 'PPE Supply Bundle',    description: 'Gloves, masks, goggles', quantity: 50, unit: 'box',  estimatedUnitPrice: 228,  estimatedTotal: 11400 },
    ],
    createdAt: daysAgo(6), updatedAt: daysAgo(1),
  },
  {
    id: 7, prNumber: 'PR-2024-007', title: 'Server Room Cooling Units',
    description: 'Replacement CRAC units for the primary data centre.',
    justification: 'Existing units have exceeded service life and are causing temperature alerts.',
    status: 'UNDER_REVIEW', department: 'IT', currency: 'USD',
    requiredDate: daysFromNow(21), estimatedTotal: 95000,
    requestedById: 1, requestedByName: 'Alice Johnson',
    items: [
      { id: 15, itemName: 'CRAC Unit 20kW',    description: 'Liebert DS, downflow',    quantity: 3, unit: 'unit', estimatedUnitPrice: 25000, estimatedTotal: 75000 },
      { id: 16, itemName: 'Installation Kit',  description: 'Cables, brackets, labour', quantity: 3, unit: 'set',  estimatedUnitPrice: 5000,  estimatedTotal: 15000 },
      { id: 17, itemName: 'Extended Warranty', description: '5-year on-site warranty',  quantity: 3, unit: 'unit', estimatedUnitPrice: 1667,  estimatedTotal: 5000  },
    ],
    createdAt: daysAgo(14), updatedAt: daysAgo(3),
  },
  {
    id: 8, prNumber: 'PR-2024-008', title: 'Catering Contract — Annual',
    description: 'Annual catering services for head office (breakfast & lunch, 5 days/week).',
    status: 'UNDER_REVIEW', department: 'Facilities', currency: 'EUR',
    requiredDate: daysFromNow(30), estimatedTotal: 120000,
    requestedById: 5, requestedByName: 'Ethan Clarke',
    items: [
      { id: 18, itemName: 'Daily Catering Service', description: '250 covers/day × 250 days', quantity: 250, unit: 'day', estimatedUnitPrice: 480, estimatedTotal: 120000 },
    ],
    createdAt: daysAgo(12), updatedAt: daysAgo(2),
  },
  {
    id: 9, prNumber: 'PR-2024-009', title: 'Security Software Licences',
    description: 'Endpoint protection and SIEM platform licences.',
    status: 'APPROVED', department: 'IT', currency: 'USD',
    requiredDate: daysFromNow(5), estimatedTotal: 54000,
    requestedById: 1, requestedByName: 'Alice Johnson',
    items: [
      { id: 19, itemName: 'CrowdStrike Falcon',   description: 'Enterprise, 500 endpoints', quantity: 500, unit: 'endpoint', estimatedUnitPrice: 60,    estimatedTotal: 30000 },
      { id: 20, itemName: 'Splunk SIEM',           description: 'Annual, 10GB/day ingest',   quantity: 1,   unit: 'licence',  estimatedUnitPrice: 24000, estimatedTotal: 24000 },
    ],
    createdAt: daysAgo(20), updatedAt: daysAgo(5),
  },
  {
    id: 10, prNumber: 'PR-2024-010', title: 'Vehicle Fleet Maintenance',
    description: 'Scheduled maintenance for 12 company vehicles.',
    status: 'APPROVED', department: 'Operations', currency: 'AED',
    requiredDate: daysFromNow(15), estimatedTotal: 42000,
    requestedById: 5, requestedByName: 'Ethan Clarke',
    items: [
      { id: 21, itemName: 'Full Service — Sedan',  description: 'Oil, filters, brake check',  quantity: 8, unit: 'vehicle', estimatedUnitPrice: 2500, estimatedTotal: 20000 },
      { id: 22, itemName: 'Full Service — SUV',    description: 'Oil, filters, tyre rotation', quantity: 4, unit: 'vehicle', estimatedUnitPrice: 5500, estimatedTotal: 22000 },
    ],
    createdAt: daysAgo(25), updatedAt: daysAgo(7),
  },
  {
    id: 11, prNumber: 'PR-2024-011', title: 'HR Software Upgrade',
    description: 'Upgrade HRMS to include payroll and performance management modules.',
    status: 'APPROVED', department: 'HR', currency: 'USD',
    requiredDate: daysFromNow(60), estimatedTotal: 36000,
    requestedById: 4, requestedByName: 'Diana Lee',
    items: [
      { id: 23, itemName: 'HRMS Pro Licence',      description: 'Annual, 200 employees',    quantity: 1, unit: 'licence', estimatedUnitPrice: 24000, estimatedTotal: 24000 },
      { id: 24, itemName: 'Implementation Services', description: 'Migration & training',   quantity: 1, unit: 'project', estimatedUnitPrice: 12000, estimatedTotal: 12000 },
    ],
    createdAt: daysAgo(30), updatedAt: daysAgo(8),
  },
  {
    id: 12, prNumber: 'PR-2024-012', title: 'Cleaning Services Contract',
    description: 'Daily office cleaning for 3 office locations.',
    status: 'APPROVED', department: 'Facilities', currency: 'SGD',
    requiredDate: daysFromNow(10), estimatedTotal: 28800,
    requestedById: 5, requestedByName: 'Ethan Clarke',
    items: [
      { id: 25, itemName: 'Office Cleaning Service', description: 'Daily, 3 locations × 12 months', quantity: 36, unit: 'month', estimatedUnitPrice: 800, estimatedTotal: 28800 },
    ],
    createdAt: daysAgo(18), updatedAt: daysAgo(4),
  },
  {
    id: 13, prNumber: 'PR-2024-013', title: 'Legacy Printer Replacement',
    description: 'Replace 10 ageing printers across all departments.',
    justification: 'High maintenance cost and recurring downtime.',
    status: 'REJECTED', department: 'IT', currency: 'USD',
    requiredDate: daysFromNow(30), estimatedTotal: 25000,
    requestedById: 1, requestedByName: 'Alice Johnson',
    items: [
      { id: 26, itemName: 'HP LaserJet Pro MFP', description: 'A3, colour, duplex', quantity: 10, unit: 'unit', estimatedUnitPrice: 2500, estimatedTotal: 25000 },
    ],
    createdAt: daysAgo(45), updatedAt: daysAgo(15),
  },
  {
    id: 14, prNumber: 'PR-2024-014', title: 'Team Building Events — Annual',
    description: 'Company-wide team building activities for 200 staff.',
    status: 'REJECTED', department: 'HR', currency: 'GBP',
    requiredDate: daysFromNow(90), estimatedTotal: 40000,
    requestedById: 4, requestedByName: 'Diana Lee',
    items: [
      { id: 27, itemName: 'Team Building Day',  description: 'Outdoor activity, 200 pax', quantity: 1, unit: 'event', estimatedUnitPrice: 25000, estimatedTotal: 25000 },
      { id: 28, itemName: 'Gala Dinner',        description: 'Venue, catering, AV',       quantity: 1, unit: 'event', estimatedUnitPrice: 15000, estimatedTotal: 15000 },
    ],
    createdAt: daysAgo(60), updatedAt: daysAgo(20),
  },
  {
    id: 15, prNumber: 'PR-2024-015', title: 'Emergency UPS Battery Replacement',
    description: 'Replacement batteries for server room UPS systems.',
    status: 'CANCELLED', department: 'IT', currency: 'USD',
    requiredDate: daysFromNow(5), estimatedTotal: 12000,
    requestedById: 1, requestedByName: 'Alice Johnson',
    items: [
      { id: 29, itemName: 'UPS Battery Module', description: '48V, 200Ah, compatible with APC SURT', quantity: 6, unit: 'unit', estimatedUnitPrice: 2000, estimatedTotal: 12000 },
    ],
    createdAt: daysAgo(50), updatedAt: daysAgo(40),
  },
]

type ListParams = { status?: string; keyword?: string; page?: number; size?: number; sort?: string; dir?: string }

export const mockPurchaseRequestsApi = {
  list: async (params: ListParams = {}): Promise<PageResponse<PurchaseRequest>> => {
    await delay()
    const { status, keyword, page, size, sort, dir } = params
    return paginate(prs, {
      page, size, sort, dir,
      filter: (pr) => {
        if (status && pr.status !== status) return false
        return matchesKeyword(pr, keyword, ['prNumber', 'title', 'department', 'requestedByName'])
      },
    })
  },

  getById: async (id: number): Promise<PurchaseRequest> => {
    await delay()
    const pr = prs.find((p) => p.id === id)
    if (!pr) throw new Error(`Purchase request ${id} not found`)
    return { ...pr, items: [...pr.items] }
  },

  create: async (req: unknown): Promise<PurchaseRequest> => {
    await delay()
    const r = req as Partial<PurchaseRequest>
    const newPr: PurchaseRequest = {
      id: nextId(prs),
      prNumber: `PR-2024-${String(nextId(prs)).padStart(3, '0')}`,
      title: r.title ?? 'New Request',
      description: r.description,
      justification: r.justification,
      status: 'DRAFT',
      department: r.department,
      currency: r.currency ?? 'USD',
      requiredDate: r.requiredDate,
      estimatedTotal: r.estimatedTotal ?? 0,
      requestedById: 1,
      requestedByName: 'Admin User',
      items: r.items ?? [],
      createdAt: now(),
      updatedAt: now(),
    }
    prs = [newPr, ...prs]
    return newPr
  },

  update: async (id: number, req: unknown): Promise<PurchaseRequest> => {
    await delay()
    const r = req as Partial<PurchaseRequest>
    prs = prs.map((p) => p.id === id ? { ...p, ...r, updatedAt: now() } : p)
    return prs.find((p) => p.id === id)!
  },

  submit: async (id: number): Promise<PurchaseRequest> => {
    await delay()
    prs = prs.map((p) => p.id === id ? { ...p, status: 'SUBMITTED', updatedAt: now() } : p)
    return prs.find((p) => p.id === id)!
  },

  cancel: async (id: number): Promise<PurchaseRequest> => {
    await delay()
    prs = prs.map((p) => p.id === id ? { ...p, status: 'CANCELLED', updatedAt: now() } : p)
    return prs.find((p) => p.id === id)!
  },
}
