import { useState } from 'react'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { money } from '../components/ListingCard.jsx'
import { useAsyncResource } from '../hooks/useAsyncResource.js'
import DeferredFeature from '../components/DeferredFeature.jsx'

const blank = { title: '', price: '', location: 'Bellville', address: '', campus: 'Bellville', type: 'Single', photos: '', desc: '', emergency: false, available: true, availableDate: '', onCampus: false, nsfas: false, gender: 'Any', beds: 1, deposit: '', utilities: '', amenities: [], houseRules: '', shuttle: '' }

export default function Dashboard() {
  const { user } = useAuth()
  const [form, setForm] = useState(blank)
  const [tab, setTab] = useState('overview')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const { data, loading, error, reload } = useAsyncResource(signal => Promise.all([
    API.providerListings({ signal }), API.me({ signal }),
  ]), [user.email])
  const mine = data?.[0] || []
  const badge = data?.[1]?.status || user.status
  const set = key => event => setForm({ ...form, [key]: event.target.value })
  const act = async (action, message) => {
    setBusy(true); setNote('')
    try { await action(); setNote(message); reload() }
    catch (failure) { setNote(failure.message) }
    finally { setBusy(false) }
  }
  const submitListing = async event => {
    event.preventDefault()
    await act(async () => {
      const gallery = form.photos.split(/\r?\n/).map(value => value.trim()).filter(Boolean)
      if (gallery.some(value => !/^(https?:\/\/|\/images\/)/.test(value) || value.length > 500)) throw new Error('Use HTTP(S) photo URLs or existing /images/ references, up to 500 characters each.')
      await API.addListing({ ...form, gallery, price: +form.price, deposit: +form.deposit, utilities: +form.utilities, beds: +form.beds })
      setForm(blank)
    }, 'Listing submitted for admin approval.')
  }

  return <>
    <div className="role-banner provider-banner"><div className="page-heading"><div><p className="eyebrow">PROVIDER SPACE</p><h1>My residences</h1></div><span className={`badge ${badge === 'verified' ? 'ok' : 'pend'}`}>{badge}</span></div></div>
    {loading && <p role="status" className="muted">Loading your residences…</p>}
    {error && <p role="alert" className="err">{error} <button className="btn sm" onClick={reload}>Retry</button></p>}
    <div className="stats"><div className="stat"><b>{mine.length}</b>Residences</div><div className="stat"><b>{mine.filter(item => item.active && item.available).length}</b>Available residences</div><div className="stat"><b>{mine.filter(item => item.status === 'pending').length}</b>Pending review</div><div className="stat"><b>{mine.filter(item => !item.active).length}</b>Inactive</div></div>
    <div className="tabs">{['overview', 'listings', 'applications', 'viewings', 'leases'].map(item => <button key={item} className={`btn sm ${tab === item ? 'on' : ''}`} onClick={() => setTab(item)}>{item}</button>)}</div>
    {tab === 'overview' && <>
      <section className="panel"><h2>Provider verification</h2><p>Account status: <strong>{badge}</strong>. An administrator verifies providers in the admin console before listing creation.</p><p className="muted">Document uploads are deferred. No ownership proof is collected in this POC.</p><button className="btn alt sm" onClick={reload}>Refresh verification</button></section>
      <section className="panel"><h2>Quick actions</h2><button className="btn" onClick={() => setTab('listings')}>Add a residence</button></section>
    </>}
    {tab === 'listings' && <>
      <section className="panel"><h2>Add a residence</h2>{badge !== 'verified' && <p className="muted">Your account must be verified before submitting.</p>}<form onSubmit={submitListing}><fieldset disabled={busy || loading || badge !== 'verified'} className="form-grid" style={{ border: 0, padding: 0, margin: 0 }}>
        <label>Listing name<input required maxLength={150} value={form.title} onChange={set('title')} /></label><label>Monthly rent (R)<input required type="number" min="1" value={form.price} onChange={set('price')} /></label>
        <label>Campus<select value={form.campus} onChange={set('campus')}>{['Bellville', 'Cape Town / District Six', 'Mowbray', 'Wellington', 'Athlone', 'Granger Bay'].map(item => <option key={item}>{item}</option>)}</select></label>
        <label>Area<input required maxLength={200} value={form.location} onChange={set('location')} /></label><label className="span-all">Street address<input required maxLength={300} value={form.address} onChange={set('address')} placeholder="Building and street address" /></label><label>Room type<select value={form.type} onChange={set('type')}><option>Single</option><option>Sharing</option><option>Bachelor</option></select></label>
        <label>Bed count<input type="number" min="1" value={form.beds} onChange={set('beds')} /></label><label>Deposit (R)<input type="number" min="0" value={form.deposit} onChange={set('deposit')} /></label><label>Utilities (R/month)<input type="number" min="0" value={form.utilities} onChange={set('utilities')} /></label>
        <label className="span-all">Residence photo URLs<textarea value={form.photos} onChange={set('photos')} placeholder="One HTTP(S) URL or /images/ reference per line" /></label>
        <label className="span-all">Description<textarea required maxLength={4000} rows="3" value={form.desc} onChange={set('desc')} /></label><label className="span-all">House rules<textarea maxLength={2000} rows="2" value={form.houseRules} onChange={set('houseRules')} /></label><label className="span-all">Shuttle details<input maxLength={500} value={form.shuttle} onChange={set('shuttle')} /></label>
        <label>Available from<input type="date" value={form.availableDate} onChange={set('availableDate')} /></label><label>Gender<select value={form.gender} onChange={set('gender')}><option>Any</option><option>Women</option><option>Men</option></select></label>
        <label className="check"><input type="checkbox" checked={form.onCampus} onChange={event => setForm({ ...form, onCampus: event.target.checked })} />On-campus claim</label>
        <label className="check"><input type="checkbox" checked={form.nsfas} onChange={event => setForm({ ...form, nsfas: event.target.checked })} />NSFAS accreditation claimed (admin review required)</label>
        <label className="check"><input type="checkbox" checked={form.emergency} onChange={event => setForm({ ...form, emergency: event.target.checked })} />Emergency, available immediately</label>
        <button className="btn">{busy ? 'Submitting…' : 'Submit for approval'}</button>
      </fieldset></form></section>
      <section className="panel tblwrap"><h2>Your listings</h2><table className="tbl"><thead><tr><th>Residence</th><th>Rent</th><th>Status</th><th>Actions</th></tr></thead><tbody>{mine.map(listing => <tr key={listing.id}><td>{listing.title}<small className="block muted">{listing.location} · {listing.beds || 1} beds</small></td><td>{money(listing.price)}</td><td><span className="badge">{listing.status}</span>{!listing.active && <span className="badge">inactive</span>}{listing.rejectionReason && <small className="block">{listing.rejectionReason}</small>}</td><td><div className="row">
        <button disabled={busy || !listing.active} className="btn alt sm" onClick={() => act(() => API.updateListing(listing.id, { available: !listing.available }), 'Availability updated.')}>{listing.available ? 'Mark leased' : 'Mark available'}</button>
        <button disabled={busy} className="btn alt sm" onClick={() => { const title = window.prompt('Update listing title', listing.title); if (title) act(() => API.updateListing(listing.id, { title }), 'Listing name updated.') }}>Edit name</button>
        <button disabled={busy} className="btn alt sm" onClick={() => { const price = window.prompt('New monthly rent (approved listings return to review)', listing.price); if (price) act(() => API.updateListing(listing.id, { price }), 'Rent updated; approved listings return to review.') }}>Edit rent</button>
        <button disabled={busy} className={`btn ${listing.active ? 'red' : 'alt'} sm`} onClick={() => { if (!listing.active) act(() => API.updateListing(listing.id, { active: true }), 'Listing reactivated.'); else if (window.confirm('Deactivate this listing?')) act(() => API.removeListing(listing.id), 'Listing deactivated.') }}>{listing.active ? 'Deactivate' : 'Reactivate'}</button>
      </div></td></tr>)}</tbody></table>{!loading && !error && !mine.length && <p className="muted">No residences yet.</p>}</section>
    </>}
    {tab === 'applications' && <DeferredFeature title="Applications inbox" />}
    {tab === 'viewings' && <DeferredFeature title="Viewing requests" />}
    {tab === 'leases' && <DeferredFeature title="Lease tracker" />}
    {note && <p role="status" className="status-note">{note}</p>}
  </>
}
