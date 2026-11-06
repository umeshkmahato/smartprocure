import { useAuthStore } from '@/store/authStore'
import { User, Mail, Shield, CalendarDays, KeyRound } from 'lucide-react'

function InfoCard({ icon: Icon, label, value }: {
  icon: React.ElementType; label: string; value?: string | null
}) {
  return (
    <div className="flex items-center gap-4 py-4 border-b border-slate-100 dark:border-slate-800 last:border-0">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-slate-100 dark:bg-slate-800">
        <Icon className="h-4 w-4 text-slate-500 dark:text-slate-400" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-slate-400 dark:text-slate-500">{label}</p>
        <p className="text-sm font-medium text-slate-800 dark:text-slate-200 truncate mt-0.5">
          {value ?? '—'}
        </p>
      </div>
    </div>
  )
}

export default function ProfilePage() {
  const { username, email, roles } = useAuthStore()

  const displayRoles = roles
    .map((r) => r.replace('ROLE_', ''))
    .join(', ')

  const initials = username ? username.slice(0, 2).toUpperCase() : 'U'

  return (
    <div className="max-w-2xl space-y-6">
      {/* Page heading */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Profile</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Your account information</p>
      </div>

      {/* Avatar + name hero */}
      <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 flex flex-col sm:flex-row items-center sm:items-start gap-5">
        <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl bg-blue-600 text-white text-2xl font-bold shadow-md">
          {initials}
        </div>
        <div className="text-center sm:text-left">
          <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">{username}</h2>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">{email}</p>
          <div className="mt-3 flex flex-wrap justify-center sm:justify-start gap-2">
            {roles.map((r) => (
              <span
                key={r}
                className="inline-flex items-center gap-1 rounded-full bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 px-2.5 py-0.5 text-xs font-medium text-blue-700 dark:text-blue-300"
              >
                <Shield className="h-3 w-3" />
                {r.replace('ROLE_', '')}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Account details */}
      <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
        <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-1">
          Account Details
        </h2>
        <InfoCard icon={User}        label="Username"    value={username} />
        <InfoCard icon={Mail}        label="Email"       value={email} />
        <InfoCard icon={Shield}      label="Roles"       value={displayRoles} />
        <InfoCard icon={KeyRound}    label="Auth"        value="JWT / Bearer Token" />
        <InfoCard icon={CalendarDays} label="Session"   value="Active" />
      </div>

      {/* Info note */}
      <p className="text-xs text-slate-400 dark:text-slate-500 text-center">
        To update your profile details, please contact your system administrator.
      </p>
    </div>
  )
}
