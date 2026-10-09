import { createContext, useContext, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API } from '../api.js'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

/* Identity is checked against the API; only the bearer token is kept locally. */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let live = true
    setReady(false); setError('')
    API.me({ signal: controller.signal })
      .then(current => { if (live) setUser(current) })
      .catch(failure => { if (live) setError(failure.message) })
      .finally(() => { if (live) setReady(true) })
    return () => { live = false; controller.abort() }
  }, [attempt])

  useEffect(() => {
    const expired = () => setUser(null)
    window.addEventListener('cputhome:session-expired', expired)
    return () => window.removeEventListener('cputhome:session-expired', expired)
  }, [])

  const login = async (email, password) => {
    const current = await API.login(email, password)
    setUser(current)
    return current
  }
  const register = async details => {
    const current = await API.register(details)
    setUser(current)
    return current
  }
  const logout = async () => { setUser(null); await API.logout() }
  return <AuthContext.Provider value={{ user, ready, error, retry: () => setAttempt(value => value + 1), login, register, logout }}>{children}</AuthContext.Provider>
}

export const homeFor = user => (!user ? '/login' : user.role === 'landlord' ? '/dashboard' : user.role === 'admin' ? '/admin' : '/')

export function Protect({ role, children }) {
  const { user, ready } = useAuth()
  if (!ready) return <p className="muted">Loading session…</p>
  return user && (!role || user.role === role) ? children : <Navigate to="/login" replace />
}
