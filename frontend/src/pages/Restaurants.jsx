import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { Empty, ErrorBox, Loading, btn, input } from '../ui.jsx'

export default function Restaurants() {
  const [city, setCity] = useState('')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async (c) => {
    setError('')
    setData(null)
    try {
      setData(await api('/restaurants' + (c ? `?city=${encodeURIComponent(c)}` : '')))
    } catch (e) {
      setError(e.message)
    }
  }, [])

  useEffect(() => { load('') }, [load])

  return (
    <>
      <h1 className="mb-4 text-2xl font-bold">Restaurants near you</h1>
      <form onSubmit={(e) => { e.preventDefault(); load(city.trim()) }} className="mb-5 flex gap-2">
        <input className={input} placeholder="Filter by city, e.g. Pune" value={city} onChange={(e) => setCity(e.target.value)} />
        <button className={btn}>Search</button>
      </form>

      {error && <ErrorBox message={error} onRetry={() => load(city.trim())} />}
      {!error && !data && <Loading />}
      {data && data.content.length === 0 && <Empty>No restaurants found{city && ` in ${city}`}.</Empty>}
      {data && (
        <ul className="grid gap-3 sm:grid-cols-2">
          {data.content.map((r) => (
            <li key={r.id}>
              <Link to={`/restaurants/${r.id}`} className="block rounded-xl border border-stone-200 bg-white p-4 hover:border-orange-400">
                <h2 className="font-semibold">{r.name}</h2>
                <p className="text-sm text-stone-500">{r.city}</p>
                {r.description && <p className="mt-1 text-sm text-stone-600">{r.description}</p>}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
