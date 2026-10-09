import { useState } from 'react'
import { Link } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useAsyncResource } from '../hooks/useAsyncResource.js'

export default function ApplicationsInbox() {
  const { user } = useAuth()
  const { data, error, loading, reload } = useAsyncResource(signal => API.applications({ signal }), [user.email])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const act = async action => { setBusy(true); setMessage(''); try { await action(); reload() } catch (failure) { setMessage(failure.message) } finally { setBusy(false) } }
  return <section className="panel"><h2>{user.role === 'student' ? 'Application tracker' : 'Applications inbox'}</h2>
    {loading && <p>Loading applications…</p>}{error && <p role="alert" className="err">{error}</p>}
    <button className="btn alt sm" onClick={reload}>Refresh applications</button>
    {data?.map(row => <article className="application-row" key={row.id}><h3><Link to={`/listing/${row.listingId}`}>{row.title}</Link></h3>
      <p>{row.name} · Move-in {row.moveIn} · <span className={`badge ${row.status}`}>{row.status}</span></p><p>{row.note}</p>
      <div className="row">{row.documents.map(doc => <button className="btn alt sm" key={doc.url} disabled={busy} onClick={() => act(() => API.downloadDocument(doc))}>{doc.name}</button>)}
      {row.status !== 'withdrawn' && (user.role === 'student' ? <button className="btn alt sm" disabled={busy} onClick={() => act(() => API.setApplication(row.id, 'withdrawn'))}>Withdraw application</button> : ['accepted', 'waitlisted', 'declined'].map(status => <button key={status} disabled={busy || row.status === status} className="btn alt sm" onClick={() => act(() => API.setApplication(row.id, status))}>{status === 'accepted' ? 'Accept' : status === 'declined' ? 'Decline' : 'Waitlist'}</button>))}</div>
    </article>)}
    {!loading && !data?.length && <p className="muted">No applications yet.</p>}{message && <p role="alert" className="err">{message}</p>}
  </section>
}
