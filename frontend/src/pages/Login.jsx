import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { btn, input } from '../ui.jsx'

export default function Login() {
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'CUSTOMER' })
  const [error, setError] = useState('')
  const [fields, setFields] = useState({})
  const [pending, setPending] = useState(false)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setPending(true)
    setError('')
    setFields({})
    try {
      if (mode === 'login') await login(form.email, form.password)
      else await register(form)
      navigate('/')
    } catch (err) {
      setError(err.message)
      setFields(err.fields || {})
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-4 text-2xl font-bold">{mode === 'login' ? 'Log in' : 'Create account'}</h1>
      <form onSubmit={submit} className="space-y-3 rounded-xl border border-stone-200 bg-white p-5">
        {mode === 'register' && (
          <label className="block text-sm">Name
            <input className={input} value={form.name} onChange={set('name')} required />
            {fields.name && <span className="text-xs text-red-600">{fields.name}</span>}
          </label>
        )}
        <label className="block text-sm">Email
          <input type="email" className={input} value={form.email} onChange={set('email')} required />
          {fields.email && <span className="text-xs text-red-600">{fields.email}</span>}
        </label>
        <label className="block text-sm">Password
          <input type="password" className={input} value={form.password} onChange={set('password')} required />
          {fields.password && <span className="text-xs text-red-600">{fields.password}</span>}
        </label>
        {mode === 'register' && (
          <label className="block text-sm">I am a
            <select className={input} value={form.role} onChange={set('role')}>
              <option value="CUSTOMER">Customer</option>
              <option value="RESTAURANT">Restaurant owner</option>
            </select>
          </label>
        )}
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button className={btn + ' w-full'} disabled={pending}>
          {pending ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Sign up'}
        </button>
      </form>
      <p className="mt-3 text-center text-sm text-stone-500">
        {mode === 'login' ? 'New here? ' : 'Already have an account? '}
        <button className="text-orange-700 underline" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); setFields({}) }}>
          {mode === 'login' ? 'Create an account' : 'Log in'}
        </button>
      </p>
    </div>
  )
}
