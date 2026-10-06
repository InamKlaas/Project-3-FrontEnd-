/* Local mock API. Keep all persistence here so this module can later use fetch(). */
const KEY = 'cputhome_v2'
export const STUDENT_DOMAIN = '@mycput.ac.za'

const campusHomes = [
  ['Bellville', 'Anglo American'], ['Bellville', 'De Beers (East Wing)'], ['Bellville', 'De Goede Hoop'],
  ['Bellville', 'Freedom Square'], ['Bellville', 'New 200 Beds'], ['Bellville', 'Post Graduate Residence'],
  ['Bellville', 'Protea Hof Tower 4'], ['Bellville', 'Sacco'], ["Bellville", "Sheriff's House"],
  ['Bellville', 'Toplin House'], ['Bellville', 'Park Central'], ['Cape Town / District Six', 'Cape Suites'],
  ['Cape Town / District Six', 'Catsville (Groote Schuur)'], ['Cape Town / District Six', 'City Edge'],
  ["Cape Town / District Six", "Elizabeth Women's Residence (Gardens)"], ['Cape Town / District Six', 'J&B Residence (Zonnebloem)'],
  ['Cape Town / District Six', 'New Market Junction'], ['Cape Town / District Six', 'St Peters Block A'],
  ['Cape Town / District Six', 'Hanover Street'], ['Mowbray', 'Viljoenhof'], ['Wellington', 'House Bliss'],
  ['Wellington', 'House Meiring'], ['Wellington', 'Navarre'], ['Wellington', 'Greenoaks'], ['Granger Bay', 'WCCN Residences (Nico Malan)'],
  ['Bellville', 'Boston House'], ['Bellville', 'Canterbury House'], ['Bellville', 'Iona Residence'],
  ['Bellville', 'Link Road'], ['Bellville', 'Sunbell House'], ['Bellville', '4th Avenue Boston'], ['Bellville', 'Chad House'],
  ['Athlone', 'Aden Street'], ['Parow', 'King Edward'], ['Parow', 'Root Square']
]
const residenceArt = ['/images/residence-bellville.svg', '/images/residence-city.svg', '/images/residence-courtyard.svg', '/images/residence-room.svg', '/images/residence-lounge.svg']

const seededListings = campusHomes.map(([location, name], index) => ({
  id: index + 1, owner: 'landlord@demo.com', title: `${name} student residence`, price: 2600 + (index % 7) * 450,
  location, campus: location, type: ['Single', 'Sharing', 'Bachelor'][index % 3],
  gallery: [residenceArt[index % residenceArt.length], residenceArt[(index + 3) % residenceArt.length], residenceArt[(index + 1) % residenceArt.length]],
  image: residenceArt[index % residenceArt.length], address: `Sample area: ${location}. Exact street address not supplied; verify with CPUT and the provider.`,
  desc: `${name} is listed here as sample data for the CPUT Home student project. Verify all details with the provider and CPUT before making decisions.`,
  available: index % 4 !== 1, availableDate: index % 4 === 1 ? '2026-02-01' : '2026-10-01', emergency: index === 0 || index === 17,
  status: 'approved', sample: true, onCampus: index < 25, nsfas: index < 25, gender: 'Any',
  amenities: ['WiFi', ...(index % 2 ? ['Laundry'] : []), 'Security'], beds: 1 + index % 4,
  rent: 2600 + (index % 7) * 450, deposit: 1500, utilities: 350, houseRules: 'No smoking indoors. Quiet hours after 22:00. Visitors must sign in.', shuttle: 'Confirm shuttle availability with the provider.',
  ownerName: 'CPUT Home demo provider', reviews: []
}))

const seed = {
  session: null,
  users: [
    { email: 'admin@cputhome.co.za', password: 'admin123', role: 'admin', name: 'CPUT Home Admin', status: 'verified' },
    { email: 'landlord@demo.com', password: 'demo123', role: 'landlord', name: 'Demo Provider', status: 'verified', accreditation: true },
    { email: '220000001@mycput.ac.za', password: 'demo123', role: 'student', name: 'Demo Student', status: 'verified', studentNumber: '220000001', campus: 'Bellville', year: '3', funding: 'NSFAS' }
  ],
  listings: seededListings,
  messages: [], favs: {}, viewings: [], applications: [], leases: [], reports: [], notifications: [], announcements: [], audit: []
}

function clone(value) { return JSON.parse(JSON.stringify(value)) }
function dbGet() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY))
    if (!saved) return clone(seed)
    const listings = saved.listings?.length ? saved.listings.map(listing => {
      const sample = seed.listings.find(item => item.id === listing.id)
      if (!listing.sample || !sample) return listing
      return { ...sample, ...listing, image: listing.image || sample.image, gallery: listing.gallery?.length ? listing.gallery : sample.gallery, address: listing.address?.includes('Exact street address') ? listing.address : sample.address }
    }) : clone(seed.listings)
    return { ...clone(seed), ...saved, users: saved.users?.length ? saved.users : clone(seed.users), listings }
  } catch { return clone(seed) }
}
function dbSet(data) { localStorage.setItem(KEY, JSON.stringify(data)) }
const nextId = items => items.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) + 1
const safeUser = user => ({ email: user.email, role: user.role, name: user.name, status: user.status || 'verified', campus: user.campus })
const appendNotice = (data, email, text, kind = 'info') => data.notifications.push({ id: nextId(data.notifications), email, text, kind, read: false, at: Date.now() })

