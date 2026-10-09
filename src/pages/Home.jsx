import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import ListingCard from '../components/ListingCard.jsx'

export default function Home() {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const [f, setF] = useState({ text: '', campus: '', type: '', min: '', max: '', avail: '', em: params.get('emergency') ? '1' : '', onCampus: '', nsfas: '', gender: '', amenity: '', sort: 'priority' })
  const [, tick] = useState(0)
  const [result, setResult] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [attempt, retry] = useState(0)
  const [failed, setFailed] = useState('')
  const set = k => e => { setF({ ...f, [k]: e.target.value }); setPage(0) }
  const emergencyParam = params.get('emergency')
  useEffect(() => { setF(current => ({ ...current, em: emergencyParam ? '1' : '' })); setPage(0) }, [emergencyParam])
  useEffect(() => {
    const controller = new AbortController()
    let live = true
    setLoading(true); setFailed('')
    const timer = setTimeout(() => {
      API.search({ search: f.text, campus: f.campus, type: f.type, minPrice: f.min, maxPrice: f.max,
        availableBy: f.avail, emergency: f.em ? true : '', onCampus: f.onCampus, nsfas: f.nsfas,
        gender: f.gender, amenity: f.amenity, sort: f.sort, page, size: 12 }, { signal: controller.signal })
        .then(rows => { if (live) setResult(rows) })
        .catch(error => { if (live) setFailed(error.message) })
        .finally(() => { if (live) setLoading(false) })
    }, 200)
    return () => { live = false; clearTimeout(timer); controller.abort() }
  }, [f, page, attempt, user?.email])
  const favs = user?.role === 'student' ? API.favs(user.email) : []

  const results = result?.content || []

  return (
    <>
      <section className="hero"><h1>Find a place to call home</h1>
        <p>Explore student accommodation around CPUT campuses. Verify details before paying.</p></section>
      <div className="sample-notice"><strong>Explore real CPUT residences.</strong> The new CPUT residence names and photos come from its official virtual tours. Prices, availability and provider accounts are development examples. <a href="https://www.cput.ac.za/student/support-services/dsa/residence/view-residences-in-360" target="_blank" rel="noreferrer">View CPUT's official directory</a>.</div>
      <div className="alert"><span>🚨 <b>Arrived without a place?</b> See rooms available right now.</span>
        <button className="btn red sm" onClick={() => { setF({ ...f, em: '1' }); setPage(0) }}>Show emergency rooms</button></div>
      <div className="filters">
        <label>Search<input aria-label="Search title or area" placeholder="Residence or area" value={f.text} onChange={set('text')} /></label>
        <label>Campus<select value={f.campus} onChange={set('campus')}><option value="">All campuses</option>{['Bellville', 'Cape Town / District Six', 'Mowbray', 'Wellington', 'Athlone', 'Granger Bay', 'Parow'].map(x => <option key={x}>{x}</option>)}</select></label>
        <label>Room type<select value={f.type} onChange={set('type')}><option value="">Any room type</option><option>Single</option><option>Sharing</option><option>Bachelor</option></select></label>
        <label>Minimum rent<input aria-label="Minimum rent" type="number" placeholder="R minimum" value={f.min} onChange={set('min')} /></label>
        <label>Maximum rent<input aria-label="Maximum rent" type="number" placeholder="R maximum" value={f.max} onChange={set('max')} /></label>
        <label>Available by<input type="date" value={f.avail} onChange={set('avail')} /></label>
        <label>Setting<select value={f.onCampus} onChange={set('onCampus')}><option value="">On or off campus</option><option value="true">On-campus</option><option value="false">Off-campus</option></select></label>
        <label>NSFAS<select value={f.nsfas} onChange={set('nsfas')}><option value="">Any accreditation</option><option value="true">NSFAS-accredited</option><option value="false">Not accredited</option></select></label>
        <label>Gender<select value={f.gender} onChange={set('gender')}><option value="">Any gender</option><option>Any</option><option>Women</option><option>Men</option></select></label>
        <label>Amenity<select value={f.amenity} onChange={set('amenity')}><option value="">Any amenity</option>{['WiFi', 'Laundry', 'Security', 'Shuttle'].map(x => <option key={x}>{x}</option>)}</select></label>
        <label>Show<select value={f.em} onChange={set('em')}><option value="">All listings</option><option value="1">Emergency only</option></select></label>
        <label>Sort<select value={f.sort} onChange={set('sort')}><option value="priority">Emergency first</option><option value="price-low">Price: low to high</option><option value="price-high">Price: high to low</option><option value="newest">Newest</option></select></label>
      </div>
      {failed && <p role="alert" className="err">{failed} <button className="btn sm" onClick={() => retry(value => value + 1)}>Retry</button></p>}
      {loading && <p role="status" className="muted">Loading residences…</p>}
      {!loading && !failed && <p className="muted">{result?.totalElements || 0} listing(s) found</p>}
      <h2 className="section-title">{f.em ? 'Available immediately' : 'Places to explore'}</h2><div className="grid">
        {!loading && !failed && results.map(l => <ListingCard key={l.id} l={l} user={user} fav={favs.includes(l.id)} onFav={id => { API.toggleFav(user.email, id); tick(x => x + 1) }} />)}
      </div>
      {!loading && !failed && !results.length && <p>No listings match your filters. Clear a filter to see more.</p>}
      {!loading && !failed && result?.totalPages > 1 && <div className="row" aria-label="Listing pages"><button className="btn alt sm" disabled={result.first} onClick={() => setPage(value => value - 1)}>Previous</button><span>Page {result.page + 1} of {result.totalPages}</span><button className="btn alt sm" disabled={result.last} onClick={() => setPage(value => value + 1)}>Next</button></div>}
      {!user && <p className="muted">Guests see a limited preview. Register to view full details and contact landlords.</p>}
    </>
  )
}
