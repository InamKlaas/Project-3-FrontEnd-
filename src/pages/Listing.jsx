import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { money } from '../components/ListingCard.jsx'
import { useAsyncResource } from '../hooks/useAsyncResource.js'
import DeferredFeature from '../components/DeferredFeature.jsx'
import HousingActions from '../components/HousingActions.jsx'

export default function Listing() {
  const { id } = useParams()
  const { user } = useAuth()
  const [text, setText] = useState('')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [photoIndex, setPhotoIndex] = useState(0)
  const isStudent = user?.role === 'student'
  const detail = useAsyncResource(signal => API.listing(id, { signal }), [id, user?.email])
  const thread = useAsyncResource(signal => isStudent ? API.thread(id, user.email, { signal }) : [], [id, user?.email, isStudent])
  useEffect(() => { setPhotoIndex(0); setText(''); setNote('') }, [id])
  const send = async event => {
    event.preventDefault()
    if (!text.trim() || busy) return
    setBusy(true); setNote('')
    try { await API.send(id, user.email, user.email, text.trim()); setText(''); thread.reload() }
    catch (failure) { setNote(failure.message) }
    finally { setBusy(false) }
  }
  if (detail.loading) return <p className="muted">Loading residence…</p>
  if (detail.error) return <p role="alert" className="err">{detail.error} <button className="btn sm" onClick={detail.reload}>Retry</button></p>
  const listing = detail.data
  if (!listing) return <p>Listing not found or no longer publicly available.</p>
  const photos = listing.gallery?.length ? listing.gallery : listing.image ? [listing.image] : []
  const activePhoto = photos[Math.min(photoIndex, Math.max(photos.length - 1, 0))]

  return <>
    <Link to="/">← Browse residences</Link>
    <div className="detail-grid">
      <section className="panel listing-detail">
        <div className="gallery"><div className="photo-placeholder">{activePhoto ? <img src={activePhoto} alt={`${listing.title} residence view ${photoIndex + 1}`} /> : <span aria-hidden="true">⌂</span>}</div><div className="sample-stamp">{activePhoto?.includes('files.kuula.io') ? 'CPUT / Kuula · official tour photo' : listing.sample ? 'Illustration · sample data' : 'Provider photo'}</div>
          {photos.length > 1 && <div className="gallery-thumbs" aria-label="Residence images">{photos.map((photo, index) => <button type="button" key={`${photo}-${index}`} className={`gallery-thumb ${photoIndex === index ? 'selected' : ''}`} onClick={() => setPhotoIndex(index)} aria-label={`Show residence image ${index + 1}`} aria-pressed={photoIndex === index}><img src={photo} alt="" /></button>)}</div>}
        </div>
        <div className="detail-heading"><div><span className={`badge ${listing.emergency ? 'em' : ''}`}>{listing.emergency ? 'Emergency room offered' : listing.type}</span> <span className={`badge ${listing.nsfas ? 'ok' : 'pend'}`}>{listing.nsfas ? 'NSFAS claimed' : 'Accreditation unverified'}</span>
          <h1>{listing.title}</h1><p className="muted">{listing.location} · {listing.onCampus ? 'On-campus' : 'Off-campus'} · {listing.gender || 'Any gender'}</p></div><div className="price">{money(listing.price)}<small>/ month, starting rent</small></div></div>
        <p>{listing.desc}</p>
        <p className="address-detail"><strong>Location:</strong> {listing.location}{user && <><br /><strong>Address:</strong> {listing.address || 'Exact street address not supplied; confirm directly with the provider.'}</>}</p>
        {user && <><h2>Cost breakdown</h2><div className="cost-grid"><div>Rent <b>{money(listing.rent || listing.price)}</b></div><div>Deposit <b>{money(listing.deposit || 0)}</b></div><div>Utilities <b>{money(listing.utilities || 0)} / month</b></div></div>
          <h2>What’s included</h2><div className="amenities">{(listing.amenities || []).map(item => <span key={item} className="badge">{item}</span>)}</div>
          <h2>House rules</h2><p>{listing.houseRules || 'Confirm house rules with the provider before applying.'}</p><h2>Transport</h2><p>{listing.shuttle || 'Confirm shuttle information with the provider.'}</p>
          <h2>Provider</h2><p>{listing.ownerName || listing.owner} · Accreditation: {listing.nsfas ? 'claimed; verify independently' : 'not verified'}</p>
          {listing.rejectionReason && <p>Review reason: {listing.rejectionReason}</p>}
        </>}
      </section>
      {!user && <aside className="panel"><h2>Limited preview</h2><p>Sign in as a CPUT student to see full details and message the provider.</p><Link className="btn" to="/login">Student sign in</Link></aside>}
    </div>
    {isStudent && <>
      <div className="panel"><h2>Message provider</h2>
        {thread.loading && <p className="muted">Loading messages…</p>}
        {thread.error && <p role="alert" className="err">{thread.error}</p>}
        {!thread.loading && !thread.error && <div className="chat">{(thread.data || []).map(message => <div key={message.id} className={`msg ${message.from === user.email ? 'me' : ''}`}>{message.text}</div>)}</div>}
        <form className="row" onSubmit={send}><input aria-label="Message" maxLength={2000} className="grow" value={text} onChange={event => setText(event.target.value)} placeholder="Ask a question about this room" /><button disabled={busy || thread.loading || !!thread.error || !text.trim()} className="btn">{busy ? 'Sending…' : 'Send'}</button></form>
        <button className="btn alt sm" onClick={thread.reload}>Refresh messages</button>
        {note && <p role="alert" className="err">{note}</p>}
      </div>
      <DeferredFeature title="Request a viewing" />
      <HousingActions listing={listing} user={user} />
    </>}
  </>
}
