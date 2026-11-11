import client from './client'
import type { ApiResponse, PageResponse, Rfq, Quotation } from '@/types'
import { mockQuotationsApi } from '@/mocks/quotations.mock'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true'

export const quotationsApi = USE_MOCK ? mockQuotationsApi : {
  listRfqs: (params?: { status?: string; search?: string; page?: number; size?: number; sort?: string; dir?: string }) =>
    client.get<ApiResponse<PageResponse<Rfq>>>('/quotations/rfqs', { params }).then((r) => r.data.data),

  getRfqById: (id: number) =>
    client.get<ApiResponse<Rfq>>(`/quotations/rfqs/${id}`).then((r) => r.data.data),

  createRfq: (req: unknown) =>
    client.post<ApiResponse<Rfq>>('/quotations/rfqs', req).then((r) => r.data.data),

  publishRfq: (id: number) =>
    client.patch<ApiResponse<Rfq>>(`/quotations/rfqs/${id}/publish`).then((r) => r.data.data),

  closeRfq: (id: number) =>
    client.patch<ApiResponse<Rfq>>(`/quotations/rfqs/${id}/close`).then((r) => r.data.data),

  listQuotations: (rfqId: number) =>
    client.get<ApiResponse<Quotation[]>>(`/quotations/rfqs/${rfqId}/quotations`).then((r) => r.data.data),

  submitQuotation: (rfqId: number, req: unknown) =>
    client.post<ApiResponse<Quotation>>(`/quotations/rfqs/${rfqId}/quotations`, req).then((r) => r.data.data),

  compareQuotations: (rfqId: number) =>
    client.get<ApiResponse<Quotation[]>>(`/quotations/rfqs/${rfqId}/compare`).then((r) => r.data.data),

  selectQuotation: (rfqId: number, quotationId: number) =>
    client.post<ApiResponse<Quotation>>(`/quotations/rfqs/${rfqId}/select/${quotationId}`).then((r) => r.data.data),
}
