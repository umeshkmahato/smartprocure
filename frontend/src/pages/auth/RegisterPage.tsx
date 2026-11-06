import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Zap, AlertCircle, CheckCircle2 } from 'lucide-react'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', password: '', confirm: '', firstName: '', lastName: '' })
  const [error, setError]     = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirm) { setError('Passwords do not match.'); return }
    if (form.password.length < 6)       { setError('Password must be at least 6 characters.'); return }
    setLoading(true)
    try {
      await authApi.register({
        username: form.username, email: form.email, password: form.password,
        firstName: form.firstName, lastName: form.lastName,
      })
      setSuccess(true)
      setTimeout(() => navigate('/login'), 2000)
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left branding panel — mirrors LoginPage */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between bg-slate-900 p-12 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-violet-600/20 via-transparent to-blue-600/20 pointer-events-none" />
        <div className="relative flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600">
            <Zap className="h-5 w-5 text-white" />
          </div>
          <div>
            <span className="text-lg font-bold text-white tracking-tight">SmartProcure</span>
            <span className="ml-2 text-xs text-slate-400 uppercase tracking-widest">Enterprise</span>
          </div>
        </div>
        <div className="relative space-y-6">
          <div>
            <h2 className="text-3xl font-bold text-white leading-tight">
              Join your team<br />in minutes.
            </h2>
            <p className="mt-3 text-slate-400 text-base leading-relaxed max-w-sm">
              Create your account and start managing procurement workflows with AI-powered insights.
            </p>
          </div>
          <div className="space-y-3 max-w-sm">
            {[
              { icon: '✦', text: 'Automated approval workflows' },
              { icon: '✦', text: 'Real-time vendor comparison' },
              { icon: '✦', text: 'AI-powered spend analytics' },
              { icon: '✦', text: 'End-to-end audit trail' },
            ].map(f => (
              <div key={f.text} className="flex items-center gap-3 text-sm text-slate-300">
                <span className="text-blue-400 text-xs">{f.icon}</span>
                {f.text}
              </div>
            ))}
          </div>
        </div>
        <p className="relative text-xs text-slate-500">© {new Date().getFullYear()} SmartProcure. All rights reserved.</p>
      </div>

      {/* Right form panel */}
      <div className="flex flex-1 flex-col items-center justify-center px-6 py-12 bg-slate-50 dark:bg-slate-950 overflow-y-auto">
        <div className="mb-6 flex items-center gap-2 lg:hidden">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600">
            <Zap className="h-4 w-4 text-white" />
          </div>
          <span className="text-lg font-bold text-slate-900 dark:text-white">SmartProcure</span>
        </div>
      <div className="w-full max-w-sm">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">Create an account</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Fill in your details to get started</p>
        </div>
        {success ? (
          <div className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-4 dark:border-emerald-800 dark:bg-emerald-950">
            <CheckCircle2 className="h-5 w-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-emerald-800 dark:text-emerald-200">Account created!</p>
              <p className="text-sm text-emerald-700 dark:text-emerald-300 mt-0.5">Redirecting to login…</p>
            </div>
          </div>
        ) : (
          <>
            {error && (
              <div className="mb-5 flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3 dark:border-red-800 dark:bg-red-950">
                <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
                <p className="text-sm text-red-700 dark:text-red-300">{error}</p>
              </div>
            )}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="firstName" required>First name</Label>
                  <Input id="firstName" placeholder="Jane"
                    value={form.firstName} onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} required />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="lastName" required>Last name</Label>
                  <Input id="lastName" placeholder="Smith"
                    value={form.lastName} onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))} required />
                </div>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="username" required>Username</Label>
                <Input id="username" autoComplete="username" placeholder="Choose a username"
                  value={form.username} onChange={e => setForm(f => ({ ...f, username: e.target.value }))} required />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="email" required>Email address</Label>
                <Input id="email" type="email" autoComplete="email" placeholder="you@company.com"
                  value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="password" required>Password</Label>
                <Input id="password" type="password" autoComplete="new-password" placeholder="At least 6 characters"
                  value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} required />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="confirm" required>Confirm password</Label>
                <Input id="confirm" type="password" autoComplete="new-password" placeholder="Repeat your password"
                  value={form.confirm} onChange={e => setForm(f => ({ ...f, confirm: e.target.value }))} required
                  error={!!(form.confirm && form.password !== form.confirm)} />
              </div>
              <Button type="submit" className="w-full mt-2" loading={loading} disabled={loading}>
                {loading ? 'Creating account…' : 'Create account'}
              </Button>
            </form>
            <p className="mt-6 text-center text-sm text-slate-500 dark:text-slate-400">
              Already have an account?{' '}
              <Link to="/login" className="font-medium text-blue-600 hover:text-blue-700 dark:text-blue-400">Sign in</Link>
            </p>
          </>
        )}
      </div>
      </div>
    </div>
  )
}
