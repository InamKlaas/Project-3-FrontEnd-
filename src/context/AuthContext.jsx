import { createContext, useContext, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { API } from '../api.js'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(API.me())
  const login = (email, password) => setUser(API.login(email, password))
  const register = details => setUser(API.register(details))
  const logout = () => { API.logout(); setUser(null) }
  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>
}

export const homeFor = user => (!user ? '/login' : user.role === 'landlord' ? '/dashboard' : user.role === 'admin' ? '/admin' : '/')

export function Protect({ role, children }) {
  const { user } = useAuth()
  return user && (!role || user.role === role) ? children : <Navigate to="/login" replace />
}