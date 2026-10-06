import { useAuth } from '../context/AuthContext.jsx'
import { API } from '../api.js'
import { downloadTextFile } from '../utils/browserFiles.js'

export default function Privacy() {
  const { user, logout } = useAuth()
  const remove = () => { if (window.confirm('Permanently remove your account and student data stored in this browser?')) { API.deleteMyData(user.email); logout() } }
  return <section className="panel narrow"><p className="eyebrow">POPIA CONTROLS</p><h1>Your data</h1><p>Export a copy of your CPUT Home prototype data or delete your locally stored profile and student records.</p><div className="notice">This is a classroom prototype. Data and uploaded files are stored in browser localStorage, are not encrypted, and must not contain real identity documents or sensitive information.</div><div className="row privacy-actions"><button className="btn" onClick={() => downloadTextFile('cput-home-my-data.json', JSON.stringify(API.exportMyData(user.email), null, 2))}>Download my data</button><button className="btn red" onClick={remove}>Delete my data</button></div></section>
}