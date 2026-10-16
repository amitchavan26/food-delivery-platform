import { useCallback, useEffect, useState } from 'react'
import { rupees } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Empty, ErrorBox, Loading, StatusBadge, btn, btnGhost, input } from '../ui.jsx'

// What the restaurant is allowed to do next. The server enforces the same rules.
const NEXT = {
  PLACED: ['ACCEPTED', 'REJECTED'],
  ACCEPTED: ['PREPARING'],
  PREPARING: ['READY'],
  READY: ['OUT_FOR_DELIVERY'],
  OUT_FOR_DELIVERY: ['DELIVERED'],
}

export default function Owner() {
  const { request } = useAuth()
  const [restaurants, setRestaurants] = useState(null)
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const list = await request('/owner/restaurants')
      setRestaurants(list)
      setSelected((s) => s || list[0]?.id || null)
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }, [request])

  useEffect(() => { load() }, [load])

  if (error && !restaurants) return <ErrorBox message={error} onRetry={load} />
  if (!restaurants) return <Loading />

  return (
    <>
      <h1 className="mb-4 text-2xl font-bold">Restaurant dashboard</h1>
      {restaurants.length === 0 && <Empty>You have no restaurants yet. Create your first one below.</Empty>}
      {restaurants.length > 0 && (
        <select className={input + ' mb-4 max-w-xs'} value={selected || ''} onChange={(e) => setSelected(e.target.value)}>
          {restaurants.map((r) => <option key={r.id} value={r.id}>{r.name} ({r.city})</option>)}
        </select>
      )}
      {selected && (
        <>
          <OrdersPanel restaurantId={selected} />
          <MenuPanel restaurantId={selected} />
        </>
      )}
      <NewRestaurant onCreated={(r) => { setSelected(r.id); load() }} />
    </>
  )
}

function OrdersPanel({ restaurantId }) {
  const { request } = useAuth()
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setOrders(await request(`/restaurants/${restaurantId}/orders`))
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }, [request, restaurantId])

  useEffect(() => {
    setOrders(null)
    load()
    const t = setInterval(load, 10000)
    return () => clearInterval(t)
  }, [load])

  async function move(id, status) {
    try {
      await request(`/orders/${id}/status`, { method: 'PATCH', body: { status } })
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <section className="mb-8">
      <h2 className="mb-2 text-lg font-semibold">Incoming orders</h2>
      {error && <ErrorBox message={error} onRetry={load} />}
      {!orders && !error && <Loading />}
      {orders && orders.length === 0 && <Empty>No orders yet.</Empty>}
      <ul className="space-y-3">
        {orders?.map((o) => (
          <li key={o.id} className="rounded-xl border border-stone-200 bg-white p-4">
            <div className="mb-1 flex items-center gap-3">
              <span className="mr-auto text-sm text-stone-500">{new Date(o.createdAt).toLocaleString()} · {rupees(o.totalPaise)}</span>
              <StatusBadge status={o.status} />
            </div>
            {o.items.map((i) => <p key={i.menuItemId} className="text-sm">{i.quantity} × {i.name}</p>)}
            <div className="mt-2 flex gap-2">
              {(NEXT[o.status] || []).map((s) => (
                <button key={s} className={s === 'REJECTED' ? btnGhost : btn} onClick={() => move(o.id, s)}>
                  {s === 'REJECTED' ? 'Reject' : 'Mark ' + s.replaceAll('_', ' ').toLowerCase()}
                </button>
              ))}
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

function MenuPanel({ restaurantId }) {
  const { request } = useAuth()
  const [menu, setMenu] = useState(null)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ name: '', description: '', price: '' })
  const [pending, setPending] = useState(false)

  const load = useCallback(async () => {
    try {
      const res = await fetch(`/api/restaurants/${restaurantId}/menu`)
      if (!res.ok) throw new Error('Could not load the menu')
      setMenu(await res.json())
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }, [restaurantId])

  useEffect(() => { setMenu(null); load() }, [load])

  async function add(e) {
    e.preventDefault()
    setPending(true)
    try {
      await request(`/restaurants/${restaurantId}/menu`, {
        method: 'POST',
        body: { name: form.name, description: form.description, pricePaise: Math.round(parseFloat(form.price) * 100) },
      })
      setForm({ name: '', description: '', price: '' })
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setPending(false)
    }
  }

  async function toggle(m) {
    try {
      await request(`/restaurants/${restaurantId}/menu/${m.id}`, {
        method: 'PUT',
        body: { name: m.name, description: m.description, pricePaise: m.pricePaise, available: !m.available },
      })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="mb-8">
      <h2 className="mb-2 text-lg font-semibold">Menu</h2>
      {error && <ErrorBox message={error} onRetry={load} />}
      {!menu && !error && <Loading />}
      {menu && menu.length === 0 && <Empty>No dishes yet. Add your first one below.</Empty>}
      <ul className="mb-3 space-y-2">
        {menu?.map((m) => (
          <li key={m.id} className="flex items-center gap-3 rounded-xl border border-stone-200 bg-white p-3">
            <span className="mr-auto">{m.name} · <b>{rupees(m.pricePaise)}</b></span>
            <button className={btnGhost} onClick={() => toggle(m)}>{m.available ? 'Mark unavailable' : 'Mark available'}</button>
          </li>
        ))}
      </ul>
      <form onSubmit={add} className="grid gap-2 rounded-xl border border-stone-200 bg-white p-3 sm:grid-cols-4">
        <input className={input} placeholder="Dish name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <input className={input} placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <input className={input} placeholder="Price in ₹" type="number" min="1" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
        <button className={btn} disabled={pending}>{pending ? 'Adding…' : 'Add dish'}</button>
      </form>
    </section>
  )
}

function NewRestaurant({ onCreated }) {
  const { request } = useAuth()
  const [form, setForm] = useState({ name: '', city: '', description: '' })
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setPending(true)
    setError('')
    try {
      const r = await request('/restaurants', { method: 'POST', body: form })
      setForm({ name: '', city: '', description: '' })
      onCreated(r)
    } catch (err) {
      setError(err.message)
    } finally {
      setPending(false)
    }
  }

  return (
    <section>
      <h2 className="mb-2 text-lg font-semibold">Add a restaurant</h2>
      <form onSubmit={submit} className="grid gap-2 rounded-xl border border-stone-200 bg-white p-3 sm:grid-cols-4">
        <input className={input} placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <input className={input} placeholder="City" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} required />
        <input className={input} placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <button className={btn} disabled={pending}>{pending ? 'Creating…' : 'Create'}</button>
        {error && <p className="text-sm text-red-600 sm:col-span-4">{error}</p>}
      </form>
    </section>
  )
}
