import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, rupees } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Empty, ErrorBox, Loading, btn, btnGhost } from '../ui.jsx'

export default function RestaurantMenu() {
  const { id } = useParams()
  const { user, request } = useAuth()
  const navigate = useNavigate()
  const [restaurant, setRestaurant] = useState(null)
  const [menu, setMenu] = useState(null)
  const [error, setError] = useState('')
  const [cart, setCart] = useState({}) // menuItemId -> quantity
  const [placing, setPlacing] = useState(false)
  const [orderError, setOrderError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      const [r, m] = await Promise.all([api(`/restaurants/${id}`), api(`/restaurants/${id}/menu`)])
      setRestaurant(r)
      setMenu(m)
    } catch (e) {
      setError(e.status === 404 ? 'This restaurant does not exist.' : e.message)
    }
  }, [id])

  useEffect(() => { load() }, [load])

  const change = (itemId, delta) =>
    setCart((c) => {
      const q = Math.max(0, Math.min(50, (c[itemId] || 0) + delta))
      const next = { ...c, [itemId]: q }
      if (q === 0) delete next[itemId]
      return next
    })

  const lines = (menu || []).filter((m) => cart[m.id])
  const total = lines.reduce((sum, m) => sum + m.pricePaise * cart[m.id], 0)

  async function placeOrder() {
    if (!user) return navigate('/login')
    setPlacing(true)
    setOrderError('')
    try {
      await request('/orders', {
        method: 'POST',
        body: { restaurantId: id, items: lines.map((m) => ({ menuItemId: m.id, quantity: cart[m.id] })) },
      })
      navigate('/orders')
    } catch (e) {
      setOrderError(e.message)
    } finally {
      setPlacing(false)
    }
  }

  if (error) return <ErrorBox message={error} onRetry={load} />
  if (!restaurant || !menu) return <Loading />

  const canOrder = !user || user.role === 'CUSTOMER'

  return (
    <>
      <h1 className="text-2xl font-bold">{restaurant.name}</h1>
      <p className="mb-5 text-stone-500">{restaurant.city}{restaurant.description && ` · ${restaurant.description}`}</p>

      {menu.length === 0 && <Empty>This restaurant has not added any dishes yet.</Empty>}
      <ul className="space-y-2">
        {menu.map((m) => (
          <li key={m.id} className="flex items-center gap-3 rounded-xl border border-stone-200 bg-white p-3">
            <div className="mr-auto">
              <p className="font-medium">{m.name}</p>
              {m.description && <p className="text-sm text-stone-500">{m.description}</p>}
              <p className="text-sm font-semibold">{rupees(m.pricePaise)}</p>
            </div>
            {!m.available ? (
              <span className="text-sm text-stone-400">Unavailable</span>
            ) : canOrder ? (
              <div className="flex items-center gap-2">
                <button className={btnGhost} onClick={() => change(m.id, -1)} aria-label={`Remove one ${m.name}`}>−</button>
                <span className="w-5 text-center">{cart[m.id] || 0}</span>
                <button className={btnGhost} onClick={() => change(m.id, 1)} aria-label={`Add one ${m.name}`}>+</button>
              </div>
            ) : null}
          </li>
        ))}
      </ul>

      {canOrder && lines.length > 0 && (
        <div className="mt-6 rounded-xl border border-orange-200 bg-orange-50 p-4">
          <h2 className="mb-2 font-semibold">Your order</h2>
          {lines.map((m) => (
            <p key={m.id} className="flex justify-between text-sm">
              <span>{cart[m.id]} × {m.name}</span><span>{rupees(m.pricePaise * cart[m.id])}</span>
            </p>
          ))}
          <p className="mt-2 flex justify-between border-t border-orange-200 pt-2 font-semibold">
            <span>Total</span><span>{rupees(total)}</span>
          </p>
          {orderError && <p className="mt-2 text-sm text-red-600">{orderError}</p>}
          <button className={btn + ' mt-3 w-full'} onClick={placeOrder} disabled={placing}>
            {placing ? 'Placing order…' : user ? 'Place order' : 'Log in to order'}
          </button>
        </div>
      )}
    </>
  )
}
