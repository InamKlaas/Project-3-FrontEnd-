import { useState } from 'react'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'

export default function Notifications() {
  const { user } = useAuth(); const [, refresh] = useState(0); const items = API.notifications(user.email)
  return <section className="panel"><div className="page-heading"><div><p className="eyebrow">UPDATES</p><h1>Notifications</h1></div><button className="btn alt sm" onClick={() => { API.markNotificationsRead(user.email); refresh(value => value + 1) }}>Mark all read</button></div>
    {items.length ? items.map(item => <article className={`notification-row ${item.read ? 'read' : ''}`} key={item.id}><b>{item.kind === 'announcement' ? 'Announcement' : 'Update'}</b><p>{item.text}</p><small>{new Date(item.at).toLocaleString()}</small></article>) : <p className="muted">You are all caught up.</p>}
  </section>
}