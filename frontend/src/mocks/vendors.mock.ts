import type { Vendor, CreateVendorRequest, PageResponse } from '@/types'
import { delay, paginate, nextId, now, daysAgo } from './utils'

let vendors: Vendor[] = [
  { id: 1,  name: 'TechSupply Co.',        email: 'sales@techsupply.com',      phone: '+1-555-0101', address: '123 Silicon Ave',           city: 'San Francisco', country: 'USA',       taxId: 'US-TAX-001', registrationNumber: 'REG-TS-001', category: 'Technology',      currency: 'USD', status: 'APPROVED',    rating: 4.5, createdAt: daysAgo(180), updatedAt: daysAgo(30) },
  { id: 2,  name: 'GlobalParts Ltd.',       email: 'info@globalparts.co.uk',    phone: '+44-20-7946-0958', address: '45 Trade Street',      city: 'London',        country: 'UK',        taxId: 'GB-VAT-02345', registrationNumber: 'REG-GP-002', category: 'Manufacturing',   currency: 'GBP', status: 'APPROVED',    rating: 4.2, createdAt: daysAgo(150), updatedAt: daysAgo(20) },
  { id: 3,  name: 'SwiftLogistics Inc.',    email: 'ops@swiftlogistics.com',    phone: '+65-6789-1234', address: '78 Harbour Drive',          city: 'Singapore',     country: 'Singapore', taxId: 'SG-TAX-003', registrationNumber: 'REG-SL-003', category: 'Logistics',       currency: 'SGD', status: 'APPROVED',    rating: 3.8, createdAt: daysAgo(120), updatedAt: daysAgo(15) },
  { id: 4,  name: 'MedEquip Solutions',     email: 'orders@medequip.ae',        phone: '+971-4-567-8901', address: 'Block 5, Healthcare City', city: 'Dubai',        country: 'UAE',       taxId: 'AE-TAX-004', registrationNumber: 'REG-ME-004', category: 'Healthcare',      currency: 'AED', status: 'APPROVED',    rating: 4.8, createdAt: daysAgo(200), updatedAt: daysAgo(5)  },
  { id: 5,  name: 'AgroFresh Supplies',     email: 'fresh@agrofresh.de',        phone: '+49-30-123-4567', address: 'Marktstraße 22',          city: 'Berlin',        country: 'Germany',   taxId: 'DE-TAX-005', registrationNumber: 'REG-AF-005', category: 'Food & Beverage', currency: 'EUR', status: 'APPROVED',    rating: 4.0, createdAt: daysAgo(90),  updatedAt: daysAgo(10) },
  { id: 6,  name: 'BuildRight Contractors', email: 'quote@buildright.com',      phone: '+1-555-0202',  address: '890 Construction Blvd',    city: 'Chicago',       country: 'USA',       taxId: 'US-TAX-006', registrationNumber: 'REG-BR-006', category: 'Facilities',      currency: 'USD', status: 'APPROVED',    rating: 3.5, createdAt: daysAgo(60),  updatedAt: daysAgo(7)  },
  { id: 7,  name: 'PrimePrint Agency',      email: 'studio@primeprint.com',     phone: '+44-121-567-8901', address: '12 Creative Quarter',   city: 'Birmingham',    country: 'UK',        taxId: 'GB-VAT-00789', registrationNumber: 'REG-PP-007', category: 'Marketing',       currency: 'GBP', status: 'APPROVED',    rating: 4.1, createdAt: daysAgo(45),  updatedAt: daysAgo(3)  },
  { id: 8,  name: 'DataVault Consulting',   email: 'hello@datavault.io',        phone: '+1-555-0303',  address: '55 Tech Park',             city: 'Austin',        country: 'USA',       taxId: 'US-TAX-008', registrationNumber: 'REG-DV-008', category: 'Consulting',      currency: 'USD', status: 'APPROVED',    rating: 4.7, createdAt: daysAgo(30),  updatedAt: daysAgo(2)  },
  { id: 9,  name: 'CloudNine Networks',     email: 'enterprise@cloudnine.net',  phone: '+1-555-0404',  address: '300 Cyber Lane',           city: 'Seattle',       country: 'USA',       taxId: 'US-TAX-009', registrationNumber: 'REG-CN-009', category: 'Technology',      currency: 'USD', status: 'APPROVED',    rating: 4.3, createdAt: daysAgo(25),  updatedAt: daysAgo(1)  },
  { id: 10, name: 'EcoPackaging Co.',       email: 'sales@ecopackaging.de',     phone: '+49-89-234-5678', address: 'Grünestraße 7',          city: 'Munich',        country: 'Germany',   taxId: 'DE-TAX-010', registrationNumber: 'REG-EP-010', category: 'Manufacturing',   currency: 'EUR', status: 'APPROVED',    rating: 3.9, createdAt: daysAgo(20),  updatedAt: daysAgo(1)  },
  { id: 11, name: 'Nexus HR Solutions',     email: 'info@nexushr.com',          phone: '+65-6234-5678', address: '10 Marina Boulevard',      city: 'Singapore',     country: 'Singapore', taxId: 'SG-TAX-011', registrationNumber: 'REG-NH-011', category: 'Consulting',      currency: 'SGD', status: 'APPROVED',    rating: 4.4, createdAt: daysAgo(15),  updatedAt: daysAgo(1)  },
  { id: 12, name: 'SafeGuard Security',     email: 'contracts@safeguard.ae',    phone: '+971-2-345-6789', address: 'Tower B, Business Bay', city: 'Abu Dhabi',     country: 'UAE',       taxId: 'AE-TAX-012', registrationNumber: 'REG-SG-012', category: 'Facilities',      currency: 'AED', status: 'APPROVED',    rating: 4.6, createdAt: daysAgo(10),  updatedAt: daysAgo(1)  },
  { id: 13, name: 'NewHorizon Traders',     email: 'trade@newhorizon.com',      phone: '+1-555-0505',  address: '44 Commerce Street',       city: 'New York',      country: 'USA',       taxId: 'US-TAX-013', registrationNumber: 'REG-NT-013', category: 'Logistics',       currency: 'USD', status: 'PENDING',     rating: undefined, createdAt: daysAgo(5),   updatedAt: daysAgo(5)  },
  { id: 14, name: 'AlphaOmega Supplies',    email: 'purchasing@alphaomega.com', phone: '+33-1-23-45-67-89', address: '8 Rue de Commerce',  city: 'Paris',         country: 'France',    taxId: 'FR-TAX-014', registrationNumber: 'REG-AO-014', category: 'Manufacturing',   currency: 'EUR', status: 'PENDING',     rating: undefined, createdAt: daysAgo(3),   updatedAt: daysAgo(3)  },
  { id: 15, name: 'FutureTech Innovations', email: 'bd@futuretech.io',          phone: '+1-555-0606',  address: '99 Innovation Drive',      city: 'Boston',        country: 'USA',       taxId: 'US-TAX-015', registrationNumber: 'REG-FT-015', category: 'Technology',      currency: 'USD', status: 'PENDING',     rating: undefined, createdAt: daysAgo(2),   updatedAt: daysAgo(2)  },
  { id: 16, name: 'PacificRim Importers',   email: 'trade@pacificrim.com',      phone: '+81-3-1234-5678', address: 'Shinjuku Business Ctr', city: 'Tokyo',         country: 'Japan',     taxId: 'JP-TAX-016', registrationNumber: 'REG-PR-016', category: 'Logistics',       currency: 'JPY', status: 'PENDING',     rating: undefined, createdAt: daysAgo(1),   updatedAt: daysAgo(1)  },
  { id: 17, name: 'OldReliable Corp.',      email: 'admin@oldreliable.com',     phone: '+1-555-0707',  address: '500 Main Street',          city: 'Detroit',       country: 'USA',       taxId: 'US-TAX-017', registrationNumber: 'REG-OR-017', category: 'Manufacturing',   currency: 'USD', status: 'SUSPENDED',   rating: 2.1, createdAt: daysAgo(365), updatedAt: daysAgo(60) },
  { id: 18, name: 'QuickFix Services',      email: 'help@quickfix.co.uk',       phone: '+44-161-345-6789', address: '7 Fix-It Lane',       city: 'Manchester',    country: 'UK',        taxId: 'GB-VAT-01122', registrationNumber: 'REG-QF-018', category: 'Facilities',      currency: 'GBP', status: 'SUSPENDED',   rating: 2.8, createdAt: daysAgo(300), updatedAt: daysAgo(45) },
  { id: 19, name: 'ShadyDeals Inc.',        email: 'noreply@shadydeals.com',    phone: '+1-555-0808',  address: 'Unknown Location',         city: 'Las Vegas',     country: 'USA',       taxId: 'US-TAX-019', registrationNumber: 'REG-SD-019', category: 'Unknown',         currency: 'USD', status: 'BLACKLISTED', rating: 1.0, createdAt: daysAgo(500), updatedAt: daysAgo(100) },
  { id: 20, name: 'FraudAlert Traders',     email: 'fraud@fraudalert.com',      phone: '+1-555-0909',  address: 'P.O. Box 999',             city: 'Miami',         country: 'USA',       taxId: 'US-TAX-020', registrationNumber: 'REG-FA-020', category: 'Unknown',         currency: 'USD', status: 'BLACKLISTED', rating: 1.2, createdAt: daysAgo(400), updatedAt: daysAgo(90) },
]

