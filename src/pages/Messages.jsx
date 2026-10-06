import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'

export default function Messages() {
  const { user } = useAuth(); const th = API.threadsFor(user)
  return (
    <><h2>Messages</h2>
      <div className="panel">{th.length ? th.map(t => <p key={t.listingId + t.student}><Link to={`/messages/${t.listingId}/${encodeURIComponent(t.student)}`}>{t.title}</Link> <span className="muted">· {t.student}</span></p>) : <p className="muted">No conversations yet. Open a listing to start one.</p>}</div></>
  )
}

export function Chat() {
  const { lid, student } = useParams(); const { user } = useAuth(); const s = decodeURIComponent(student)
  const [t, setT] = useState(''); const [, tick] = useState(0)
  const send = () => { if (t.trim()) { API.send(lid, s, user.email, t.trim()); setT(''); tick(x => x + 1) } }
  return (
    <><Link to="/messages">← Back</Link>
      <div className="panel"><h3>{API.listing(lid)?.title}</h3>
        <div className="chat">{API.thread(lid, s).map(m => <div key={m.id} className={`msg ${m.from === user.email ? 'me' : ''}`}>{m.text}</div>)}</div>
        <div className="row"><input style={{ flex: 1 }} value={t} onChange={e => setT(e.target.value)} placeholder="Type a reply…" /><button className="btn" onClick={send}>Send</button></div></div></>
  )
}
