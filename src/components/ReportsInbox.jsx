import { useState } from 'react'
import { API } from '../api.js'
import { useAsyncResource } from '../hooks/useAsyncResource.js'

export default function ReportsInbox() {
  const { data, loading, error, reload } = useAsyncResource(signal => API.reports({ signal }), [])
  const [busy, setBusy] = useState(false)
  const [note, setNote] = useState('')
  const resolve = async (id, status) => { setBusy(true); setNote(''); try { await API.resolveReport(id, status); reload() } catch (failure) { setNote(failure.message) } finally { setBusy(false) } }
  return <section className="panel"><h2>Reported concerns</h2><button className="btn alt sm" onClick={reload}>Refresh reports</button>
    {loading && <p>Loading reports…</p>}{error && <p role="alert" className="err">{error}</p>}
    {data?.map(row => <article key={row.id} className="application-row"><h3>{row.title}</h3><p>{row.name} · <span className="badge">{row.status}</span></p><p>{row.note}</p>
      {row.status === 'open' && <div className="row"><button className="btn sm" disabled={busy} onClick={() => resolve(row.id, 'resolved')}>Resolve</button><button className="btn alt sm" disabled={busy} onClick={() => resolve(row.id, 'dismissed')}>Dismiss</button></div>}</article>)}
    {!loading && !data?.length && <p className="muted">No concerns reported.</p>}{note && <p role="alert" className="err">{note}</p>}
  </section>
}
