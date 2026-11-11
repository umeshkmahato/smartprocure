import type { ApprovalStep, PageResponse } from '@/types'
import { delay, paginate, now, daysAgo } from './utils'

let approvals: ApprovalStep[] = [
  { id: 1,  purchaseRequestId: 4,  prNumber: 'PR-2024-004', prTitle: 'Cloud Infrastructure Upgrade',    approverId: 10, approverName: 'Admin User', status: 'PENDING',  level: 1, comments: undefined,                      actionedAt: undefined,        createdAt: daysAgo(10), updatedAt: daysAgo(10) },
  { id: 2,  purchaseRequestId: 5,  prNumber: 'PR-2024-005', prTitle: 'Staff Training Programme — Q2',   approverId: 10, approverName: 'Admin User', status: 'PENDING',  level: 1, comments: undefined,                      actionedAt: undefined,        createdAt: daysAgo(8),  updatedAt: daysAgo(8)  },
  { id: 3,  purchaseRequestId: 6,  prNumber: 'PR-2024-006', prTitle: 'Medical Supplies Restock',         approverId: 10, approverName: 'Admin User', status: 'PENDING',  level: 1, comments: undefined,                      actionedAt: undefined,        createdAt: daysAgo(6),  updatedAt: daysAgo(6)  },
  { id: 4,  purchaseRequestId: 7,  prNumber: 'PR-2024-007', prTitle: 'Server Room Cooling Units',        approverId: 10, approverName: 'Admin User', status: 'PENDING',  level: 2, comments: undefined,                      actionedAt: undefined,        createdAt: daysAgo(14), updatedAt: daysAgo(14) },
  { id: 5,  purchaseRequestId: 8,  prNumber: 'PR-2024-008', prTitle: 'Catering Contract — Annual',       approverId: 10, approverName: 'Admin User', status: 'PENDING',  level: 2, comments: undefined,                      actionedAt: undefined,        createdAt: daysAgo(12), updatedAt: daysAgo(12) },
  { id: 6,  purchaseRequestId: 9,  prNumber: 'PR-2024-009', prTitle: 'Security Software Licences',       approverId: 10, approverName: 'Admin User', status: 'APPROVED', level: 1, comments: 'Approved — critical security need.', actionedAt: daysAgo(5),  createdAt: daysAgo(20), updatedAt: daysAgo(5)  },
  { id: 7,  purchaseRequestId: 10, prNumber: 'PR-2024-010', prTitle: 'Vehicle Fleet Maintenance',         approverId: 10, approverName: 'Admin User', status: 'APPROVED', level: 1, comments: 'Routine maintenance, approved.',    actionedAt: daysAgo(7),  createdAt: daysAgo(25), updatedAt: daysAgo(7)  },
  { id: 8,  purchaseRequestId: 11, prNumber: 'PR-2024-011', prTitle: 'HR Software Upgrade',              approverId: 10, approverName: 'Admin User', status: 'APPROVED', level: 1, comments: 'Aligns with digital transformation.', actionedAt: daysAgo(8), createdAt: daysAgo(30), updatedAt: daysAgo(8)  },
  { id: 9,  purchaseRequestId: 12, prNumber: 'PR-2024-012', prTitle: 'Cleaning Services Contract',       approverId: 10, approverName: 'Admin User', status: 'APPROVED', level: 1, comments: 'Existing supplier, good rates.',    actionedAt: daysAgo(4),  createdAt: daysAgo(18), updatedAt: daysAgo(4)  },
  { id: 10, purchaseRequestId: 13, prNumber: 'PR-2024-013', prTitle: 'Legacy Printer Replacement',       approverId: 10, approverName: 'Admin User', status: 'REJECTED', level: 1, comments: 'Budget constraint — defer to Q3.',  actionedAt: daysAgo(15), createdAt: daysAgo(45), updatedAt: daysAgo(15) },
  { id: 11, purchaseRequestId: 14, prNumber: 'PR-2024-014', prTitle: 'Team Building Events — Annual',    approverId: 10, approverName: 'Admin User', status: 'REJECTED', level: 1, comments: 'Over budget — resubmit with revised scope.', actionedAt: daysAgo(20), createdAt: daysAgo(60), updatedAt: daysAgo(20) },
  { id: 12, purchaseRequestId: 4,  prNumber: 'PR-2024-004', prTitle: 'Cloud Infrastructure Upgrade',    approverId: 11, approverName: 'Finance Director', status: 'DELEGATED', level: 2, comments: 'Delegated to Finance Director for final sign-off.', actionedAt: daysAgo(9), createdAt: daysAgo(10), updatedAt: daysAgo(9) },
]

type ListParams = { status?: string; page?: number; size?: number; sort?: string; dir?: string }

export const mockApprovalsApi = {
  list: async (params: ListParams = {}): Promise<PageResponse<ApprovalStep>> => {
    await delay()
    const { status, page, size, sort, dir } = params
    return paginate(approvals, {
      page, size, sort, dir,
      filter: (a) => !status || a.status === status,
    })
  },

  getById: async (id: number): Promise<ApprovalStep> => {
    await delay()
    const a = approvals.find((a) => a.id === id)
    if (!a) throw new Error(`Approval ${id} not found`)
    return { ...a }
  },

  approve: async (id: number, comments?: string): Promise<ApprovalStep> => {
    await delay()
    approvals = approvals.map((a) =>
      a.id === id ? { ...a, status: 'APPROVED', comments: comments ?? 'Approved.', actionedAt: now(), updatedAt: now() } : a,
    )
    return approvals.find((a) => a.id === id)!
  },

  reject: async (id: number, comments: string): Promise<ApprovalStep> => {
    await delay()
    approvals = approvals.map((a) =>
      a.id === id ? { ...a, status: 'REJECTED', comments, actionedAt: now(), updatedAt: now() } : a,
    )
    return approvals.find((a) => a.id === id)!
  },

  myPending: async (): Promise<PageResponse<ApprovalStep>> => {
    await delay()
    const pending = approvals.filter((a) => a.status === 'PENDING')
    return paginate(pending)
  },
}
