/* POC identity/listing/moderation/messaging flows use the Spring Boot API.
 * Saved IDs remain browser-only. Legacy local helpers are retained for
 * deferred work; the corresponding UI workflows show deferred panels.
 * HTTP methods return Promises and must be awaited. */
const KEY = 'cputhome_v2'
const TOKEN_KEY = 'cputhome_token'
const API_BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '')
export const STUDENT_DOMAIN = '@mycput.ac.za'

function token() { try { return localStorage.getItem(TOKEN_KEY) } catch { return null } }

async function req(path, { method = 'GET', body, token: explicit, signal, binary = false } = {}) {
  const jwt = explicit !== undefined ? explicit : token()
  const controller = new AbortController()
  const abort = () => controller.abort()
  if (signal?.aborted) abort()
  signal?.addEventListener('abort', abort)
  const timeout = setTimeout(abort, 30000)
  try {
    const res = await fetch(API_BASE + path, {
      method,
      headers: { ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }), ...(jwt ? { Authorization: `Bearer ${jwt}` } : {}) },
      body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body),
      signal: controller.signal,
    })
    if (binary && res.ok) return await res.blob()
    const text = await res.text()
    let data = null
    try { data = text ? JSON.parse(text) : null } catch { /* a proxy can return an HTML error */ }
    if (!res.ok) {
      if (res.status === 401 && jwt && token() === jwt) {
        localStorage.removeItem(TOKEN_KEY)
        window.dispatchEvent(new Event('cputhome:session-expired'))
      }
      const err = new Error(data?.message || `Request failed (${res.status})`)
      err.status = res.status; err.code = data?.code; err.fieldErrors = data?.fieldErrors
      throw err
    }
    return data
  } catch (error) {
    if (signal?.aborted) throw error
    if (error.name === 'AbortError') throw new Error('The API took too long to respond. Please retry.')
    if (error instanceof TypeError) throw new Error('Could not reach the API. Check that the backend is running.')
    throw error
  } finally {
    clearTimeout(timeout)
    signal?.removeEventListener('abort', abort)
  }
}

async function allPages(path, map) {
  const rows = []
  let page = 0
  let last = false
  while (!last) {
    const data = await req(`${path}?page=${page++}&size=50`)
    rows.push(...data.content.map(map))
    last = data.last
  }
  return rows
}

const toUser = user => user && ({
  id: user.id,
  email: user.email,
  role: user.role,
  name: user.fullName || user.name,
  status: user.status,
  campus: user.campus,
  studentNumber: user.studentNumber,
})

const toCard = row => row && ({
  id: row.id,
  owner: row.owner,
  ownerName: row.ownerName,
  title: row.title,
  price: row.price,
  rent: row.rent ?? row.price,
  location: row.location,
  campus: row.campus,
  type: row.type,
  available: row.available,
  availableDate: row.availableDate,
  emergency: row.emergency,
  status: row.status,
  onCampus: row.onCampus,
  nsfas: row.nsfas,
  gender: row.gender,
  amenities: row.amenities || [],
  beds: row.beds ?? 1,
  image: row.image,
  gallery: row.gallery || [],
  deposit: row.deposit ?? 0,
  utilities: row.utilities ?? 0,
  houseRules: row.houseRules,
  shuttle: row.shuttle,
  desc: row.desc,
  address: row.address,
  sample: !!row.sample,
  active: row.active,
  published: row.published,
  rejectionReason: row.rejectionReason,
  createdAt: row.createdAt,
  reviews: [],
});

const toMessage = row => row && ({
  id: row.id,
  listingId: row.listingId,
  student: row.student,
  from: row.from,
  text: row.text,
  ts: row.createdAt ? Date.parse(row.createdAt) : Date.now(),
})

/* ---------- synchronous local store: deferred demo-only flows ---------- */

const seed = {
  session: null,
  users: [],
  listings: [],
  messages: [], favs: {}, viewings: [], applications: [], leases: [], reports: [], notifications: [], announcements: [], audit: []
}

function clone(value) { return JSON.parse(JSON.stringify(value)) }
function dbGet() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY))
    if (!saved) return clone(seed)
    return { ...clone(seed), ...saved }
  } catch { return clone(seed) }
}
function dbSet(data) { localStorage.setItem(KEY, JSON.stringify(data)) }
const nextId = items => items.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) + 1
const appendNotice = (data, email, text, kind = 'info') => data.notifications.push({ id: nextId(data.notifications), email, text, kind, read: false, at: Date.now() })

