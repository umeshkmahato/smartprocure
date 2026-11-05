import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth'

export default function RegisterPage() {
  const navigate = useNavigate()
  const setAuth  = useAuthStore((s) => s.setAuth)
  const [form, setForm] = useState({
    username: '', email: '', password: '',
    firstName: '', lastName: '', department: '',
  })
  const [error,   setError]   = useState('')
  const [loading, setLoading] = useState(false)

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await authApi.register(form)
      setAuth(data)
      navigate('/dashboard', { replace: true })
    } catch {
      setError('Registration failed. Username or email may already be taken.')
    } finally {
      setLoading(false)
    }
  }

  const field = (label: string, key: string, type = 'text', required = true) => (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
      <input
        type={type} required={required}
        className="w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        value={form[key as keyof typeof form]} onChange={set(key)}
      />
    </div>
  )

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-slate-900">Smart<span className="text-blue-600">Procure</span></h1>
          <p className="text-slate-500 mt-1 text-sm">Create your account</p>
        </div>
        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border p-6 space-y-4">
          {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">{error}</div>}
          <div className="grid grid-cols-2 gap-3">
            {field('First Name', 'firstName')}
            {field('Last Name', 'lastName')}
          </div>
          {field('Username', 'username')}
          {field('Email', 'email', 'email')}
          {field('Password', 'password', 'password')}
          {field('Department', 'department', 'text', false)}
          <button type="submit" disabled={loading}
            className="w-full bg-blue-600 text-white rounded-md py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors">
            {loading ? 'Creating account…' : 'Create account'}
          </button>
          <p className="text-center text-xs text-slate-500">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-600 hover:underline">Sign in</Link>
          </p>
        </form>
      </div>
    </div>
  )
}
