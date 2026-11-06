import { useThemeStore } from '@/store/themeStore'
import { Sun, Moon, Monitor, Bell, Globe, Shield, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="text-xs font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-1">
      {children}
    </h2>
  )
}

function SettingRow({
  icon: Icon,
  label,
  description,
  children,
}: {
  icon: React.ElementType
  label: string
  description?: string
  children?: React.ReactNode
}) {
  return (
    <div className="flex items-center justify-between gap-4 py-4 border-b border-slate-100 dark:border-slate-800 last:border-0">
      <div className="flex items-center gap-3 min-w-0">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
          <Icon className="h-4 w-4 text-slate-500 dark:text-slate-400" />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-800 dark:text-slate-200">{label}</p>
          {description && (
            <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">{description}</p>
          )}
        </div>
      </div>
      <div className="shrink-0">{children}</div>
    </div>
  )
}

function ThemeButton({
  value,
  current,
  label,
  icon: Icon,
  onClick,
}: {
  value: string
  current: string
  label: string
  icon: React.ElementType
  onClick: () => void
}) {
  const active = value === current
  return (
    <button
      onClick={onClick}
      className={cn(
        'flex flex-col items-center gap-1.5 rounded-xl border px-5 py-3 text-xs font-medium transition-all',
        active
          ? 'border-blue-500 bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 shadow-sm'
          : 'border-slate-200 dark:border-slate-700 text-slate-500 dark:text-slate-400 hover:border-slate-300 dark:hover:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800',
      )}
    >
      <Icon className="h-4 w-4" />
      {label}
    </button>
  )
}

function Toggle({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <button
      role="switch"
      aria-checked={checked}
      onClick={onChange}
      className={cn(
        'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2',
        checked ? 'bg-blue-600' : 'bg-slate-200 dark:bg-slate-700',
      )}
    >
      <span
        className={cn(
          'pointer-events-none inline-block h-4 w-4 rounded-full bg-white shadow-sm transition-transform duration-200',
          checked ? 'translate-x-4' : 'translate-x-0',
        )}
      />
    </button>
  )
}

export default function SettingsPage() {
  const { theme, toggle } = useThemeStore()

  return (
    <div className="max-w-2xl space-y-6">
      {/* Heading */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">Settings</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Manage your app preferences</p>
      </div>

      {/* Appearance */}
      <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
        <SectionTitle>Appearance</SectionTitle>

        <div className="pt-3 pb-4 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-3 mb-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800">
              {theme === 'dark' ? (
                <Moon className="h-4 w-4 text-slate-500 dark:text-slate-400" />
              ) : (
                <Sun className="h-4 w-4 text-slate-500 dark:text-slate-400" />
              )}
            </div>
            <div>
              <p className="text-sm font-medium text-slate-800 dark:text-slate-200">Theme</p>
              <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">Choose your preferred colour scheme</p>
            </div>
          </div>
          <div className="flex gap-3 mt-3">
            <ThemeButton
              value="light"
              current={theme}
              label="Light"
              icon={Sun}
              onClick={() => { if (theme !== 'light') toggle() }}
            />
            <ThemeButton
              value="dark"
              current={theme}
              label="Dark"
              icon={Moon}
              onClick={() => { if (theme !== 'dark') toggle() }}
            />
            <ThemeButton
              value="system"
              current={theme === 'light' || theme === 'dark' ? theme : 'system'}
              label="System"
              icon={Monitor}
              onClick={() => {}}
            />
          </div>
        </div>

        <SettingRow
          icon={Globe}
          label="Language"
          description="Interface display language"
        >
          <span className="inline-flex items-center gap-1 text-sm text-slate-500 dark:text-slate-400">
            English <ChevronRight className="h-3.5 w-3.5" />
          </span>
        </SettingRow>
      </div>

      {/* Notifications */}
      <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
        <SectionTitle>Notifications</SectionTitle>

        <SettingRow
          icon={Bell}
          label="Approval alerts"
          description="Get notified when a purchase request needs your approval"
        >
          <Toggle checked={true} onChange={() => {}} />
        </SettingRow>

        <SettingRow
          icon={Bell}
          label="Invoice updates"
          description="Receive updates when invoice status changes"
        >
          <Toggle checked={true} onChange={() => {}} />
        </SettingRow>

        <SettingRow
          icon={Bell}
          label="Vendor status changes"
          description="Notify when a vendor is approved or suspended"
        >
          <Toggle checked={false} onChange={() => {}} />
        </SettingRow>
      </div>

      {/* Security */}
      <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5">
        <SectionTitle>Security</SectionTitle>

        <SettingRow
          icon={Shield}
          label="Two-factor authentication"
          description="Add an extra layer of security to your account"
        >
          <span className="inline-flex items-center rounded-full bg-slate-100 dark:bg-slate-800 px-2.5 py-0.5 text-xs font-medium text-slate-500 dark:text-slate-400">
            Coming soon
          </span>
        </SettingRow>
      </div>

      <p className="text-xs text-slate-400 dark:text-slate-500 text-center">
        Some settings require a page refresh to take effect.
      </p>
    </div>
  )
}
