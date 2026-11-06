import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import AppLayout from '@/components/layout/AppLayout'
import LoginPage from '@/pages/auth/LoginPage'
import RegisterPage from '@/pages/auth/RegisterPage'
import DashboardPage from '@/pages/dashboard/DashboardPage'
import VendorsPage from '@/pages/vendors/VendorsPage'
import VendorDetailPage from '@/pages/vendors/VendorDetailPage'
import PurchaseRequestsPage from '@/pages/purchaseRequests/PurchaseRequestsPage'
import CreatePurchaseRequestPage from '@/pages/purchaseRequests/CreatePurchaseRequestPage'
import PurchaseRequestDetailPage from '@/pages/purchaseRequests/PurchaseRequestDetailPage'
import ApprovalsPage from '@/pages/approvals/ApprovalsPage'
import QuotationsPage from '@/pages/quotations/QuotationsPage'
import CreateRFQPage from '@/pages/quotations/CreateRFQPage'
import QuotationDetailPage from '@/pages/quotations/QuotationDetailPage'
import PurchaseOrdersPage from '@/pages/purchaseOrders/PurchaseOrdersPage'
import PurchaseOrderDetailPage from '@/pages/purchaseOrders/PurchaseOrderDetailPage'
import CreatePurchaseOrderPage from '@/pages/purchaseOrders/CreatePurchaseOrderPage'
import InvoicesPage from '@/pages/invoices/InvoicesPage'
import InvoiceDetailPage from '@/pages/invoices/InvoiceDetailPage'
import CreateInvoicePage from '@/pages/invoices/CreateInvoicePage'
import ProfilePage from '@/pages/profile/ProfilePage'
import SettingsPage from '@/pages/settings/SettingsPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.accessToken)
  return token ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Protected */}
      <Route element={<RequireAuth><AppLayout /></RequireAuth>}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />

        {/* Vendors */}
        <Route path="/vendors"          element={<VendorsPage />} />
        <Route path="/vendors/:id"      element={<VendorDetailPage />} />

        {/* Purchase Requests */}
        <Route path="/purchase-requests"          element={<PurchaseRequestsPage />} />
        <Route path="/purchase-requests/new"      element={<CreatePurchaseRequestPage />} />
        <Route path="/purchase-requests/:id"      element={<PurchaseRequestDetailPage />} />

        {/* Approvals */}
        <Route path="/approvals" element={<ApprovalsPage />} />

        {/* Quotations */}
        <Route path="/quotations"      element={<QuotationsPage />} />
        <Route path="/quotations/new"  element={<CreateRFQPage />} />
        <Route path="/quotations/:id"  element={<QuotationDetailPage />} />

        {/* Purchase Orders */}
        <Route path="/purchase-orders"       element={<PurchaseOrdersPage />} />
        <Route path="/purchase-orders/new"   element={<CreatePurchaseOrderPage />} />
        <Route path="/purchase-orders/:id"   element={<PurchaseOrderDetailPage />} />

        {/* Invoices */}
        <Route path="/invoices"       element={<InvoicesPage />} />
        <Route path="/invoices/new"   element={<CreateInvoicePage />} />
        <Route path="/invoices/:id"   element={<InvoiceDetailPage />} />

        {/* Profile & Settings */}
        <Route path="/profile"   element={<ProfilePage />} />
        <Route path="/settings"  element={<SettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
