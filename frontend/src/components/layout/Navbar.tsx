import { useLocation, Link } from 'react-router-dom'
import { Bell, Sun, Moon, ChevronRight, LogOut, User, Settings } from 'lucide-react'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { useAuthStore } from '@/store/authStore'
import { useThemeStore } from '@/store/themeStore'
import { cn } from '@/lib/utils'

const routeLabels: Record<string, string> = {
  dashboard:         'Dashboard',
  vendors:           'Vendors',
  'purchase-requests': 'Purchase Requests',
  'purchase-orders':   'Purchase Orders',
  invoices:          'Invoices',
  approvals:         'Approvals',
  quotations:        'Quotations',
  new:               'New',
  profile:           'Profile',
  settings:          'Settings',
}

function Breadcrumbs() {
  const { pathname } = useLocation()
  const segments = pathname.split('/').filter(Boolean)

  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-sm">
      <Link to="/dashboard" className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors">
        Home
      </Link>
      {segments.map((seg, i) => {
        const href = '/' + segments.slice(0, i + 1).join('/')
        const label = routeLabels[seg] ?? seg
        const isLast = i === segments.length - 1
        return (
          <span key={href} className="flex items-center gap-1">
            <ChevronRight className="h-3.5 w-3.5 text-slate-300 dark:text-slate-600" />
            {isLast ? (
              <span className="font-medium text-slate-800 dark:text-slate-200">{label}</span>
            ) : (
              <Link to={href} className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors">
                {label}
              </Link>
            )}
          </span>
        )
      })}
    </nav>
  )
}

export default function Navbar() {
  const { username, roles, clear } = useAuthStore()
  const { theme, toggle } = useThemeStore()
  const initials = username ? username.slice(0, 2).toUpperCase() : 'U'

  return (
    <header className="fixed top-0 right-0 left-0 z-20 flex h-14 items-center justify-between border-b border-slate-200 bg-white/95 backdrop-blur-sm px-6 dark:border-slate-800 dark:bg-slate-950/95">
      {/* Breadcrumbs */}
      <Breadcrumbs />

      {/* Right actions */}
      <div className="flex items-center gap-2">
        {/* Theme toggle */}
        <button
          onClick={toggle}
          aria-label="Toggle theme"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200 transition-colors"
        >
          {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </button>

        {/* Notifications */}
        <button
          aria-label="Notifications"
          className="relative flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200 transition-colors"
        >
          <Bell className="h-4 w-4" />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-blue-600 ring-2 ring-white dark:ring-slate-950" />
        </button>

        {/* User menu */}
        <DropdownMenu.Root>
          <DropdownMenu.Trigger asChild>
            <button className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors outline-none">
              <div className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">
                {initials}
              </div>
              <div className="hidden text-left sm:block">
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 leading-tight">{username}</p>
                <p className="text-xs text-slate-400 leading-tight">
                  {roles.map((r) => r.replace('ROLE_', '')).join(', ')}
                </p>
              </div>
            </button>
          </DropdownMenu.Trigger>

          <DropdownMenu.Portal>
            <DropdownMenu.Content
              align="end"
              sideOffset={8}
              className={cn(
                'z-50 min-w-[180px] rounded-xl border border-slate-200 bg-white p-1 shadow-lg',
                'dark:border-slate-700 dark:bg-slate-900',
                'data-[state=open]:animate-fade-in',
              )}
            >
              <div className="px-3 py-2 mb-1 border-b border-slate-100 dark:border-slate-800">
                <p className="text-xs font-semibold text-slate-900 dark:text-slate-100">{username}</p>
                <p className="text-xs text-slate-400 mt-0.5">{roles.map((r) => r.replace('ROLE_', '')).join(', ')}</p>
              </div>
              <DropdownMenu.Item className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer outline-none">
                <User className="h-3.5 w-3.5" /> Profile
              </DropdownMenu.Item>
              <DropdownMenu.Item className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer outline-none">
                <Settings className="h-3.5 w-3.5" /> Settings
              </DropdownMenu.Item>
              <DropdownMenu.Separator className="my-1 h-px bg-slate-100 dark:bg-slate-800" />
              <DropdownMenu.Item
                onClick={clear}
                className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-950 cursor-pointer outline-none"
              >
                <LogOut className="h-3.5 w-3.5" /> Sign out
              </DropdownMenu.Item>
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>
      </div>
    </header>
  )
}
