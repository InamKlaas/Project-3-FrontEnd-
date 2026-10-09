import { useState } from 'react'
import { API } from '../api.js'
import { useAsyncResource } from '../hooks/useAsyncResource.js'

export default function HousingActions({ listing, user }) {
  const applications = useAsyncResource(signal => API.applications({ signal }), [listing.id, user.email])
  const reports = useAsyncResource(signal => API.reports({ signal }), [listing.id, user.email])
  const reviews = useAsyncResource(signal => API.reviews(listing.id, { signal }), [listing.id])
  const [note, setNote] = useState('')
  const [moveIn, setMoveIn] = useState('')
  const [concern, setConcern] = useState('')
  const [review, setReview] = useState('')
  const [rating, setRating] = useState('5')
  const [files, setFiles] = useState([])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const application = applications.data?.find(item => item.listingId === listing.id)
  const report = reports.data?.find(item => item.listingId === listing.id)
  const act = async (action, text) => {
    setBusy(true); setMessage('')
    try { await action(); setMessage(text); applications.reload(); reports.reload(); reviews.reload() }
    catch (error) { setMessage(error.message); applications.reload() }
    finally { setBusy(false) }
  }
  const apply = event => {
    event.preventDefault()
    act(async () => {
      if (files.length > 3 || files.some(file => file.size > 5 * 1024 * 1024)) throw new Error('Choose up to three documents, at most 5 MB each.')
      const row = await API.submitApplication({ listingId: listing.id, note, moveIn })
      for (const file of files) await API.uploadDocument(row.id, file)
      setFiles([])
    }, 'Application submitted. You can track it in My housing.')
  }
  return <div className="housing-actions">
    <section className="panel"><h2>Apply for this room</h2>
      {applications.error && <p role="alert" className="err">{applications.error} <button className="btn sm" onClick={applications.reload}>Retry</button></p>}
      {application ? <><p className="status-note">Application #{application.id}: <strong>{application.status}</strong></p>
        <p>{application.note}</p><p>Move-in: {application.moveIn}</p>
        {application.documents.map(doc => <button key={doc.url} className="btn alt sm" onClick={() => act(() => API.downloadDocument(doc), 'Document downloaded.')}>{doc.name}</button>)}
        {!['withdrawn', 'declined'].includes(application.status) && <label>Add document (PDF, JPEG or PNG, up to 5 MB)<input disabled={busy} type="file" accept=".pdf,.jpg,.jpeg,.png" onChange={event => { const file = event.target.files[0]; if (file) act(() => API.uploadDocument(application.id, file), 'Document uploaded.'); event.target.value = '' }} /></label>}
      </> : <form onSubmit={apply}><label>Preferred move-in date<input required type="date" min={new Date().toLocaleDateString('en-CA')} value={moveIn} onChange={event => setMoveIn(event.target.value)} /></label>
        <label>Application message<textarea required minLength={3} maxLength={2000} value={note} onChange={event => setNote(event.target.value)} placeholder="Tell the provider about your accommodation needs" /></label>
        <label>Supporting documents (optional, up to 3 files, 5 MB each)<input type="file" multiple accept=".pdf,.jpg,.jpeg,.png" onChange={event => setFiles([...event.target.files])} /></label>
        <button className="btn" disabled={busy || applications.loading || !!applications.error || !listing.available}>{busy ? 'Submitting…' : 'Submit application'}</button></form>}
    </section>
    <section className="panel"><h2>Report a concern</h2>
      {report ? <p className="status-note">Your report #{report.id} is <strong>{report.status}</strong>.</p> : <form onSubmit={event => { event.preventDefault(); act(() => API.reportListing(listing.id, user.email, concern), 'Concern sent to the admin team.') }}>
        <label>Concern details<textarea required minLength={3} maxLength={2000} value={concern} onChange={event => setConcern(event.target.value)} placeholder="Describe misleading information or another concern" /></label>
        <button className="btn red" disabled={busy || reports.loading || !!reports.error}>Send concern</button></form>}
      {reports.error && <p role="alert" className="err">{reports.error} <button className="btn sm" onClick={reports.reload}>Retry</button></p>}
    </section>
    <section className="panel"><h2>Leave a review</h2>
      <form onSubmit={event => { event.preventDefault(); act(async () => { await API.review(listing.id, user.email, rating, review); setReview('') }, 'Review published.') }}>
        <label>Rating<select value={rating} onChange={event => setRating(event.target.value)}>{[5, 4, 3, 2, 1].map(value => <option key={value} value={value}>{value} star{value !== 1 ? 's' : ''}</option>)}</select></label>
        <label>Your review<textarea required minLength={3} maxLength={2000} value={review} onChange={event => setReview(event.target.value)} /></label>
        <button className="btn" disabled={busy}>Publish review</button></form>
      {reviews.loading && <p>Loading reviews…</p>}{reviews.error && <p role="alert" className="err">{reviews.error}</p>}
      {reviews.data?.map(row => <article className="review" key={row.id}><strong>{row.name} · {row.rating}/5</strong><p>{row.text}</p></article>)}
      {!reviews.loading && !reviews.data?.length && <p className="muted">No reviews yet.</p>}
    </section>
    {message && <p role="status" className="status-note">{message}</p>}
  </div>
}
