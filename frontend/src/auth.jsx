import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { api } from './api.js'

const AuthContext = createContext(null)
const KEY = 'fd_user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem(KEY)) } catch { return null }
  })

  const save = (u) => {
    setUser(u)
    if (u) localStorage.setItem(KEY, JSON.stringify(u))
    else localStorage.removeItem(KEY)
  }

  const login = async (email, password) => save(await api('/auth/login', { method: 'POST', body: { email, password } }))
  const register = async (form) => save(await api('/auth/register', { method: 'POST', body: form }))
  const logout = useCallback(() => save(null), [])

  // Authenticated request. If the token has expired, sign the user out.
  const request = useCallback(async (path, opts = {}) => {
    try {
      return await api(path, { ...opts, token: user?.token })
    } catch (e) {
      if (e.status === 401 && user) logout()
      throw e
    }
  }, [user, logout])

  const value = useMemo(() => ({ user, login, register, logout, request }), [user, request, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
