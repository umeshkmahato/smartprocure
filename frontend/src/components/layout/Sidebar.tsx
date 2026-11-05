import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Users, FileText, CheckSquare,
  MessageSquare, ShoppingCart, Receipt, LogOut,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { cn } from '@/lib/utils'

const nav = [
  { to: '/dashboard',         label: 'Dashboard',         icon: LayoutDashboard },
  { to: '/vendors',           label: 'Vendors',            icon: Users },
  { to: '/purchase-requests', label: 'Purchase Requests',  icon: FileText },
  { to: '/approvals',         label: 'Approvals',          icon: CheckSquare },
  { to: '/quotations',        label: 'Quotations',         icon: MessageSquare },
  { to: '/purchase-orders',   label: 'Purchase Orders',    icon: ShoppingCart },
  { to: '/invoices',          label: 'Invoices',           icon: Receipt },
]

export default function Sidebar() {
  const clear = useAuthStore((s) => s.clear)

  return (
    <aside className="w-64 bg-slate-900 text-slate-100 flex flex-col">
      {/* Logo */}
      <div className="px-6 py-5 border-b border-slate-700">
        <span className="text-lg font-bold tracking-tight text-white">
          Smart<span className="text-blue-400">Procure</span>
        </span>
      </div>

      {/* Nav links */}
      <nav className="flex-1 py-4 space-y-1 px-3 overflow-y-auto">
        {nav.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-300 hover:bg-slate-800 hover:text-white',
              )
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Logout */}
      <div className="px-3 py-4 border-t border-slate-700">
        <button
          onClick={clear}
          className="flex items-center gap-3 w-full rounded-md px-3 py-2 text-sm font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition-colors"
        >
          <LogOut size={16} />
          Sign out
        </button>
      </div>
    </aside>
  )
}
