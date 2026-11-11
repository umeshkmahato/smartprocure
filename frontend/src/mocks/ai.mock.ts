import type { AiSimilarVendor } from '@/types'
import { delay } from './utils'

const CHAT_RESPONSES: Record<string, string> = {
  default:
    'I can help you with procurement decisions, vendor recommendations, spend analysis, and purchase request guidance. What would you like to know?',
  vendor:
    '**Top recommended vendors** based on your query:\n\n' +
    '1. **TechSupply Co.** — Rating 4.5/5, Technology category, USD, 180+ days approved\n' +
    '2. **DataVault Consulting** — Rating 4.7/5, Consulting, USD, strong delivery record\n' +
    '3. **MedEquip Solutions** — Rating 4.8/5, Healthcare, AED, fastest turnaround\n\n' +
    'Would you like to create an RFQ for any of these vendors?',
  spend:
    '**Spend Summary (YTD)**\n\n' +
    '- Total PO Value: **$514,450**\n' +
    '- Invoices Paid: **$55,500**\n' +
    '- Pending Approval: **$307,600**\n' +
    '- Top Category: Technology (38%)\n\n' +
    'Spend is 12% higher than the same period last year.',
  approve:
    'You have **5 pending approvals** requiring your attention:\n\n' +
    '- PR-2024-004 Cloud Infrastructure Upgrade ($72,000)\n' +
    '- PR-2024-005 Staff Training Programme ($15,600)\n' +
    '- PR-2024-006 Medical Supplies Restock ($18,400)\n' +
    '- PR-2024-007 Server Room Cooling Units ($95,000)\n' +
    '- PR-2024-008 Catering Contract ($120,000)\n\n' +
    'Navigate to **Approvals** to action these.',
  po:
    '**Purchase Order Status**\n\n' +
    '- Draft: 2 | Issued: 3 | Acknowledged: 2\n' +
    '- Partially Delivered: 1 | Delivered: 1\n' +
    '- Invoiced: 1 | Closed: 1 | Cancelled: 1\n\n' +
    'All issued POs are on track. PO-2024-004 has a partial delivery pending.',
}

function pickResponse(message: string): string {
  const m = message.toLowerCase()
  if (m.includes('vendor') || m.includes('recommend') || m.includes('supplier')) return CHAT_RESPONSES.vendor
  if (m.includes('spend') || m.includes('budget') || m.includes('cost') || m.includes('total')) return CHAT_RESPONSES.spend
  if (m.includes('approv') || m.includes('pending') || m.includes('review')) return CHAT_RESPONSES.approve
  if (m.includes('order') || m.includes('po ') || m.includes('purchase order')) return CHAT_RESPONSES.po
  return CHAT_RESPONSES.default
}

const SIMILAR_VENDORS: AiSimilarVendor[] = [
  { vendorId: 1,  vendorName: 'TechSupply Co.',      similarity: 0.97, category: 'Technology',    status: 'APPROVED' },
  { vendorId: 8,  vendorName: 'DataVault Consulting', similarity: 0.94, category: 'Consulting',    status: 'APPROVED' },
  { vendorId: 9,  vendorName: 'CloudNine Networks',   similarity: 0.91, category: 'Technology',    status: 'APPROVED' },
  { vendorId: 4,  vendorName: 'MedEquip Solutions',   similarity: 0.88, category: 'Healthcare',    status: 'APPROVED' },
  { vendorId: 11, vendorName: 'Nexus HR Solutions',   similarity: 0.85, category: 'Consulting',    status: 'APPROVED' },
]

export const mockAiApi = {
  chat: async (message: string): Promise<string> => {
    await delay(900)
    return pickResponse(message)
  },

  recommend: async (_query: string, _topK = 5, _filterStatus = 'APPROVED'): Promise<string> => {
    await delay(1200)
    return CHAT_RESPONSES.vendor
  },

  similar: async (_query: string, topK = 5): Promise<AiSimilarVendor[]> => {
    await delay(600)
    return SIMILAR_VENDORS.slice(0, topK)
  },

  embedAll: async (): Promise<string> => {
    await delay(1500)
    return 'Mock: 20 vendor embeddings refreshed successfully.'
  },
}
