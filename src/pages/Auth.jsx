import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth, homeFor } from '../context/AuthContext.jsx'
import { API, STUDENT_DOMAIN } from '../api.js'

export default function Auth({ mode }) {
  const reg = mode === 'register'; const { login, register } = useAuth(); const nav = useNavigate()
  const [f, setF] = useState({ name: '', email: '', password: '', role: 'student', studentNumber: '', campus: 'Bellville', year: '1', funding: 'NSFAS' }); const [err, setErr] = useState('')
  const [verification, setVerification] = useState(false)
  const set = k => e => setF({ ...f, [k]: e.target.value })
  const submit = e => {
    e.preventDefault()
    try {
      if (reg) { register(f); setVerification(true) }
      else { login(f.email, f.password); nav(homeFor(API.me())) }
    } catch (ex) { setErr(ex.message) }
  }
  const confirm = () => { API.confirmEmail(f.email.toLowerCase()); nav(homeFor(API.me())) }
  if (verification) return <div className="panel narrow"><h1>Confirm your email</h1><p>A simulated confirmation message has been sent to <strong>{f.email}</strong>.</p><p>For this local demo, confirm below to continue.</p><button className="btn" onClick={confirm}>Confirm email and continue</button></div>
  return (
    <div className="panel narrow"><h2>{reg ? 'Create account' : 'Log in'}</h2>
      <form onSubmit={submit}>
        {reg && <><label>Full name</label><input required value={f.name} onChange={set('name')} />
          <label>I am a…</label><select value={f.role} onChange={set('role')}><option value="student">Student (CPUT email required)</option><option value="landlord">Landlord</option></select></>}
        {reg && f.role === 'student' && <><label>Student number</label><input required value={f.studentNumber} onChange={set('studentNumber')} />
          <label>Campus</label><select value={f.campus} onChange={set('campus')}>{['Bellville', 'Cape Town / District Six', 'Mowbray', 'Wellington', 'Athlone', 'Granger Bay'].map(campus => <option key={campus}>{campus}</option>)}</select>
          <label>Year of study</label><select value={f.year} onChange={set('year')}>{[1, 2, 3, 4, 5, 6].map(year => <option key={year}>{year}</option>)}</select>
          <label>Funding type</label><select value={f.funding} onChange={set('funding')}><option>NSFAS</option><option>Bursary</option><option>Self-funded</option></select></>}
        <label>Email</label><input type="email" required value={f.email} onChange={set('email')} placeholder={reg && f.role === 'student' ? 'studentnumber' + STUDENT_DOMAIN : ''} />
        <label>Password</label><input type="password" required value={f.password} onChange={set('password')} />
        <p className="err">{err}</p><button className="btn" style={{ width: '100%' }}>{reg ? 'Register' : 'Log in'}</button>
      </form>
      {!reg && <p className="muted">Demo: 220000001@mycput.ac.za / demo123 · landlord@demo.com / demo123 · admin@cputhome.co.za / admin123</p>}
    </div>
  )
}
