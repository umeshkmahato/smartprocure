import client from './client'
import type { ApiResponse, PageResponse, PurchaseRequest } from '@/types'
import { mockPurchaseRequestsApi } from '@/mocks/purchaseRequests.mock'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true'

export const purchaseRequestsApi = USE_MOCK ? mockPurchaseRequestsApi : {
  list: (params?: { status?: string; keyword?: string; page?: number; size?: number; sort?: string; dir?: string }) =>
    client
      .get<ApiResponse<PageResponse<PurchaseRequest>>>('/purchase-requests', { params })
      .then((r) => r.data.data),

  getById: (id: number) =>
    client.get<ApiResponse<PurchaseRequest>>(`/purchase-requests/${id}`).then((r) => r.data.data),

  create: (req: unknown) =>
    client.post<ApiResponse<PurchaseRequest>>('/purchase-requests', req).then((r) => r.data.data),

  update: (id: number, req: unknown) =>
    client.put<ApiResponse<PurchaseRequest>>(`/purchase-requests/${id}`, req).then((r) => r.data.data),

  submit: (id: number) =>
    client.patch<ApiResponse<PurchaseRequest>>(`/purchase-requests/${id}/submit`).then((r) => r.data.data),

  cancel: (id: number) =>
    client.patch<ApiResponse<PurchaseRequest>>(`/purchase-requests/${id}/cancel`).then((r) => r.data.data),
}
