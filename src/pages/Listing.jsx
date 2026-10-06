import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { money } from '../components/ListingCard.jsx'
import { readFileAsDataURL } from '../utils/browserFiles.js'

export default function Listing() {
  const { id } = useParams(); const { user } = useAuth()
  const [text, setText] = useState(''); const [date, setDate] = useState(''); const [note, setNote] = useState('')
  const [photoIndex, setPhotoIndex] = useState(0)
  const [step, setStep] = useState(1); const [application, setApplication] = useState({ phone: '', studentNumber: '', funding: 'NSFAS', guarantor: '', consent: false });
  const [files, setFiles] = useState({}); const [reason, setReason] = useState(''); const [rating, setRating] = useState('5'); const [review, setReview] = useState('')
  const [, tick] = useState(0); const refresh = () => tick(x => x + 1)
  useEffect(() => setPhotoIndex(0), [id])
  const listing = API.listing(id)
  if (!listing || (listing.status !== 'approved' && !(user && (user.role === 'admin' || user.email === listing.owner)))) return <p>Listing not found.</p>
  const photos = listing.gallery?.length ? listing.gallery : listing.image ? [listing.image] : []
  const activePhoto = photos[Math.min(photoIndex, Math.max(photos.length - 1, 0))]
  const isStudent = user?.role === 'student'; const msgs = isStudent ? API.thread(listing.id, user.email) : []
  const mine = isStudent ? API.viewings().filter(v => v.listingId === listing.id && v.student === user.email) : []
  const apps = isStudent ? API.applications().filter(a => a.listingId === listing.id && a.student === user.email) : []
  const send = () => { if (text.trim()) { API.send(listing.id, user.email, user.email, text.trim()); setText(''); refresh() } }
  const book = () => { try { API.requestViewing(listing.id, user.email, date); setDate(''); setNote('Viewing requested.'); refresh() } catch (error) { setNote(error.message) } }
  const updateApp = key => event => setApplication({ ...application, [key]: event.target.type === 'checkbox' ? event.target.checked : event.target.value })
  const apply = async event => {
    event.preventDefault()
    try {
      const documents = {}; for (const key of ['idDocument', 'registrationDocument', 'fundingDocument']) documents[key] = await readFileAsDataURL(files[key])
      API.submitApplication({ listingId: listing.id, student: user.email, applicant: user.name, ...application, documents })
      setNote('Application submitted. Follow its status under Saved & applications.'); refresh()
    } catch (error) { setNote(error.message) }
  }
  const report = event => { event.preventDefault(); API.reportListing(listing.id, user.email, reason); setReason(''); setNote('Report sent to the moderation queue.'); refresh() }
  const addReview = event => { event.preventDefault(); try { API.review(listing.id, user.email, rating, review); setReview(''); setNote('Review added.'); refresh() } catch (error) { setNote(error.message) } }

  return <>
    <Link to="/">← Browse residences</Link>
    <div className="detail-grid">
      <section className="panel listing-detail">
        <div className="gallery"><div className="photo-placeholder">{activePhoto ? <img src={activePhoto} alt={`${listing.title} residence view ${photoIndex + 1}`} /> : <span aria-hidden="true">⌂</span>}</div><div className="sample-stamp">{listing.sample ? 'Illustration · sample data' : 'Provider photo'}</div>
          {photos.length > 1 && <div className="gallery-thumbs" aria-label="Residence images">{photos.map((photo, index) => <button type="button" key={`${photo}-${index}`} className={`gallery-thumb ${photoIndex === index ? 'selected' : ''}`} onClick={() => setPhotoIndex(index)} aria-label={`Show residence image ${index + 1}`} aria-pressed={photoIndex === index}><img src={photo} alt="" /></button>)}</div>}
        </div>
        <div className="detail-heading"><div><span className={`badge ${listing.emergency ? 'em' : ''}`}>{listing.emergency ? 'Available immediately' : listing.type}</span> <span className={`badge ${listing.nsfas ? 'ok' : 'pend'}`}>{listing.nsfas ? 'NSFAS sample' : 'Accreditation unverified'}</span>
          <h1>{listing.title}</h1><p className="muted">{listing.location} · {listing.onCampus ? 'On-campus' : 'Off-campus'} · {listing.gender || 'Any gender'}</p></div><div className="price">{money(listing.price)}<small>/ month</small></div></div>
        <p>{user ? listing.desc : `${listing.desc.slice(0, 90)}…`}</p>
        <p className="address-detail"><strong>Location:</strong> {listing.location}{user && <><br /><strong>Address:</strong> {listing.address || 'Exact street address not supplied; confirm directly with the provider.'}</>}</p>
        {user && <><h2>Cost breakdown</h2><div className="cost-grid"><div>Rent <b>{money(listing.rent || listing.price)}</b></div><div>Deposit <b>{money(listing.deposit || 0)}</b></div><div>Utilities <b>{money(listing.utilities || 0)} / month</b></div></div>
          <h2>What’s included</h2><div className="amenities">{(listing.amenities || []).map(item => <span key={item} className="badge">{item}</span>)}</div>
          <h2>House rules</h2><p>{listing.houseRules || 'Confirm house rules with the provider before applying.'}</p><h2>Transport</h2><p>{listing.shuttle || 'Confirm shuttle information with the provider.'}</p>
          <h2>Provider</h2><p>{listing.ownerName || listing.owner} · Accreditation: {listing.nsfas ? 'sample marked, verify independently' : 'not verified'}</p>
          <h2>Reviews</h2>{(listing.reviews || []).map((item, index) => <p key={index}>★ {item.rating}/5 · {item.text}</p>)}{!listing.reviews?.length && <p className="muted">No reviews yet.</p>}
        </>}
      </section>
      {!user && <aside className="panel"><h2>Limited preview</h2><p>Sign in as a CPUT student to see full details, message the provider, request a viewing, and apply.</p><Link className="btn" to="/login">Student sign in</Link></aside>}
    </div>
    {isStudent && <>
      <div className="panel"><h2>Request a viewing</h2><div className="row"><label className="grow">Date and time<input type="datetime-local" value={date} onChange={event => setDate(event.target.value)} /></label><button className="btn" onClick={book}>Request slot</button></div>{mine.map(viewing => <p key={viewing.id}>{viewing.date.replace('T', ' ')} <span className={`badge ${viewing.status}`}>{viewing.status}</span></p>)}</div>
      <div className="panel"><h2>Apply for this room</h2>{apps.map(item => <p key={item.id}>Application {item.id} · <span className="badge">{item.status}</span></p>)}
        {!apps.length && <form onSubmit={apply}>
          {step === 1 ? <><label>Phone number<input required value={application.phone} onChange={updateApp('phone')} /></label><label>Student number<input required value={application.studentNumber} onChange={updateApp('studentNumber')} /></label><label>Funding type<select value={application.funding} onChange={updateApp('funding')}><option>NSFAS</option><option>Bursary</option><option>Self-funded</option></select></label><button type="button" className="btn" onClick={() => setStep(2)}>Continue to documents</button></> : <><label>Guarantor name and contact<input required value={application.guarantor} onChange={updateApp('guarantor')} /></label>
            {[['idDocument', 'ID document'], ['registrationDocument', 'Proof of registration'], ['fundingDocument', 'Proof of funding']].map(([key, title]) => <label key={key}>{title}<input type="file" accept="image/*,.pdf" required onChange={event => setFiles({ ...files, [key]: event.target.files[0] })} /></label>)}
            <label className="check"><input type="checkbox" checked={application.consent} onChange={updateApp('consent')} required />I consent to this project prototype retaining the application documents in this browser for processing. I understand this is not a secure production storage system.</label>
            <div className="row"><button type="button" className="btn alt" onClick={() => setStep(1)}>Back</button><button className="btn">Submit application</button></div></>}
        </form>}
      </div>
      <div className="panel"><h2>Message provider</h2><div className="chat">{msgs.map(message => <div key={message.id} className={`msg ${message.from === user.email ? 'me' : ''}`}>{message.text}</div>)}</div><div className="row"><input aria-label="Message" className="grow" value={text} onChange={event => setText(event.target.value)} placeholder="Ask a question about this room" /><button className="btn" onClick={send}>Send</button></div></div>
      <div className="panel"><h2>Report a concern</h2><form className="row" onSubmit={report}><label className="grow">Reason<input required value={reason} onChange={event => setReason(event.target.value)} placeholder="Fraud, misleading detail, or safety concern" /></label><button className="btn red">Report listing</button></form></div>
      <div className="panel"><h2>Leave a review</h2>{API.canReview(user.email, listing.id) ? <form className="row" onSubmit={addReview}><label>Rating<select value={rating} onChange={event => setRating(event.target.value)}>{[5, 4, 3, 2, 1].map(value => <option key={value}>{value}</option>)}</select></label><label className="grow">Review<input required value={review} onChange={event => setReview(event.target.value)} /></label><button className="btn">Submit review</button></form> : <p className="muted">A completed signed lease is required before reviewing.</p>}</div>
    </>}
    {note && <p role="status" className="status-note">{note}</p>}
  </>
}