import { useState, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import ListingCard from '../components/ListingCard.jsx'

export default function Home() {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const [f, setF] = useState({ text: '', campus: '', type: '', min: '', max: '', avail: '', em: params.get('emergency') ? '1' : '', onCampus: '', nsfas: '', gender: '', amenity: '', sort: 'priority' })
  const [, tick] = useState(0)
  const set = k => e => setF({ ...f, [k]: e.target.value })
  const favs = user?.role === 'student' ? API.favs(user.email) : []

  const results = useMemo(() => API.approved().filter(l =>
    (!f.text || (l.title + l.location).toLowerCase().includes(f.text.toLowerCase())) &&
    (!f.campus || l.campus === f.campus) && (!f.onCampus || String(l.onCampus) === f.onCampus) &&
    (!f.nsfas || String(l.nsfas) === f.nsfas) && (!f.gender || l.gender === f.gender) &&
    (!f.amenity || l.amenities?.includes(f.amenity)) &&
    (!f.type || l.type === f.type) && l.price >= (+f.min || 0) && l.price <= (+f.max || Infinity) &&
    (!f.avail || (l.available && l.availableDate <= f.avail)) && (!f.em || l.emergency)
  ).sort((a, b) => f.sort === 'price-low' ? a.price - b.price : f.sort === 'price-high' ? b.price - a.price : f.sort === 'newest' ? b.id - a.id : Number(b.emergency) - Number(a.emergency)), [f])

  return (
    <>
      <section className="hero"><h1>Find a place to call home</h1>
        <p>Explore student accommodation around CPUT campuses. Verify details before paying.</p></section>
      <div className="sample-notice"><strong>Sample data for a student project.</strong> Names, prices, availability, accreditation and provider details are illustrative and must be verified against CPUT's official list. CPUT Home is not an official CPUT service.</div>
      <div className="alert"><span>🚨 <b>Arrived without a place?</b> See rooms available right now.</span>
        <button className="btn red sm" onClick={() => setF({ ...f, em: '1' })}>Show emergency rooms</button></div>
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
      <p className="muted">{results.length} listing(s) found</p>
      <h2 className="section-title">{f.em ? 'Available immediately' : 'Places to explore'}</h2><div className="grid">
        {results.map(l => <ListingCard key={l.id} l={l} user={user} fav={favs.includes(l.id)} onFav={id => { API.toggleFav(user.email, id); tick(x => x + 1) }} />)}
      </div>
      {!results.length && <p>No listings match your filters. Clear a filter to see more.</p>}
      {!user && <p className="muted">Guests see a limited preview. Register to view full details and contact landlords.</p>}
    </>
  )
}