type ListParams = { status?: string; keyword?: string; page?: number; size?: number; sort?: string; dir?: string }

export const mockVendorsApi = {
  list: async (params: ListParams = {}): Promise<PageResponse<Vendor>> => {
    await delay()
    const { status, keyword, page, size, sort, dir } = params
    return paginate(vendors, {
      page, size, sort, dir,
      filter: (v) => {
        if (status && v.status !== status) return false
        if (keyword) {
          const kw = keyword.toLowerCase()
          return [v.name, v.email, v.city, v.country, v.category]
            .some((f) => f?.toLowerCase().includes(kw))
        }
        return true
      },
    })
  },

  getById: async (id: number): Promise<Vendor> => {
    await delay()
    const v = vendors.find((v) => v.id === id)
    if (!v) throw new Error(`Vendor ${id} not found`)
    return { ...v }
  },

  create: async (req: CreateVendorRequest): Promise<Vendor> => {
    await delay()
    const newV: Vendor = { id: nextId(vendors), ...req, status: 'PENDING', createdAt: now(), updatedAt: now() }
    vendors = [newV, ...vendors]
    return newV
  },

  update: async (id: number, req: Partial<CreateVendorRequest>): Promise<Vendor> => {
    await delay()
    vendors = vendors.map((v) => v.id === id ? { ...v, ...req, updatedAt: now() } : v)
    return vendors.find((v) => v.id === id)!
  },

  approve: async (id: number): Promise<Vendor> => {
    await delay()
    vendors = vendors.map((v) => v.id === id ? { ...v, status: 'APPROVED', updatedAt: now() } : v)
    return vendors.find((v) => v.id === id)!
  },

  suspend: async (id: number, _reason: string): Promise<Vendor> => {
    await delay()
    vendors = vendors.map((v) => v.id === id ? { ...v, status: 'SUSPENDED', updatedAt: now() } : v)
    return vendors.find((v) => v.id === id)!
  },

  blacklist: async (id: number, _reason: string): Promise<Vendor> => {
    await delay()
    vendors = vendors.map((v) => v.id === id ? { ...v, status: 'BLACKLISTED', updatedAt: now() } : v)
    return vendors.find((v) => v.id === id)!
  },

  recommend: async (_description: string): Promise<string> => {
    await delay(900)
    return (
      'Based on your requirements, I recommend **TechSupply Co.** (rating: 4.5/5, category: Technology, USD) ' +
      'or **DataVault Consulting** (rating: 4.7/5, category: Consulting, USD). ' +
      'Both have strong track records and are currently approved vendors.'
    )
  },
}
