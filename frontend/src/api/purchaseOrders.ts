import client from './client'
import type { ApiResponse, PageResponse, PurchaseOrder, CreatePoRequest } from '@/types'
import { mockPurchaseOrdersApi } from '@/mocks/purchaseOrders.mock'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true'

export const purchaseOrdersApi = USE_MOCK ? mockPurchaseOrdersApi : {
  list: (params?: { status?: string; vendorId?: number; keyword?: string; page?: number; size?: number; sort?: string; dir?: string }) =>
    client
      .get<ApiResponse<PageResponse<PurchaseOrder>>>('/purchase-orders', { params })
      .then((r) => r.data.data),

  getById: (id: number) =>
    client.get<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}`).then((r) => r.data.data),

  getByPoNumber: (poNumber: string) =>
    client.get<ApiResponse<PurchaseOrder>>(`/purchase-orders/number/${poNumber}`).then((r) => r.data.data),

  create: (req: CreatePoRequest) =>
    client.post<ApiResponse<PurchaseOrder>>('/purchase-orders', req).then((r) => r.data.data),

  update: (id: number, req: unknown) =>
    client.put<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}`, req).then((r) => r.data.data),

  issue: (id: number) =>
    client.patch<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/issue`).then((r) => r.data.data),

  acknowledge: (id: number) =>
    client.patch<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/acknowledge`).then((r) => r.data.data),

  deliver: (id: number, lines: { itemId: number; deliveredQty: number }[]) =>
    client.patch<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/deliver`, { lines }).then((r) => r.data.data),

  cancel: (id: number) =>
    client.patch<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/cancel`).then((r) => r.data.data),

  close: (id: number) =>
    client.patch<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/close`).then((r) => r.data.data),
}
