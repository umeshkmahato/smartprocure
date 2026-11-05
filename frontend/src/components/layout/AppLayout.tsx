import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import { cn } from '@/lib/utils'
import Sidebar from './Sidebar'
import Navbar from './Navbar'
import AiChatWidget from '@/components/ai/AiChatWidget'
import { Toaster } from '@/components/ui/toaster'
import { useThemeStore, initTheme } from '@/store/themeStore'

export default function AppLayout() {
  const { theme } = useThemeStore()

  useEffect(() => { initTheme() }, [])

  return (
    <div className={cn('min-h-screen', theme === 'dark' && 'dark')}>
      <div className="flex h-screen bg-slate-50 dark:bg-slate-950 overflow-hidden">
        <Sidebar />
        {/* Main content — offset by sidebar width; sidebar toggles between w-60 and w-16 */}
        <div className="flex flex-col flex-1 overflow-hidden transition-all duration-300">
          <Navbar />
          <main className="flex-1 overflow-y-auto pt-14">
            <div className="px-6 py-6 max-w-screen-2xl mx-auto animate-fade-in">
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
