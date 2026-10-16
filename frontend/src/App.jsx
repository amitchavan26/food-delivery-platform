import { Link, NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Login from './pages/Login.jsx'
import Restaurants from './pages/Restaurants.jsx'
import RestaurantMenu from './pages/RestaurantMenu.jsx'
import Orders from './pages/Orders.jsx'
import Owner from './pages/Owner.jsx'

function RequireRole({ role, children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) return <Navigate to="/" replace />
  return children
}

export default function App() {
  const { user, logout } = useAuth()
  const link = ({ isActive }) => (isActive ? 'font-semibold text-orange-700' : 'text-stone-600 hover:text-stone-900')

  return (
    <div className="min-h-screen">
      <header className="border-b border-stone-200 bg-white">
        <nav className="mx-auto flex max-w-4xl items-center gap-5 px-4 py-3">
          <Link to="/" className="mr-auto text-lg font-bold text-orange-600">🍲 Food Delivery</Link>
          <NavLink to="/" end className={link}>Restaurants</NavLink>
          {user?.role === 'CUSTOMER' && <NavLink to="/orders" className={link}>My orders</NavLink>}
          {user?.role === 'RESTAURANT' && <NavLink to="/owner" className={link}>Dashboard</NavLink>}
          {user ? (
            <button onClick={logout} className="text-sm text-stone-500 hover:text-stone-900">
              Log out ({user.name})
            </button>
          ) : (
            <NavLink to="/login" className={link}>Log in</NavLink>
          )}
        </nav>
      </header>

      <main className="mx-auto max-w-4xl px-4 py-6">
        <Routes>
          <Route path="/" element={<Restaurants />} />
          <Route path="/restaurants/:id" element={<RestaurantMenu />} />
          <Route path="/login" element={<Login />} />
          <Route path="/orders" element={<RequireRole role="CUSTOMER"><Orders /></RequireRole>} />
          <Route path="/owner" element={<RequireRole role="RESTAURANT"><Owner /></RequireRole>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}
