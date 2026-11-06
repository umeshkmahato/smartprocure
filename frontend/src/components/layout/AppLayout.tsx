import { useEffect, useState, useCallback } from 'react'
import { Outlet } from 'react-router-dom'
import { cn } from '@/lib/utils'
import Sidebar from './Sidebar'
import Navbar from './Navbar'
import AiChatWidget from '@/components/ai/AiChatWidget'
import { Toaster } from '@/components/ui/toaster'
import { useThemeStore, initTheme } from '@/store/themeStore'

export default function AppLayout() {
  const { theme } = useThemeStore()
  const [mobileOpen, setMobileOpen] = useState(false)

  useEffect(() => { initTheme() }, [])

  // Close mobile drawer on route change (listen to clicks on main)
  const closeMobile = useCallback(() => setMobileOpen(false), [])

  return (
    <div className={cn('min-h-screen', theme === 'dark' && 'dark')}>
      <div className="flex h-screen bg-slate-50 dark:bg-slate-950 overflow-hidden">
        <Sidebar mobileOpen={mobileOpen} onMobileClose={closeMobile} />

        {/* Mobile backdrop */}
        {mobileOpen && (
          <div
            className="fixed inset-0 z-20 bg-black/50 lg:hidden"
            onClick={closeMobile}
            aria-hidden="true"
          />
        )}

        <div className="flex flex-col flex-1 overflow-hidden transition-all duration-300">
          <Navbar onMenuToggle={() => setMobileOpen(o => !o)} />
          <main className="flex-1 overflow-y-auto pt-14" onClick={closeMobile}>
            <div className="px-4 sm:px-6 py-6 max-w-screen-2xl mx-auto animate-fade-in">
              <Outlet />
            </div>
          </main>
        </div>

        <AiChatWidget />
        <Toaster />
      </div>
    </div>
  )
}
