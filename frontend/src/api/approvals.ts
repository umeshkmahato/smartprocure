import client from './client'
import type { ApiResponse, PageResponse, ApprovalStep } from '@/types'
import { mockApprovalsApi } from '@/mocks/approvals.mock'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true'

export const approvalsApi = USE_MOCK ? mockApprovalsApi : {
  list: (params?: { status?: string; page?: number; size?: number; sort?: string; dir?: string }) =>
    client
      .get<ApiResponse<PageResponse<ApprovalStep>>>('/approvals', { params })
      .then((r) => r.data.data),

  getById: (id: number) =>
    client.get<ApiResponse<ApprovalStep>>(`/approvals/${id}`).then((r) => r.data.data),

  approve: (id: number, comments?: string) =>
    client.patch<ApiResponse<ApprovalStep>>(`/approvals/${id}/approve`, { comments }).then((r) => r.data.data),

  reject: (id: number, comments: string) =>
    client.patch<ApiResponse<ApprovalStep>>(`/approvals/${id}/reject`, { comments }).then((r) => r.data.data),

  myPending: () =>
    client.get<ApiResponse<PageResponse<ApprovalStep>>>('/approvals/my-pending').then((r) => r.data.data),
}