export const API = {
  me() { return dbGet().session },
  login(email, password) {
    const data = dbGet(); const user = data.users.find(item => item.email === email.trim().toLowerCase() && item.password === password)
    if (!user) throw new Error('Wrong email or password.')
    if (user.status === 'suspended') throw new Error('This account is suspended. Contact an administrator.')
    data.session = safeUser(user); dbSet(data); return data.session
  },
  logout() { const data = dbGet(); data.session = null; dbSet(data) },
  register(details) {
    const email = details.email.trim().toLowerCase(); const data = dbGet()
    if (details.role === 'student' && !email.endsWith(STUDENT_DOMAIN)) throw new Error(`Students must register with a CPUT email (${STUDENT_DOMAIN}).`)
    if (details.password.length < 6) throw new Error('Password must be at least 6 characters.')
    if (data.users.some(user => user.email === email)) throw new Error('Account already exists.')
    const user = { ...details, email, status: details.role === 'student' ? 'pending-email' : 'pending-verification', verifiedEmail: false }
    data.users.push(user); data.session = safeUser(user); dbSet(data); return data.session
  },
  confirmEmail(email) {
    const data = dbGet(); const user = data.users.find(item => item.email === email)
    if (!user) throw new Error('Account not found.')
    user.verifiedEmail = true; user.status = user.role === 'student' ? 'verified' : user.status
    if (data.session?.email === email) data.session = safeUser(user)
    dbSet(data); return data.session
  },
  listings() { return dbGet().listings },
  approved() { return this.listings().filter(item => item.status === 'approved' && item.available) },
  listing(id) { return this.listings().find(item => item.id === +id) },
  addListing(listing) {
    if (!listing.title || !listing.price || !listing.location || !listing.desc) throw new Error('Complete all required fields.')
    const data = dbGet(); data.listings.push({ ...listing, id: nextId(data.listings), status: 'pending', sample: false }); dbSet(data)
  },
  updateListing(id, patch) { const data = dbGet(); const listing = data.listings.find(item => item.id === +id); if (listing) Object.assign(listing, patch); dbSet(data) },
  removeListing(id) { const data = dbGet(); data.listings = data.listings.filter(item => item.id !== +id); dbSet(data) },
  send(listingId, student, from, text) { const data = dbGet(); const listing = data.listings.find(item => item.id === +listingId); data.messages.push({ id: nextId(data.messages), listingId: +listingId, student, from, text, ts: Date.now() }); appendNotice(data, from === student ? listing?.owner : student, `New message about ${listing?.title || 'a listing'}.`, 'message'); dbSet(data) },
  thread(listingId, student) { return dbGet().messages.filter(message => message.listingId === +listingId && message.student === student) },
  threadsFor(user) {
    const data = dbGet(); const seen = {}
    data.messages.forEach(message => {
      const listing = data.listings.find(item => item.id === message.listingId)
      if ((user.role === 'student' && message.student === user.email) || (user.role === 'landlord' && listing?.owner === user.email)) seen[message.listingId + '|' + message.student] = { listingId: message.listingId, student: message.student, title: listing?.title || '(removed)' }
    })
    return Object.values(seen)
  },
  favs(email) { return dbGet().favs?.[email] || [] },
  toggleFav(email, id) { const data = dbGet(); data.favs ??= {}; const ids = (data.favs[email] ??= []); const index = ids.indexOf(+id); index < 0 ? ids.push(+id) : ids.splice(index, 1); dbSet(data) },
  viewings() { return dbGet().viewings || [] },
  requestViewing(listingId, student, date) {
    if (!date) throw new Error('Pick a date and time first.')
    const data = dbGet(); const listing = data.listings.find(item => item.id === +listingId); data.viewings.push({ id: nextId(data.viewings), listingId: +listingId, student, date, status: 'pending' }); appendNotice(data, listing?.owner, `New viewing request for ${listing?.title || 'a listing'}.`, 'viewing'); dbSet(data)
  },
  setViewing(id, status) { const data = dbGet(); const request = data.viewings.find(item => item.id === +id); if (request) { request.status = status; appendNotice(data, request.student, `Your viewing request was ${status}.`, 'viewing') }; dbSet(data) },
  users() { return dbGet().users.map(({ password, ...user }) => user) },
  removeUser(email) { const data = dbGet(); data.users = data.users.filter(user => user.email !== email); data.listings = data.listings.filter(listing => listing.owner !== email); dbSet(data) },
  setUserStatus(email, status) { const data = dbGet(); const user = data.users.find(item => item.email === email); if (user) user.status = status; dbSet(data) },
  applications() { return dbGet().applications || [] },
  submitApplication(application) {
    const data = dbGet(); if (data.applications.some(item => item.student === application.student && item.listingId === +application.listingId && item.status !== 'declined')) throw new Error('You already have an application for this listing.')
    const listing = data.listings.find(item => item.id === +application.listingId)
    const record = { ...application, id: nextId(data.applications), listingId: +application.listingId, status: 'submitted', createdAt: Date.now() }; data.applications.push(record); appendNotice(data, listing?.owner, `A student applied for ${listing?.title || 'a listing'}.`, 'application'); dbSet(data); return record
  },
  setApplication(id, status) { const data = dbGet(); const item = data.applications.find(application => application.id === +id); if (item) { item.status = status; appendNotice(data, item.student, `Your accommodation application status is now ${status}.`, 'application') }; dbSet(data) },
  leases() { return (dbGet().leases || []).map(lease => new Date(`${lease.endDate}T23:59:59`) < new Date() ? { ...lease, status: 'expired' } : lease) },
  issueLease(lease) { const data = dbGet(); data.leases.push({ ...lease, id: nextId(data.leases), status: 'unsigned', createdAt: Date.now() }); appendNotice(data, lease.student, `A lease is ready for your signature: ${lease.room}.`, 'lease'); dbSet(data) },
  signLease(id, name, signature) { const data = dbGet(); const lease = data.leases.find(item => item.id === +id); if (!lease || new Date(`${lease.endDate}T23:59:59`) < new Date()) throw new Error('This lease has expired and cannot be signed.'); Object.assign(lease, { status: 'signed', signedName: name, signature, signedAt: Date.now() }); appendNotice(data, lease.owner, `${lease.tenant} signed the lease for ${lease.room}.`, 'lease'); dbSet(data) },
  reports() { return dbGet().reports || [] },
  reportListing(listingId, reporter, reason) { const data = dbGet(); data.reports.push({ id: nextId(data.reports), listingId: +listingId, reporter, reason, status: 'open', createdAt: Date.now() }); dbSet(data) },
  resolveReport(id, status) { const data = dbGet(); const report = data.reports.find(item => item.id === +id); if (report) report.status = status; dbSet(data) },
  onboardProvider(email, details) { const data = dbGet(); const user = data.users.find(item => item.email === email); if (user) Object.assign(user, details, { status: 'pending-verification' }); dbSet(data) },
  setAccreditation(email, value) { const data = dbGet(); const user = data.users.find(item => item.email === email); if (user) user.accreditation = value; dbSet(data) },
  notifications(email) { return (dbGet().notifications || []).filter(item => item.email === email || item.email === '*').sort((a, b) => b.at - a.at) },
  markNotificationsRead(email) { const data = dbGet(); data.notifications.forEach(item => { if (item.email === email || item.email === '*') item.read = true }); dbSet(data) },
  announce(text) { const data = dbGet(); data.announcements.push({ id: nextId(data.announcements), text, at: Date.now() }); data.notifications.push({ id: nextId(data.notifications), email: '*', text, kind: 'announcement', read: false, at: Date.now() }); dbSet(data) },
  audit(action, actor) { const data = dbGet(); data.audit.push({ id: nextId(data.audit), action, actor, at: Date.now() }); dbSet(data) },
  auditLog() { return dbGet().audit || [] },
  canReview(email, listingId) { return this.leases().some(lease => lease.student === email && lease.listingId === +listingId && Boolean(lease.signedAt)) },
  review(listingId, student, rating, text) { const data = dbGet(); const listing = data.listings.find(item => item.id === +listingId); if (!this.canReview(student, listingId)) throw new Error('A signed lease is required before reviewing.'); if (listing) { listing.reviews ??= []; if (listing.reviews.some(item => item.student === student)) throw new Error('You have already reviewed this listing.'); listing.reviews.push({ student, rating: +rating, text }) }; dbSet(data) },
  exportCSV(collection) {
    const rows = collection === 'applications' ? this.applications() : this.listings()
    if (!rows.length) return ''
    const headers = [...new Set(rows.flatMap(row => Object.keys(row)))]; const quote = value => `"${String(value ?? '').replaceAll('"', '""')}"`
    return [headers.map(quote).join(','), ...rows.map(row => headers.map(header => quote(typeof row[header] === 'object' ? JSON.stringify(row[header]) : row[header])).join(','))].join('\r\n')
  },
  exportMyData(email) { const data = dbGet(); const account = data.users.find(user => user.email === email); const { password, ...user } = account || {}; return { user, applications: data.applications.filter(item => item.student === email), leases: data.leases.filter(item => item.student === email), messages: data.messages.filter(item => item.student === email), viewings: data.viewings.filter(item => item.student === email) } },
  deleteMyData(email) { const data = dbGet(); data.users = data.users.filter(user => user.email !== email); data.applications = data.applications.filter(item => item.student !== email); data.leases = data.leases.filter(item => item.student !== email); data.messages = data.messages.filter(item => item.student !== email); data.viewings = data.viewings.filter(item => item.student !== email); data.notifications = data.notifications.filter(item => item.email !== email); data.reports = data.reports.filter(item => item.reporter !== email); delete data.favs[email]; data.session = null; dbSet(data) }
}