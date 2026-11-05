// ── Auth ──────────────────────────────────────────────────────────────────────
export interface LoginRequest { username: string; password: string }
export interface RegisterRequest {
  username: string; email: string; password: string
  firstName: string; lastName: string; department?: string; role?: string
}
export interface AuthResponse {
  accessToken: string; refreshToken: string
  username: string; email: string; roles: string[]
}

// ── Pagination ────────────────────────────────────────────────────────────────
export interface PageResponse<T> {
  content: T[]; totalElements: number; totalPages: number
  number: number; size: number; first: boolean; last: boolean
}
export interface ApiResponse<T> { success: boolean; message: string; data: T }

/** Shared sort state used by every list page */
export interface SortState { field: string; dir: 'asc' | 'desc' }

/** Common list-query params accepted by every paginated backend endpoint */
export interface ListParams {
  page?: number
  size?: number
  sort?: string
  dir?: string
}

// ── Vendor ────────────────────────────────────────────────────────────────────
export type VendorStatus = 'PENDING' | 'APPROVED' | 'SUSPENDED' | 'BLACKLISTED'
export interface Vendor {
  id: number; name: string; email: string; phone?: string
  address?: string; city?: string; country?: string
  taxId?: string; registrationNumber?: string
  category?: string; currency?: string; status: VendorStatus
  rating?: number; createdAt: string; updatedAt: string
}
export interface CreateVendorRequest {
  name: string; email: string; phone?: string; address?: string
  city?: string; country?: string; taxId?: string
  registrationNumber?: string; category?: string; currency?: string; notes?: string
}

// ── Purchase Request ──────────────────────────────────────────────────────────
export type PrStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export interface PrLineItem {
  id: number; itemName: string; description?: string
  quantity: number; unit?: string; estimatedUnitPrice: number
  estimatedTotal: number; notes?: string
}
export interface PurchaseRequest {
  id: number; prNumber: string; title: string; description?: string
  status: PrStatus; department?: string; justification?: string
  requiredDate?: string; estimatedTotal: number; currency: string
  requestedById: number; requestedByName: string
  items: PrLineItem[]; createdAt: string; updatedAt: string
}

// ── Approval ──────────────────────────────────────────────────────────────────
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DELEGATED'
export interface ApprovalStep {
  id: number; purchaseRequestId: number; prNumber: string; prTitle: string
  approverId: number; approverName: string
  status: ApprovalStatus; level: number
  comments?: string; actionedAt?: string
  createdAt: string; updatedAt: string
}

// ── Purchase Order ────────────────────────────────────────────────────────────
export type PoStatus =
  | 'DRAFT' | 'ISSUED' | 'ACKNOWLEDGED'
  | 'PARTIALLY_DELIVERED' | 'DELIVERED'
  | 'INVOICED' | 'CLOSED' | 'CANCELLED'

export interface PoLineItem {
  id: number; itemName: string; description?: string
  quantity: number; unit?: string; unitPrice: number
  totalPrice: number; deliveredQty: number; notes?: string
}
export interface PurchaseOrder {
  id: number; poNumber: string; vendorId: number; vendorName: string
  purchaseRequestId?: number; quotationId?: number
  issuedById?: number; issuedByName?: string
  status: PoStatus; currency: string; totalAmount: number
  deliveryDate?: string; deliveryAddress?: string
  paymentTerms?: string; notes?: string
  issuedAt?: string; acknowledgedAt?: string; deliveredAt?: string
  items: PoLineItem[]; createdAt: string; updatedAt: string
}
export interface CreatePoRequest {
  vendorId: number; purchaseRequestId?: number; quotationId?: number
  currency?: string; deliveryDate?: string; deliveryAddress?: string
  paymentTerms?: string; notes?: string
  items: { itemName: string; description?: string; quantity: number; unit?: string; unitPrice: number; notes?: string }[]
}

// ── Quotation ─────────────────────────────────────────────────────────────────
export type RfqStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'AWARDED'
export type QuotationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'SELECTED' | 'REJECTED'
export interface Rfq {
  id: number; rfqNumber: string; title: string; description?: string
  status: RfqStatus; deadline?: string; purchaseRequestId?: number
  createdAt: string; updatedAt: string
}
export interface Quotation {
  id: number; rfqId: number; rfqNumber: string
  vendorId: number; vendorName: string
  status: QuotationStatus; totalAmount: number; currency: string
  validUntil?: string; notes?: string
  createdAt: string; updatedAt: string
}

// ── Invoice ───────────────────────────────────────────────────────────────────
export type InvoiceStatus =
  | 'SUBMITTED' | 'UNDER_REVIEW' | 'VERIFIED' | 'DISPUTED' | 'PAID' | 'CANCELLED'
export interface Invoice {
  id: number; purchaseOrderId: number; poNumber: string
  vendorId: number; vendorName: string
  invoiceNumber: string; status: InvoiceStatus
  totalAmount: number; currency: string
  invoiceDate: string; dueDate?: string
  documentUrl?: string; notes?: string; disputeReason?: string
  verifiedAt?: string; paidAt?: string
  createdAt: string; updatedAt: string
}
export interface CreateInvoiceRequest {
  purchaseOrderId: number; invoiceNumber: string
  totalAmount: number; currency?: string
  invoiceDate: string; dueDate?: string
  documentUrl?: string; notes?: string
}
