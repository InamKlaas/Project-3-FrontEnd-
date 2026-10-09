import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth, homeFor } from '../context/AuthContext.jsx'
import { STUDENT_DOMAIN } from '../api.js'

export default function Auth({ mode }) {
  const reg = mode === 'register'; const { user, login, register } = useAuth(); const nav = useNavigate()
  const [f, setF] = useState({ name: '', email: '', password: '', role: 'student', studentNumber: '', campus: 'Bellville', year: '1', funding: 'NSFAS' }); const [err, setErr] = useState('')
  const [verification, setVerification] = useState(false)
  const [busy, setBusy] = useState(false)
  const set = k => e => setF({ ...f, [k]: e.target.value })
  const submit = async e => {
    e.preventDefault()
    setErr('')
    setBusy(true)
    try {
      if (reg) { await register(f); setVerification(true) }
      else { const current = await login(f.email, f.password); nav(homeFor(current)) }
    } catch (ex) { setErr(ex.message) } finally { setBusy(false) }
  }
  if (verification) return <div className="panel narrow"><h1>Account created</h1><p>Your account is saved by the API.</p><p>{user?.role === 'landlord' ? 'An administrator must verify your provider account before you can create listings.' : 'Email delivery is deferred in this POC. Your email status stays pending; browsing and messaging are available for the demo.'}</p><button className="btn" onClick={() => nav(homeFor(user))}>Continue</button></div>
  return (
    <div className="panel narrow"><h2>{reg ? 'Create account' : 'Log in'}</h2>
      <form onSubmit={submit}>
        {reg && <><label htmlFor="full-name">Full name</label><input id="full-name" required maxLength={120} value={f.name} onChange={set('name')} />
          <label htmlFor="role">I am a…</label><select id="role" value={f.role} onChange={set('role')}><option value="student">Student (CPUT email required)</option><option value="landlord">Landlord</option></select></>}
        {reg && f.role === 'student' && <><label htmlFor="student-number">Student number</label><input id="student-number" required value={f.studentNumber} onChange={set('studentNumber')} />
          <label htmlFor="campus">Campus</label><select id="campus" value={f.campus} onChange={set('campus')}>{['Bellville', 'Cape Town / District Six', 'Mowbray', 'Wellington', 'Athlone', 'Granger Bay'].map(campus => <option key={campus}>{campus}</option>)}</select>
          <label htmlFor="year">Year of study</label><select id="year" value={f.year} onChange={set('year')}>{[1, 2, 3, 4, 5, 6].map(year => <option key={year}>{year}</option>)}</select>
          <label htmlFor="funding">Funding type</label><select id="funding" value={f.funding} onChange={set('funding')}><option>NSFAS</option><option>Bursary</option><option>Self-funded</option></select></>}
        <label htmlFor="email">Email</label><input id="email" type="email" required value={f.email} onChange={set('email')} placeholder={reg && f.role === 'student' ? 'studentnumber' + STUDENT_DOMAIN : ''} />
        <label htmlFor="password">Password</label><input id="password" type="password" required minLength={reg ? 8 : undefined} value={f.password} onChange={set('password')} />
        {err && <p role="alert" className="err">{err}</p>}<button className="btn" disabled={busy} style={{ width: '100%' }}>{busy ? 'Please wait…' : reg ? 'Register' : 'Log in'}</button>
      </form>
      {!reg && <p className="muted">Local dev seed: 220001001@mycput.ac.za · verified-landlord@seed.local · admin@seed.local. Password: SeedDemo123! (dev profile only).</p>}
    </div>
  )
}
