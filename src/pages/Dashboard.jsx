import { useState } from 'react'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { money } from '../components/ListingCard.jsx'
import { readFileAsDataURL } from '../utils/browserFiles.js'

const blank = { title: '', price: '', location: 'Bellville', address: '', campus: 'Bellville', type: 'Single', image: '', gallery: [], desc: '', emergency: false, available: true, onCampus: false, nsfas: false, gender: 'Any', beds: 1, deposit: '', utilities: '', amenities: [], houseRules: '', shuttle: '' }

export default function Dashboard() {
  const { user } = useAuth(); const [form, setForm] = useState(blank); const [tab, setTab] = useState('overview'); const [note, setNote] = useState('')
  const [, tick] = useState(0); const refresh = () => tick(value => value + 1); const set = key => event => setForm({ ...form, [key]: event.target.value })
  const mine = API.listings().filter(listing => listing.owner === user.email); const ids = mine.map(listing => listing.id)
  const requests = API.viewings().filter(viewing => ids.includes(viewing.listingId)); const apps = API.applications().filter(application => ids.includes(application.listingId))
  const pending = apps.filter(application => application.status === 'submitted' || application.status === 'under review')
  const leases = API.leases().filter(lease => lease.owner === user.email)
  const submitListing = async event => {
    event.preventDefault()
    try { API.addListing({ ...form, owner: user.email, ownerName: user.name, price: +form.price, rent: +form.price, deposit: +form.deposit, utilities: +form.utilities, beds: +form.beds }); setForm(blank); setNote('Listing submitted for admin approval.'); refresh() }
    catch (error) { setNote(error.message) }
  }
  const setAppStatus = (application, status) => { API.setApplication(application.id, status); setNote(`Application marked ${status}.`); refresh() }
  const createLease = application => {
    const listing = API.listing(application.listingId)
    const startDate = window.prompt('Lease start date (YYYY-MM-DD)', new Date().toISOString().slice(0, 10)); if (!startDate) return
    const endDate = window.prompt('Lease end date (YYYY-MM-DD)', `${new Date().getFullYear()}-12-31`); if (!endDate) return
    API.issueLease({ listingId: listing.id, owner: user.email, student: application.student, tenant: application.applicant, room: listing.title, rent: listing.price, deposit: listing.deposit || 0, startDate, endDate, houseRules: listing.houseRules || 'As agreed in writing.' }); API.updateListing(listing.id, { available: false }); API.setApplication(application.id, 'approved'); setNote('Lease sent to student for signature.'); refresh()
  }
  const uploadProof = async event => { event.preventDefault(); const formElement = event.currentTarget; const registrationNumber = formElement.elements.registrationNumber.value; const file = formElement.querySelector('input[type="file"]').files[0]; const proof = await readFileAsDataURL(file); API.onboardProvider(user.email, { registrationNumber, proof, status: 'pending-verification' }); setNote('Provider verification submitted to the admin queue.'); refresh() }

  return <>
    <div className="role-banner provider-banner"><div className="page-heading"><div><p className="eyebrow">PROVIDER SPACE</p><h1>My residences</h1></div><span className={`badge ${API.users().find(item => item.email === user.email)?.status === 'verified' ? 'ok' : 'pend'}`}>{API.users().find(item => item.email === user.email)?.status || 'pending verification'}</span></div></div>
    <div className="stats"><div className="stat"><b>{mine.length}</b>Residences</div><div className="stat"><b>{mine.filter(item => item.available).length}</b>Available beds</div><div className="stat"><b>{pending.length}</b>Applications to review</div><div className="stat"><b>{requests.filter(item => item.status === 'pending').length}</b>Viewing requests</div><div className="stat"><b>{money(mine.filter(item => !item.available).reduce((total, item) => total + item.price, 0))}</b>Estimated occupied rent</div></div>
    <div className="tabs">{['overview', 'listings', 'applications', 'viewings', 'leases'].map(item => <button key={item} className={`btn sm ${tab === item ? 'on' : ''}`} onClick={() => setTab(item)}>{item}</button>)}</div>
    {tab === 'overview' && <>
      <section className="panel"><h2>Provider verification</h2><p>Submit an ID or company registration number and proof of ownership. Listings are sample/demo until independently verified.</p><form className="form-grid" onSubmit={uploadProof}><label>ID or company registration number<input name="registrationNumber" required /></label><label>Proof of ownership (PDF/image)<input type="file" accept="image/*,.pdf" required /></label><button className="btn">Submit for verification</button></form></section>
      <section className="panel"><h2>Quick actions</h2><div className="row"><button className="btn" onClick={() => setTab('listings')}>Add a residence</button><button className="btn alt" onClick={() => setTab('applications')}>Review applications ({pending.length})</button></div></section>
    </>}
    {tab === 'listings' && <>
      <section className="panel"><h2>Add a residence</h2><form onSubmit={submitListing} className="form-grid">
        <label>Listing name<input required value={form.title} onChange={set('title')} /></label><label>Monthly rent (R)<input required type="number" min="1" value={form.price} onChange={set('price')} /></label>
        <label>Campus<select value={form.campus} onChange={set('campus')}>{['Bellville', 'Cape Town / District Six', 'Mowbray', 'Wellington', 'Athlone', 'Granger Bay'].map(item => <option key={item}>{item}</option>)}</select></label>
        <label>Area<input required value={form.location} onChange={set('location')} /></label><label className="span-all">Street address<input required value={form.address} onChange={set('address')} placeholder="Building and street address" /></label><label>Room type<select value={form.type} onChange={set('type')}><option>Single</option><option>Sharing</option><option>Bachelor</option></select></label>
        <label>Bed count<input type="number" min="1" value={form.beds} onChange={set('beds')} /></label><label>Deposit (R)<input type="number" value={form.deposit} onChange={set('deposit')} /></label><label>Utilities (R/month)<input type="number" value={form.utilities} onChange={set('utilities')} /></label>
        <label>Residence photos<input type="file" accept="image/*" multiple onChange={async event => { const photos = await Promise.all(Array.from(event.target.files).map(readFileAsDataURL)); const gallery = photos.map(photo => photo.data); setForm({ ...form, gallery, image: gallery[0] || '' }) }} /></label>
        <label className="span-all">Description<textarea required rows="3" value={form.desc} onChange={set('desc')} /></label><label className="span-all">House rules<textarea rows="2" value={form.houseRules} onChange={set('houseRules')} /></label><label className="span-all">Shuttle details<input value={form.shuttle} onChange={set('shuttle')} /></label>
        <label>Gender<select value={form.gender} onChange={set('gender')}><option>Any</option><option>Women</option><option>Men</option></select></label>
        <label className="check"><input type="checkbox" checked={form.nsfas} onChange={event => setForm({ ...form, nsfas: event.target.checked })} />NSFAS accreditation claimed (admin review required)</label>
        <label className="check"><input type="checkbox" checked={form.emergency} onChange={event => setForm({ ...form, emergency: event.target.checked })} />Emergency, available immediately</label>
        <button className="btn">Submit for approval</button>
      </form></section>
      <section className="panel tblwrap"><h2>Your listings</h2><table className="tbl"><thead><tr><th>Residence</th><th>Rent</th><th>Status</th><th>Actions</th></tr></thead><tbody>{mine.map(listing => <tr key={listing.id}><td>{listing.title}<small className="block muted">{listing.location} · {listing.beds || 1} beds</small></td><td>{money(listing.price)}</td><td><span className="badge">{listing.status}</span></td><td><div className="row"><button className="btn alt sm" onClick={() => { API.updateListing(listing.id, { available: !listing.available }); refresh() }}>{listing.available ? 'Mark leased' : 'Mark available'}</button><button className="btn alt sm" onClick={() => { const title = window.prompt('Update listing title', listing.title); if (title) { API.updateListing(listing.id, { title }); refresh() } }}>Edit name</button><button className="btn red sm" onClick={() => { if (window.confirm('Delete this listing?')) { API.removeListing(listing.id); refresh() } }}>Delete</button></div></td></tr>)}</tbody></table></section>
    </>}
    {tab === 'applications' && <section className="panel tblwrap"><h2>Applications inbox</h2>{apps.length ? <table className="tbl"><thead><tr><th>Applicant</th><th>Room</th><th>Funding</th><th>Documents</th><th>Status / actions</th></tr></thead><tbody>{apps.map(application => <tr key={application.id}><td>{application.applicant}<small className="block muted">{application.student} · {application.phone}</small></td><td>{API.listing(application.listingId)?.title}</td><td>{application.funding}</td><td>{Object.values(application.documents || {}).filter(Boolean).map(file => <a className="block" key={file.name} href={file.data} download={file.name}>{file.name}</a>)}</td><td><span className="badge">{application.status}</span><div className="row"><button className="btn alt sm" onClick={() => setAppStatus(application, 'under review')}>Review</button><button className="btn green sm" onClick={() => createLease(application)}>Accept + lease</button><button className="btn alt sm" onClick={() => setAppStatus(application, 'waitlisted')}>Waitlist</button><button className="btn red sm" onClick={() => setAppStatus(application, 'declined')}>Decline</button></div></td></tr>)}</tbody></table> : <p className="muted">No applications yet.</p>}</section>}
    {tab === 'viewings' && <section className="panel tblwrap"><h2>Viewing requests</h2><table className="tbl"><tbody>{requests.map(viewing => <tr key={viewing.id}><td>{API.listing(viewing.listingId)?.title}</td><td>{viewing.student}</td><td>{viewing.date.replace('T', ' ')}</td><td>{viewing.status === 'pending' ? <div className="row"><button className="btn green sm" onClick={() => { API.setViewing(viewing.id, 'accepted'); refresh() }}>Accept</button><button className="btn red sm" onClick={() => { API.setViewing(viewing.id, 'declined'); refresh() }}>Decline</button></div> : <span className="badge">{viewing.status}</span>}</td></tr>)}</tbody></table>{!requests.length && <p className="muted">No viewing requests yet.</p>}</section>}
    {tab === 'leases' && <section className="panel"><h2>Lease tracker</h2>{leases.map(lease => <p key={lease.id}>{lease.tenant} · {lease.room} · {lease.startDate} to {lease.endDate} · <span className="badge">{lease.status}</span></p>)}{!leases.length && <p className="muted">No leases issued. Approve an application to create one.</p>}</section>}
    {note && <p role="status" className="status-note">{note}</p>}
  </>
}