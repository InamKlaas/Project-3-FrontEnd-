import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useAsyncResource } from '../hooks/useAsyncResource.js'

export default function Messages() {
  const { user } = useAuth()
  const { data: threads, loading, error, reload } = useAsyncResource(signal => API.threadsFor(user, { signal }), [user.email])
  return <><h2>Messages</h2><div className="panel">
    {loading ? <p className="muted">Loading conversations…</p> : error ? <p role="alert" className="err">{error}</p> : threads?.length ? threads.map(thread => <p key={`${thread.listingId}|${thread.student}`}><Link to={`/messages/${thread.listingId}/${encodeURIComponent(thread.student)}`}>{thread.title}</Link> <span className="muted">· {thread.student}</span></p>) : <p className="muted">No conversations yet. Open a listing to start one.</p>}
    <button className="btn alt sm" onClick={reload}>Refresh conversations</button>
  </div></>
}

export function Chat() {
  const { lid, student } = useParams()
  const { user } = useAuth()
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const { data, loading, error, reload } = useAsyncResource(async signal => {
    const [listing, messages] = await Promise.all([API.listing(lid, { signal }), API.thread(lid, student, { signal })])
    return { listing, messages }
  }, [lid, student, user.email])
  const send = async event => {
    event.preventDefault()
    if (!text.trim() || busy) return
    setBusy(true); setNotice('')
    try { await API.send(lid, student, user.email, text.trim()); setText(''); reload() }
    catch (failure) { setNotice(failure.message) }
    finally { setBusy(false) }
  }
  return <><Link to="/messages">← Back</Link><div className="panel"><h3>{data?.listing?.title || 'Conversation'}</h3>
    {loading && <p className="muted">Loading conversation…</p>}
    {error && <p role="alert" className="err">{error}</p>}
    {!loading && !error && <div className="chat">{data?.messages.map(message => <div key={message.id} className={`msg ${message.from === user.email ? 'me' : ''}`}>{message.text}</div>)}</div>}
    <form className="row" onSubmit={send}><input aria-label="Reply" style={{ flex: 1 }} maxLength={2000} value={text} onChange={event => setText(event.target.value)} placeholder="Type a reply…" /><button disabled={busy || loading || !!error || !text.trim()} className="btn">{busy ? 'Sending…' : 'Send'}</button></form>
    <button className="btn alt sm" onClick={reload}>Refresh messages</button>
    {notice && <p role="alert" className="err">{notice}</p>}
  </div></>
}
