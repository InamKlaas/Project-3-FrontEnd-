import { useState } from 'react'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useSignatureCanvas } from '../hooks/useSignatureCanvas.js'
import { downloadTextFile } from '../utils/browserFiles.js'
import ListingCard from '../components/ListingCard.jsx'
import { money } from '../components/ListingCard.jsx'

export default function MyStuff() {
  const { user, logout } = useAuth(); const [, tick] = useState(0); const refresh = () => tick(value => value + 1)
  const [signatureName, setSignatureName] = useState(''); const [notice, setNotice] = useState(''); const signatureCanvas = useSignatureCanvas()
  const favorites = API.favs(user.email); const saved = API.approved().filter(listing => favorites.includes(listing.id))
  const views = API.viewings().filter(viewing => viewing.student === user.email); const applications = API.applications().filter(application => application.student === user.email); const leases = API.leases().filter(lease => lease.student === user.email)
  const notifications = API.notifications(user.email)
  const sign = lease => { if (!signatureName.trim() || !signatureCanvas.hasInk(lease.id)) return setNotice('Type your full legal name and draw your signature before signing.'); try { const signature = signatureCanvas.toDataURL(lease.id); API.signLease(lease.id, signatureName, signature); setNotice('Lease signed and recorded in this browser.'); setSignatureName(''); refresh() } catch (error) { setNotice(error.message) } }
  const exportData = () => downloadTextFile('cput-home-my-data.json', JSON.stringify(API.exportMyData(user.email), null, 2))
  const deleteData = () => { if (window.confirm('Permanently remove your locally stored profile and student records from this browser?')) { API.deleteMyData(user.email); logout() } }

  return <>
    <div className="role-banner student-banner"><div className="page-heading"><div><p className="eyebrow">STUDENT SPACE</p><h1>Saved & applications</h1></div><button className="btn alt" onClick={() => { API.markNotificationsRead(user.email); refresh() }}>Mark notifications read</button></div></div>
    <section className="panel"><h2>Notifications</h2>{notifications.length ? notifications.slice(0, 8).map(item => <p key={item.id} className={item.read ? 'muted' : ''}>{item.text} <small>{new Date(item.at).toLocaleString()}</small></p>) : <p className="muted">No notifications yet.</p>}</section>
    <h2>Saved listings</h2><div className="grid">{saved.map(listing => <ListingCard key={listing.id} l={listing} user={user} fav onFav={id => { API.toggleFav(user.email, id); refresh() }} />)}</div>{!saved.length && <p className="muted">No saved listings yet.</p>}
    <section className="panel"><h2>Viewing requests</h2>{views.length ? views.map(viewing => <p key={viewing.id}>{API.listing(viewing.listingId)?.title} · {viewing.date.replace('T', ' ')} <span className="badge">{viewing.status}</span></p>) : <p className="muted">No viewing requests yet.</p>}</section>
    <section className="panel tblwrap"><h2>Application tracker</h2>{applications.length ? <table className="tbl"><thead><tr><th>Residence</th><th>Submitted</th><th>Status</th></tr></thead><tbody>{applications.map(application => <tr key={application.id}><td>{API.listing(application.listingId)?.title}</td><td>{new Date(application.createdAt).toLocaleDateString()}</td><td><span className="badge">{application.status}</span></td></tr>)}</tbody></table> : <p className="muted">You have not submitted any applications.</p>}</section>
    <h2>Leases</h2>{leases.map(lease => <article className="panel lease-paper" key={lease.id}>
      <div className="lease-actions"><span className={`badge ${lease.status === 'signed' ? 'ok' : 'pend'}`}>{lease.status}</span><button className="btn alt sm" onClick={() => window.print()}>Print / save PDF</button></div>
      <h2>Residential lease agreement</h2><p>This demo lease is a project template and is not legal advice or a legally reviewed CPUT document.</p>
      <p><b>Provider:</b> {lease.owner} · <b>Tenant:</b> {lease.tenant} ({lease.student})</p><p><b>Room:</b> {lease.room} · <b>Term:</b> {lease.startDate} to {lease.endDate}</p><p><b>Rent:</b> {money(lease.rent)} monthly · <b>Deposit:</b> {money(lease.deposit)}</p><p><b>House rules:</b> {lease.houseRules}</p>
      {lease.signedAt ? <p>Electronically signed by <b>{lease.signedName}</b> on {new Date(lease.signedAt).toLocaleString()}<br /><img className="signature-image" src={lease.signature} alt="Drawn electronic signature" /></p> : lease.status === 'expired' ? <p>This unsigned lease has expired.</p> : <div className="signature-area"><label>Type your full legal name<input value={signatureName} onChange={event => setSignatureName(event.target.value)} /></label><label>Draw signature below</label><canvas {...signatureCanvas.canvasProps(lease.id)} width="600" height="130" className="signature-canvas" aria-label="Draw your electronic signature" /><button className="btn" onClick={() => sign(lease)}>Sign lease</button></div>}
    </article>)}{!leases.length && <p className="muted">No lease agreements have been issued yet.</p>}
    <section className="panel"><h2>Your data and privacy</h2><p>Download your locally stored account/application data or delete it from this browser. Files in this prototype are not encrypted or sent to a secure service.</p><div className="row"><button className="btn alt" onClick={exportData}>Download my data</button><button className="btn red" onClick={deleteData}>Delete my data and account</button></div></section>
    {notice && <p role="status" className="status-note">{notice}</p>}
  </>
}