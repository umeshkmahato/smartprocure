import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { useAuthStore } from '@/store/authStore'
import {
  LayoutDashboard, Building2, ShoppingCart, FileText,
  CheckSquare, FileSearch, ChevronDown, ChevronRight,
  PanelLeftClose, PanelLeftOpen, Zap, Package,
  CreditCard, LogOut, User, Settings, Receipt,
} from 'lucide-react'

interface NavItem {
  label: string
  href?: string
  icon: React.ElementType
  children?: NavItem[]
}

const navGroups: { group: string; items: NavItem[] }[] = [
  {
    group: 'Workspace',
    items: [
      { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    ],
  },
  {
    group: 'Business Operations',
    items: [
      { label: 'Vendors', href: '/vendors', icon: Building2 },
      { label: 'Approvals', href: '/approvals', icon: CheckSquare },
      {
        label: 'Procurement',
        icon: ShoppingCart,
        children: [
          { label: 'Purchase Requests', href: '/purchase-requests', icon: FileSearch },
          { label: 'Purchase Orders',   href: '/purchase-orders',   icon: Package },
          { label: 'Quotations',        href: '/quotations',        icon: FileText },
        ],
      },
      {
        label: 'Finance',
        icon: CreditCard,
        children: [
          { label: 'Invoices', href: '/invoices', icon: Receipt },
        ],
      },
    ],
  },
  {
    group: 'Administration',
    items: [
      { label: 'Profile',  href: '/profile',  icon: User },
      { label: 'Settings', href: '/settings', icon: Settings },
    ],
  },
]

function NavItemRow({ item, collapsed, depth = 0 }: { item: NavItem; collapsed: boolean; depth?: number }) {
  const location = useLocation()
  const isChildActive = item.children?.some((c) => c.href && location.pathname.startsWith(c.href))
  const [open, setOpen] = useState(isChildActive ?? false)

  if (item.children) {
    return (
      <div>
        <button
          onClick={() => setOpen((o) => !o)}
          className={cn(
            'flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors duration-150',
            'text-slate-400 hover:text-white hover:bg-white/[0.08]',
            isChildActive && 'text-white',
            collapsed && 'justify-center px-2',
          )}
        >
          <item.icon className="h-4 w-4 shrink-0" />
          {!collapsed && (
            <>
              <span className="flex-1 text-left font-medium">{item.label}</span>
              {open
                ? <ChevronDown className="h-3.5 w-3.5 opacity-60" />
                : <ChevronRight className="h-3.5 w-3.5 opacity-60" />}
            </>
          )}
        </button>
        {!collapsed && open && (
          <div className="mt-0.5 ml-3 pl-3 border-l border-white/10 space-y-0.5">
            {item.children.map((child) => (
              <NavItemRow key={child.href} item={child} collapsed={false} depth={depth + 1} />
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <NavLink
      to={item.href!}
      title={collapsed ? item.label : undefined}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors duration-150',
          'hover:text-white hover:bg-white/[0.08]',
          collapsed && 'justify-center px-2',
          isActive
            ? 'bg-blue-600 text-white font-medium shadow-sm'
            : 'text-slate-400',
        )
      }
    >
      <item.icon className="h-4 w-4 shrink-0" />
      {!collapsed && <span className="font-medium">{item.label}</span>}
    </NavLink>
  )
}

interface SidebarProps {
  mobileOpen?: boolean
  onMobileClose?: () => void
}

export default function Sidebar({ mobileOpen = false, onMobileClose }: SidebarProps) {
  const [collapsed, setCollapsed] = useState(false)
  const clear = useAuthStore((s) => s.clear)

  return (
    <aside
      className={cn(
        'fixed left-0 top-0 z-30 flex h-screen flex-col bg-slate-900',
        'border-r border-white/[0.08] transition-all duration-300 ease-in-out',
        // Desktop: always visible, collapsible
        'hidden lg:flex',
        collapsed ? 'lg:w-16' : 'lg:w-60',
        // Mobile: slide-in drawer overlay
        mobileOpen && 'flex w-72 lg:flex',
      )}
      aria-label="Main navigation"
    >
      {/* Logo */}
      <div className={cn(
        'flex h-14 shrink-0 items-center border-b border-white/[0.08] px-4',
        collapsed && 'justify-center px-2',
      )}>
        <div className="flex items-center gap-2.5">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-blue-600">
            <Zap className="h-4 w-4 text-white" />
          </div>
          {!collapsed && (
            <div className="leading-tight">
              <span className="block text-sm font-bold text-white tracking-tight">SmartProcure</span>
              <span className="block text-[10px] text-slate-400 uppercase tracking-widest">Enterprise</span>
            </div>
          )}
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-4 overflow-x-hidden overflow-y-auto px-2 py-3">
        {navGroups.map(({ group, items }) => (
          <div key={group}>
            {!collapsed && (
              <p className="mb-1 px-3 text-[10px] font-semibold uppercase tracking-widest text-slate-500">
                {group}
              </p>
            )}
            <div className="space-y-0.5">
              {items.map((item) => (
                <NavItemRow key={item.label} item={item} collapsed={collapsed} />
              ))}
            </div>
          </div>
        ))}
      </nav>

      {/* Sign out + collapse */}
      <div className="shrink-0 space-y-0.5 border-t border-white/[0.08] p-2">
        {/* Mobile close button */}
        {onMobileClose && (
          <button
            onClick={onMobileClose}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-400 hover:text-white hover:bg-white/[0.08] transition-colors duration-150 lg:hidden"
          >
            <PanelLeftClose className="h-4 w-4 shrink-0" />
            <span className="font-medium">Close menu</span>
          </button>
        )}
        <button
          onClick={clear}
          className={cn(
            'flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-400',
            'hover:text-white hover:bg-white/[0.08] transition-colors duration-150',
            collapsed && 'justify-center px-2',
          )}
        >
          <LogOut className="h-4 w-4 shrink-0" />
          {!collapsed && <span className="font-medium">Sign out</span>}
        </button>
        {/* Desktop collapse toggle */}
        <button
          onClick={() => setCollapsed((c) => !c)}
          className={cn(
            'hidden lg:flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate-500',
            'hover:text-white hover:bg-white/[0.08] transition-colors duration-150',
            collapsed && 'justify-center px-2',
          )}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed
            ? <PanelLeftOpen className="h-4 w-4 shrink-0" />
            : <><PanelLeftClose className="h-4 w-4 shrink-0" /><span className="font-medium">Collapse</span></>}
        </button>
      </div>
    </aside>
  )
}
