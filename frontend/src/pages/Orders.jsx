import { useCallback, useEffect, useState } from 'react'
import { rupees } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Empty, ErrorBox, Loading, StatusBadge, btnGhost } from '../ui.jsx'

export default function Orders() {
  const { request } = useAuth()
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setOrders(await request('/orders/mine'))
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }, [request])

  // Refresh every 10 seconds so status changes made by the restaurant show up.
  useEffect(() => {
    load()
    const t = setInterval(load, 10000)
    return () => clearInterval(t)
  }, [load])

  async function cancel(id) {
    try {
      await request(`/orders/${id}/cancel`, { method: 'POST' })
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  if (error && !orders) return <ErrorBox message={error} onRetry={load} />
  if (!orders) return <Loading />

  return (
    <>
      <h1 className="mb-4 text-2xl font-bold">My orders</h1>
      {error && <ErrorBox message={error} />}
      {orders.length === 0 && <Empty>You have not placed any orders yet.</Empty>}
      <ul className="space-y-3">
        {orders.map((o) => (
          <li key={o.id} className="rounded-xl border border-stone-200 bg-white p-4">
            <div className="mb-2 flex items-center gap-3">
              <h2 className="mr-auto font-semibold">{o.restaurantName}</h2>
              <StatusBadge status={o.status} />
            </div>
            {o.items.map((i) => (
              <p key={i.menuItemId} className="text-sm text-stone-600">{i.quantity} × {i.name}</p>
            ))}
            <div className="mt-2 flex items-center justify-between">
              <span className="text-sm text-stone-500">
                {new Date(o.createdAt).toLocaleString()} · <b>{rupees(o.totalPaise)}</b>
              </span>
              {o.status === 'PLACED' && <button className={btnGhost} onClick={() => cancel(o.id)}>Cancel order</button>}
            </div>
          </li>
        ))}
      </ul>
    </>
  )
}
