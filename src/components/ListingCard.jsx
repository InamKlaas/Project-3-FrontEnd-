import { Link } from 'react-router-dom'
export const money = n => 'R' + Number(n).toLocaleString()

export default function ListingCard({ l, user, fav, onFav }) {
  return (
    <article className="card">
      <div className="img" style={l.image ? { backgroundImage: `url(${l.image})` } : {}}>{l.image ? '' : '🏠'}</div>
      <div className="body">
        {l.emergency && <span className="badge em">Emergency</span>}<span className="badge">{l.type}</span>
        {user?.role === 'student' && <button type="button" className="heart" onClick={() => onFav(l.id)} aria-label={fav ? 'Remove saved listing' : 'Save listing'} aria-pressed={!!fav}>{fav ? '❤️' : '🤍'}</button>}
        <h3>{l.title}</h3>
        <div className="muted">📍 {l.location} · {l.available ? 'Available now' : 'Unavailable'}</div>
        <p className="price">{money(l.price)}/month</p>
        <Link className="btn sm" to={`/listing/${l.id}`}>{user ? 'View details' : 'Preview'}</Link>
      </div>
    </article>
  )
}
