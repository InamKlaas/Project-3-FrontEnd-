import { useAuth } from '../context/AuthContext.jsx'
import { API } from '../api.js'
import { downloadTextFile } from '../utils/browserFiles.js'

export default function Privacy() {
  const { user } = useAuth()
  const remove = () => { if (window.confirm('Clear your browser-only demo data? Your API account and messages will remain.')) { API.deleteMyData(user.email); window.location.reload() } }
  return <section className="panel narrow"><p className="eyebrow">BROWSER DATA</p><h1>Your data</h1><p>Account and message data now live in MySQL. Server-side privacy export/deletion is deferred beyond the stage-7 POC.</p><p>These controls only manage the demo data saved in this browser.</p><div className="row privacy-actions"><button className="btn" onClick={() => downloadTextFile('cput-home-browser-data.json', JSON.stringify({ email: user.email, favorites: API.favs(user.email) }, null, 2))}>Download saved IDs</button><button className="btn red" onClick={remove}>Clear browser-only demo data</button></div></section>
}
