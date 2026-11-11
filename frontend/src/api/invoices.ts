import client from './client'
import type { ApiResponse, PageResponse, Invoice, CreateInvoiceRequest } from '@/types'
import { mockInvoicesApi } from '@/mocks/invoices.mock'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true'

export const invoicesApi = USE_MOCK ? mockInvoicesApi : {
  list: (params?: { status?: string; vendorId?: number; poId?: number; keyword?: string; page?: number; size?: number; sort?: string; dir?: string }) =>
    client
      .get<ApiResponse<PageResponse<Invoice>>>('/invoices', { params })
      .then((r) => r.data.data),

  getById: (id: number) =>
    client.get<ApiResponse<Invoice>>(`/invoices/${id}`).then((r) => r.data.data),

  create: (req: CreateInvoiceRequest) =>
    client.post<ApiResponse<Invoice>>('/invoices', req).then((r) => r.data.data),

  update: (id: number, req: unknown) =>
    client.put<ApiResponse<Invoice>>(`/invoices/${id}`, req).then((r) => r.data.data),

  startReview: (id: number) =>
    client.patch<ApiResponse<Invoice>>(`/invoices/${id}/review`).then((r) => r.data.data),

  verify: (id: number) =>
    client.patch<ApiResponse<Invoice>>(`/invoices/${id}/verify`).then((r) => r.data.data),

  dispute: (id: number, reason: string) =>
    client.patch<ApiResponse<Invoice>>(`/invoices/${id}/dispute`, { reason }).then((r) => r.data.data),

  markPaid: (id: number) =>
    client.patch<ApiResponse<Invoice>>(`/invoices/${id}/pay`).then((r) => r.data.data),

  cancel: (id: number) =>
    client.patch<ApiResponse<Invoice>>(`/invoices/${id}/cancel`).then((r) => r.data.data),
}
