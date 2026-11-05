import { useAuthStore } from '@/store/authStore'
import { Bell, User } from 'lucide-react'

export default function Navbar() {
  const { username, roles } = useAuthStore()

  return (
    <header className="h-14 border-b bg-white flex items-center justify-between px-6 shrink-0">
      <div />
      <div className="flex items-center gap-4">
        <button className="relative text-slate-500 hover:text-slate-800">
          <Bell size={18} />
        </button>
        <div className="flex items-center gap-2 text-sm">
          <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white">
            <User size={14} />
          </div>
          <div className="hidden sm:block">
            <p className="font-medium text-slate-800 leading-tight">{username}</p>
            <p className="text-xs text-slate-400 leading-tight">
              {roles.map((r) => r.replace('ROLE_', '')).join(', ')}
            </p>
          </div>
        </div>
      </div>
    </header>
  )
}
