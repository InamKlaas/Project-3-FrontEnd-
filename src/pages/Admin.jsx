import { useState } from 'react'
import { Link } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useAsyncResource } from '../hooks/useAsyncResource.js'
import DeferredFeature from '../components/DeferredFeature.jsx'

export default function Admin() {
  const { user } = useAuth()
  const [tab, setTab] = useState('listings')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const { data, loading, error, reload } = useAsyncResource(() => Promise.all([
    API.adminListings(), API.users(), API.adminProviders(),
  ]), [user.email])
  const [all, people, providers] = data || [[], [], []]
  const users = people.filter(item => item.role !== 'admin')
  const act = async (description, action) => {
    setBusy(true); setNotice('')
    try { await action(); setNotice(description); reload() }
    catch (failure) { setNotice(failure.message) }
    finally { setBusy(false) }
  }
  const rejectListing = listing => { const reason = window.prompt('Reason for rejecting this listing'); if (reason) act(`Rejected listing #${listing.id}`, () => API.updateListing(listing.id, { status: 'rejected', rejectionReason: reason })) }
  const exportListings = async () => {
    try {
      const content = await API.exportCSV('listings')
      const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }))
      const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'cput-home-listings.csv'; anchor.click(); URL.revokeObjectURL(url)
    } catch (failure) { setNotice(failure.message) }
  }
  const bars = ['Bellville', 'Cape Town / District Six', 'Mowbray', 'Wellington', 'Athlone', 'Granger Bay', 'Parow'].map(campus => ({ campus, count: all.filter(listing => listing.campus === campus).length }))
  const max = Math.max(1, ...bars.map(item => item.count))

  return <>
    <div className="role-banner admin-banner"><div className="page-heading"><div><p className="eyebrow">MODERATION</p><h1>Admin console</h1></div><div className="row"><button disabled={loading || busy} className="btn alt sm" onClick={exportListings}>Export listings CSV</button><button disabled className="btn alt sm" title="Applications are deferred">Export applications CSV</button></div></div></div>
    {loading && <p role="status" className="muted">Loading moderation data…</p>}
    {error && <p role="alert" className="err">{error} <button className="btn sm" onClick={reload}>Retry</button></p>}
    <div className="stats"><div className="stat"><b>{all.filter(item => item.status === 'pending').length}</b>Listing reviews</div><div className="stat"><b>{providers.filter(item => item.status === 'pending-verification').length}</b>Provider checks</div><div className="stat"><b>{users.length}</b>Accounts</div><div className="stat"><b>{all.filter(item => item.active && item.emergency).length}</b>Emergency flags</div></div>
    <div className="tabs">{['listings', 'providers', 'reports', 'users', 'analytics', 'audit'].map(item => <button key={item} className={`btn sm ${tab === item ? 'on' : ''}`} onClick={() => setTab(item)}>{item}</button>)}<button className="btn alt sm" onClick={reload}>Refresh</button></div>
    {tab === 'listings' && <section className="panel tblwrap"><h2>Listing verification queue</h2><table className="tbl"><thead><tr><th>Listing</th><th>Provider</th><th>Campus / rent</th><th>Status</th><th>Actions</th></tr></thead><tbody>{all.map(listing => <tr key={listing.id}><td><Link to={`/listing/${listing.id}`}>{listing.title}</Link>{listing.sample && <small className="block muted">Sample data; independently verify</small>}</td><td>{listing.owner}</td><td>{listing.campus} · R{listing.price}</td><td><span className="badge">{listing.status}</span>{!listing.active && <span className="badge">inactive</span>}{!listing.published && <span className="badge">unpublished</span>}{listing.rejectionReason && <small className="block">{listing.rejectionReason}</small>}</td><td><div className="row">
      <button disabled={busy || loading || (listing.status === 'approved' && listing.published)} className="btn green sm" onClick={() => act(`Approved listing #${listing.id}`, () => API.updateListing(listing.id, { status: 'approved' }))}>Approve</button>
      <button disabled={busy || loading} className="btn alt sm" onClick={() => rejectListing(listing)}>Reject with reason</button>
      <button disabled={busy || loading || !listing.active} className="btn red sm" onClick={() => act(`Deactivated listing #${listing.id}`, () => API.removeListing(listing.id))}>Deactivate</button>
    </div></td></tr>)}</tbody></table>{!loading && !error && !all.length && <p className="muted">No listings to review.</p>}</section>}
    {tab === 'providers' && <section className="panel tblwrap"><h2>Landlords and accreditation</h2><p className="muted">The accreditation flag is a demo claim, not verified CPUT accreditation.</p><table className="tbl"><thead><tr><th>Provider</th><th>Verification</th><th>Demo accreditation</th><th>Actions</th></tr></thead><tbody>{providers.map(provider => <tr key={provider.email}><td>{provider.name}<small className="block muted">{provider.email}</small></td><td>{provider.status}</td><td>{provider.accreditation ? 'Flagged' : 'Not flagged'}</td><td><div className="row">
      <button disabled={busy || loading || provider.status === 'verified'} className="btn green sm" onClick={() => act(`Verified provider ${provider.email}`, () => API.verifyProvider(provider.userId, 'VERIFIED'))}>Verify</button>
      <button disabled={busy || loading} className="btn alt sm" onClick={() => act(`Updated accreditation flag for ${provider.email}`, () => API.setAccreditation(provider.email, !provider.accreditation))}>{provider.accreditation ? 'Remove accreditation flag' : 'Mark accreditation flag'}</button>
      <button disabled={busy || loading || provider.status === 'rejected'} className="btn red sm" onClick={() => act(`Rejected provider ${provider.email}`, () => API.verifyProvider(provider.userId, 'REJECTED'))}>Reject</button>
    </div></td></tr>)}</tbody></table></section>}
    {tab === 'users' && <section className="panel tblwrap"><h2>User management</h2><table className="tbl"><thead><tr><th>User</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead><tbody>{users.map(item => <tr key={item.email}><td>{item.name}<small className="block muted">{item.email}</small></td><td>{item.role}</td><td>{item.status}</td><td><div className="row"><button disabled={busy || loading} className="btn alt sm" onClick={() => act(`Changed account state for ${item.email}`, () => API.setUserEnabled(item.userId, item.status === 'suspended'))}>{item.status === 'suspended' ? 'Restore' : 'Suspend'}</button><button disabled={busy || loading} className="btn red sm" onClick={() => { if (window.confirm(`Remove ${item.email}?`)) act(`Removed user ${item.email}`, () => API.removeUser(item.email)) }}>Remove</button></div></td></tr>)}</tbody></table></section>}
    {tab === 'analytics' && <section className="panel"><h2>Listings by campus</h2><div className="bar-chart">{bars.map(item => <div className="bar-row" key={item.campus}><span>{item.campus}</span><div className="bar-track"><div className="bar-fill" style={{ width: `${item.count / max * 100}%` }} /></div><b>{item.count}</b></div>)}</div><p className="muted">Counts use the current moderation feed. Application analytics are deferred.</p></section>}
    {tab === 'reports' && <DeferredFeature title="Fraud and misleading-listing reports" />}
    {tab === 'audit' && <DeferredFeature title="Announcements and audit reporting">Core moderation decisions are persisted in the database; a full reporting UI is deferred.</DeferredFeature>}
    {notice && <p role="status" className="status-note">{notice}</p>}
  </>
}