export const API = {
  /* ---------- identity (HTTP) ---------- */
  async me(options) {
    const jwt = token()
    if (!jwt) return null
    try {
      return toUser(await req('/auth/me', options))
    } catch (error) {
      if (error.status === 401) return null
      throw error
    }
  },
  async login(email, password) {
    const data = await req('/auth/login', { method: 'POST', token: null, body: { identifier: email.trim(), password } })
    localStorage.setItem(TOKEN_KEY, data.token)
    return toUser(data.user)
  },
  async register(details) {
    const data = await req('/auth/register', {
      method: 'POST',
      token: null,
      body: {
        fullName: details.name,
        email: details.email,
        password: details.password,
        role: (details.role || 'student').toUpperCase(),
        studentNumber: details.studentNumber || undefined,
        campus: details.campus || undefined,
        year: details.year || undefined,
        funding: details.funding || undefined,
      },
    })
    localStorage.setItem(TOKEN_KEY, data.token)
    return toUser(data.user)
  },
  async logout() {
    const jwt = token()
    try { localStorage.removeItem(TOKEN_KEY) } catch { /* private mode */ }
    try { await req('/auth/logout', { method: 'POST', token: jwt }) } catch { /* stateless logout is local */ }
  },

  /* ---------- listings (HTTP) ---------- */
  async listings() {
    return allPages('/listings', toCard)
  },
  async approved() {
    return this.listings()
  },
  async search(filters = {}, options) {
    const params = new URLSearchParams()
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '' && value !== undefined && value !== null) params.set(key, String(value))
    })
    const page = await req(`/listings?${params}`, options)
    return { ...page, content: page.content.map(toCard) }
  },
  async providerListings(options) {
    return (await req('/providers/me/listings', options)).map(toCard)
  },
  async listing(id, options) {
    try {
      return toCard(await req(`/listings/${id}`, options))
    } catch (error) {
      if (error && error.status === 404) return undefined
      throw error
    }
  },
  async addListing(listing) {
    if (!listing.title || !listing.price || !listing.location || !listing.desc) throw new Error('Complete all required fields.')
    const created = await req('/listings', {
      method: 'POST',
      body: {
        title: listing.title,
        description: listing.desc,
        location: listing.location,
        campus: listing.campus || 'Bellville',
        address: listing.address,
        onCampus: !!listing.onCampus,
        nsfas: !!listing.nsfas,
        gender: listing.gender || 'Any',
        amenities: listing.amenities || [],
        imageUrls: listing.gallery && listing.gallery.length ? listing.gallery : (listing.image ? [listing.image] : []),
        houseRules: listing.houseRules,
        shuttle: listing.shuttle,
        utilities: listing.utilities || 0,
        rooms: [{
          roomType: listing.type || 'Single',
          monthlyRent: Number(listing.price),
          deposit: Number(listing.deposit) || 0,
          beds: Number(listing.beds) || 1,
          available: listing.available !== false,
          availableDate: listing.availableDate || undefined,
          emergency: !!listing.emergency,
        }],
      },
    })
    return toCard(created)
  },
  async updateListing(id, patch) {
    const next = { ...patch }
    if (next.desc !== undefined) { next.description = next.desc; delete next.desc }
    if (next.rent !== undefined && next.price === undefined) next.price = next.rent
    if (next.gallery !== undefined) next.imageUrls = next.gallery
    else if (next.image !== undefined) next.imageUrls = next.image ? [next.image] : []
    delete next.rent; delete next.gallery; delete next.image; delete next.owner; delete next.ownerName
    if (next.status === 'approved') {
      delete next.status
      const done = await req(`/admin/listings/${id}/approve`, { method: 'POST' })
      if (Object.keys(next).length) await req(`/listings/${id}`, { method: 'PATCH', body: next })
      return toCard(done)
    }
    if (next.status === 'rejected') {
      const done = await req(`/admin/listings/${id}/reject`, { method: 'POST', body: { reason: next.rejectionReason } })
      delete next.status; delete next.rejectionReason
      if (Object.keys(next).length) await req(`/listings/${id}`, { method: 'PATCH', body: next })
      return toCard(done)
    }
    delete next.status; delete next.rejectionReason
    if (next.available !== undefined && typeof next.available !== 'boolean') delete next.available
    if (next.price !== undefined) next.price = Number(next.price)
    return toCard(await req(`/listings/${id}`, { method: 'PATCH', body: next }))
  },
  async removeListing(id) {
    await req(`/listings/${id}`, { method: 'DELETE' })
  },

  /* ---------- messaging (HTTP) ---------- */
  async send(listingId, student, from, text) {
    return toMessage(await req(`/listings/${listingId}/messages`, { method: 'POST', body: { text, studentEmail: student } }))
  },
  async thread(listingId, student, options) {
    const rows = await req(`/listings/${listingId}/messages?studentId=${encodeURIComponent(student)}`, options)
    return (rows || []).map(toMessage)
  },
  async threadsFor(user, options) {
    const rows = await req('/conversations', options)
    return (rows || []).map(row => ({ listingId: row.listingId, student: row.student, title: row.title }))
  },

  /* ---------- admin (HTTP) ---------- */
  async users() {
    return allPages('/admin/users', row => ({
      email: row.email,
      role: row.role,
      name: row.fullName || row.name,
      status: row.enabled === false ? 'suspended' : (row.status || 'verified'),
      campus: row.campus,
      accreditation: row.accreditation,
      registrationNumber: row.registrationNumber,
      userId: row.id,
    }))
  },
  async setUserEnabled(id, enabled) {
    return req(`/admin/users/${id}/${enabled ? 'enable' : 'disable'}`, { method: 'POST' })
  },
  async verifyProvider(id, status) {
    return req(`/admin/providers/${id}/verification`, { method: 'PATCH', body: { status } })
  },
  async setUserStatus(email, status) {
    const rows = await this.users()
    const target = rows.find(item => item.email === email)
    if (!target) throw new Error('Account not found.')
    if (status === 'suspended') {
      await req(`/admin/users/${target.userId}/disable`, { method: 'POST' })
    } else if (status === 'verified' && target.role === 'landlord') {
      if (target.status === 'suspended') await this.setUserEnabled(target.userId, true)
      await req(`/admin/providers/${target.userId}/verification`, { method: 'PATCH', body: { status: 'VERIFIED' } })
    } else if (status === 'verified') {
      await req(`/admin/users/${target.userId}/enable`, { method: 'POST' })
    } else if (status === 'rejected') {
      await req(`/admin/providers/${target.userId}/verification`, { method: 'PATCH', body: { status: 'REJECTED' } })
    } else {
      throw new Error(`Unsupported account state: ${status}`)
    }
  },
  async removeUser(email) {
    const rows = await this.users()
    const target = rows.find(item => item.email === email)
    if (!target) throw new Error('Account not found.')
    await req(`/admin/users/${target.userId}`, { method: 'DELETE' })
  },
  async setAccreditation(email, value) {
    const providers = await this.adminProviders()
    const target = providers.find(item => item.email === email)
    if (!target) throw new Error('Account not found.')
    await req(`/admin/providers/${target.userId}/accreditation`, { method: 'PATCH', body: { accredited: !!value } })
  },
  async adminProviders(status) {
    const params = status ? `?status=${encodeURIComponent(status)}` : ''
    const rows = await req(`/admin/providers${params}`)
    return (rows || []).map(row => ({
      email: row.email,
      name: row.fullName || row.name,
      status: row.verificationStatus === 'verified' ? 'verified' : (row.verificationStatus === 'rejected' ? 'rejected' : 'pending-verification'),
      accreditation: !!row.accreditation,
      registrationNumber: row.registrationNumber,
      userId: row.userId,
    }))
  },
  async adminListings(status) {
    const params = status ? `?status=${encodeURIComponent(status)}` : ''
    const rows = await req(`/admin/listings${params}`)
    return (rows || []).map(toCard)
  },

  /* ---------- demo-only local store (unchanged behavior) ---------- */
  favs(email) { return [...new Set((dbGet().favs?.[email] || []).map(Number).filter(Number.isFinite))] },
  toggleFav(email, id) { const data = dbGet(); data.favs ??= {}; const ids = this.favs(email); const index = ids.indexOf(+id); index < 0 ? ids.push(+id) : ids.splice(index, 1); data.favs[email] = ids; dbSet(data) },
  viewings() { return dbGet().viewings || [] },
  requestViewing(listingId, student, date) {
    if (!date) throw new Error('Pick a date and time first.')
    const data = dbGet(); const listing = data.listings.find(item => item.id === +listingId); data.viewings.push({ id: nextId(data.viewings), listingId: +listingId, student, date, status: 'pending' }); appendNotice(data, listing?.owner, `New viewing request for ${listing?.title || 'a listing'}.`, 'viewing'); dbSet(data)
  },
  setViewing(id, status) { const data = dbGet(); const request = data.viewings.find(item => item.id === +id); if (request) { request.status = status; appendNotice(data, request.student, `Your viewing request was ${status}.`, 'viewing') }; dbSet(data) },
  applications(options) { return req('/applications', options) },
  submitApplication(application) { return req(`/listings/${application.listingId}/applications`, { method: 'POST', body: { note: application.note, moveIn: application.moveIn } }) },
  setApplication(id, status) { return req(`/applications/${id}/status`, { method: 'POST', body: { status } }) },
  uploadDocument(id, file) { const body = new FormData(); body.append('file', file); return req(`/applications/${id}/documents`, { method: 'POST', body }) },
  async downloadDocument(document) {
    const blob = await req(document.url, { binary: true }); const url = URL.createObjectURL(blob)
    const a = window.document.createElement('a'); a.href = url; a.download = document.name; a.click(); URL.revokeObjectURL(url)
  },
  leases() { return (dbGet().leases || []).map(lease => new Date(`${lease.endDate}T23:59:59`) < new Date() ? { ...lease, status: 'expired' } : lease) },
  issueLease(lease) { const data = dbGet(); data.leases.push({ ...lease, id: nextId(data.leases), status: 'unsigned', createdAt: Date.now() }); appendNotice(data, lease.student, `A lease is ready for your signature: ${lease.room}.`, 'lease'); dbSet(data) },
  signLease(id, name, signature) { const data = dbGet(); const lease = data.leases.find(item => item.id === +id); if (!lease || new Date(`${lease.endDate}T23:59:59`) < new Date()) throw new Error('This lease has expired and cannot be signed.'); Object.assign(lease, { status: 'signed', signedName: name, signature, signedAt: Date.now() }); appendNotice(data, lease.owner, `${lease.tenant} signed the lease for ${lease.room}.`, 'lease'); dbSet(data) },
  reports(options) { return req('/reports', options) },
  reportListing(listingId, reporter, reason) { return req(`/listings/${listingId}/reports`, { method: 'POST', body: { note: reason } }) },
  resolveReport(id, status) { return req(`/reports/${id}/status`, { method: 'POST', body: { status } }) },
  onboardProvider(email, details) { const data = dbGet(); const user = data.users.find(item => item.email === email); if (user) Object.assign(user, details, { status: 'pending-verification' }); dbSet(data) },
  notifications(email) { return (dbGet().notifications || []).filter(item => item.email === email || item.email === '*').sort((a, b) => b.at - a.at) },
  markNotificationsRead(email) { const data = dbGet(); data.notifications.forEach(item => { if (item.email === email || item.email === '*') item.read = true }); dbSet(data) },
  announce(text) { const data = dbGet(); data.announcements.push({ id: nextId(data.announcements), text, at: Date.now() }); data.notifications.push({ id: nextId(data.notifications), email: '*', text, kind: 'announcement', read: false, at: Date.now() }); dbSet(data) },
  audit(action, actor) { const data = dbGet(); data.audit.push({ id: nextId(data.audit), action, actor, at: Date.now() }); dbSet(data) },
  auditLog() { return dbGet().audit || [] },
  reviews(listingId, options) { return req(`/listings/${listingId}/reviews`, options) },
  review(listingId, student, rating, text) { return req(`/listings/${listingId}/reviews`, { method: 'POST', body: { rating: +rating, note: text } }) },
  async exportCSV(collection) {
    const rows = collection === 'applications' ? await this.applications() : await this.adminListings()
    if (!rows.length) return ''
    const headers = [...new Set(rows.flatMap(row => Object.keys(row)))]; const quote = value => `"${String(value ?? '').replaceAll('"', '""')}"`
    return [headers.map(quote).join(','), ...rows.map(row => headers.map(header => quote(typeof row[header] === 'object' ? JSON.stringify(row[header]) : row[header])).join(','))].join('\r\n')
  },
  exportMyData(email) { const data = dbGet(); const account = data.users.find(user => user.email === email); const { password, ...user } = account || {}; return { user, applications: data.applications.filter(item => item.student === email), leases: data.leases.filter(item => item.student === email), messages: data.messages.filter(item => item.student === email), viewings: data.viewings.filter(item => item.student === email) } },
  deleteMyData(email) { const data = dbGet(); data.users = data.users.filter(user => user.email !== email); data.applications = data.applications.filter(item => item.student !== email); data.leases = data.leases.filter(item => item.student !== email); data.messages = data.messages.filter(item => item.student !== email); data.viewings = data.viewings.filter(item => item.student !== email); data.notifications = data.notifications.filter(item => item.email !== email); data.reports = data.reports.filter(item => item.reporter !== email); delete data.favs[email]; data.session = null; dbSet(data) },
}
