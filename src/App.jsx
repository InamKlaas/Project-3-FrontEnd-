import { Routes, Route, NavLink, Link } from 'react-router-dom'
import { useAuth, Protect } from './context/AuthContext.jsx'
import Home from './pages/Home.jsx'
import Listing from './pages/Listing.jsx'
import Auth from './pages/Auth.jsx'
import Messages, { Chat } from './pages/Messages.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Admin from './pages/Admin.jsx'
import MyStuff from './pages/MyStuff.jsx'
import Notifications from './pages/Notifications.jsx'
import Privacy from './pages/Privacy.jsx'
import ThemeToggle from './components/ThemeToggle.jsx'

export default function App() {
  const { user, ready, error, retry, logout } = useAuth()
  if (!ready) return <p className="muted">Loading session…</p>
  if (error) return <main><section className="panel"><h1>Could not restore your session</h1><p role="alert" className="err">{error}</p><button className="btn" onClick={retry}>Retry</button></section></main>
  return (
    <>
      <header className="nav">
        <Link to="/" className="logo">🏠 CPUT<span>Home</span></Link>
        <nav>
          <ThemeToggle />
          <NavLink to="/">Browse</NavLink>
          <NavLink to="/?emergency=1">🚨 Emergency</NavLink>
          {!user && <><NavLink to="/login">Login</NavLink><Link className="btn sm" to="/register">Sign up</Link></>}
          {user?.role === 'student' && <><NavLink to="/my">My housing</NavLink><NavLink to="/privacy">My data</NavLink></>}
          {user && user.role !== 'admin' && <NavLink to="/messages">Messages</NavLink>}
          {user && <NavLink to="/notifications">Notifications</NavLink>}
          {user?.role === 'landlord' && <NavLink to="/dashboard">My residences</NavLink>}
          {user?.role === 'admin' && <NavLink to="/admin">Admin</NavLink>}
          {user && <><span className="muted">{user.name} ({user.role})</span><a href="#" onClick={e => { e.preventDefault(); logout() }}>Logout</a></>}
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/listing/:id" element={<Listing />} />
           <Route path="/login" element={<Auth key="login" mode="login" />} />
           <Route path="/register" element={<Auth key="register" mode="register" />} />
          <Route path="/my" element={<Protect role="student"><MyStuff /></Protect>} />
          <Route path="/privacy" element={<Protect role="student"><Privacy /></Protect>} />
          <Route path="/notifications" element={<Protect><Notifications /></Protect>} />
          <Route path="/messages" element={<Protect><Messages /></Protect>} />
          <Route path="/messages/:lid/:student" element={<Protect><Chat /></Protect>} />
          <Route path="/dashboard" element={<Protect role="landlord"><Dashboard /></Protect>} />
          <Route path="/admin" element={<Protect role="admin"><Admin /></Protect>} />
          <Route path="*" element={<p>Page not found.</p>} />
        </Routes>
      </main>
    </>
  )
}
