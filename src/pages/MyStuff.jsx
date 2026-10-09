import { useState } from 'react'
import { API } from '../api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useAsyncResource } from '../hooks/useAsyncResource.js'
import ListingCard from '../components/ListingCard.jsx'
import DeferredFeature from '../components/DeferredFeature.jsx'

export default function MyStuff() {
  const { user } = useAuth()
  const [, tick] = useState(0)
  const favorites = API.favs(user.email)
  const { data: saved, loading, error, reload } = useAsyncResource(async signal => {
    const rows = await Promise.all(favorites.map(id => API.listing(id, { signal })))
    return rows.filter(Boolean)
  }, [user.email, favorites.join(',')])
  return <>
    <div className="role-banner student-banner"><div className="page-heading"><div><p className="eyebrow">STUDENT SPACE</p><h1>Saved & applications</h1></div></div></div>
    <h2>Saved listings</h2><p className="muted">Saved IDs are browser-only; residence details come from the API.</p>
    {loading && <p className="muted">Loading saved residences…</p>}
    {error && <p role="alert" className="err">{error} <button className="btn sm" onClick={reload}>Retry</button></p>}
    {!loading && !error && <div className="grid">{saved?.map(listing => <ListingCard key={listing.id} l={listing} user={user} fav onFav={id => { API.toggleFav(user.email, id); tick(value => value + 1) }} />)}</div>}
    {!loading && !error && !saved?.length && <p className="muted">No saved listings yet.</p>}
    <DeferredFeature title="Viewing requests" />
    <DeferredFeature title="Application tracker" />
    <DeferredFeature title="Leases" />
  </>
}
