import client from './client'
import type { ApiResponse, PageResponse, Vendor, CreateVendorRequest } from '@/types'

export const vendorsApi = {
  list: (params?: { status?: string; keyword?: string; page?: number; size?: number; sort?: string; dir?: string }) =>
    client.get<ApiResponse<PageResponse<Vendor>>>('/vendors', { params }).then((r) => r.data.data),

  getById: (id: number) =>
    client.get<ApiResponse<Vendor>>(`/vendors/${id}`).then((r) => r.data.data),

  create: (req: CreateVendorRequest) =>
    client.post<ApiResponse<Vendor>>('/vendors', req).then((r) => r.data.data),

  update: (id: number, req: Partial<CreateVendorRequest>) =>
    client.put<ApiResponse<Vendor>>(`/vendors/${id}`, req).then((r) => r.data.data),

  approve: (id: number) =>
    client.patch<ApiResponse<Vendor>>(`/vendors/${id}/approve`).then((r) => r.data.data),

  suspend: (id: number, reason: string) =>
    client.patch<ApiResponse<Vendor>>(`/vendors/${id}/suspend`, { reason }).then((r) => r.data.data),

  blacklist: (id: number, reason: string) =>
    client.patch<ApiResponse<Vendor>>(`/vendors/${id}/blacklist`, { reason }).then((r) => r.data.data),

  recommend: (description: string) =>
    client.post<ApiResponse<string>>('/vendors/recommend', { description }).then((r) => r.data.data),
}
