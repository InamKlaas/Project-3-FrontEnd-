import { test, expect } from '@playwright/test'

const api = process.env.POC_API_URL || 'http://localhost:8080/api'
const password = 'SeedDemo123!'

async function login(page, email, secret = password) {
  await page.goto('/login')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(secret)
  await page.getByRole('button', { name: 'Log in', exact: true }).click()
  await expect(page.locator('header')).toContainText(email === 'admin@seed.local' ? '(admin)' : '(landlord)')
}

async function register(page, email, role, number) {
  await page.goto('/register')
  await page.getByLabel('Full name').fill(`Synthetic browser ${role}`)
  await page.getByLabel('I am a…').selectOption(role)
  if (number) await page.getByLabel('Student number').fill(number)
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  const response = page.waitForResponse(response => response.url().endsWith('/api/auth/register') && response.request().method() === 'POST', { timeout: 45000 })
  await page.getByRole('button', { name: 'Register', exact: true }).click()
  const result = await response
  expect(result.status()).toBe(201)
  const body = await result.json()
  await expect(page.getByRole('heading', { name: 'Account created' })).toBeVisible()
  await page.getByRole('button', { name: 'Continue', exact: true }).click()
  return body.user.id
}

test.beforeAll(async ({ request }) => {
  const health = await request.get(api.replace(/\/api$/, '/actuator/health'))
  expect(health.ok(), 'Start the dev-profile backend with local MySQL before npm test').toBeTruthy()
  expect((await health.json()).status).toBe('UP')
  const browse = await request.get(`${api}/listings?size=1`)
  expect(browse.ok()).toBeTruthy()
})

test('guest search uses server filters, matching-room prices and limited previews', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('.card').first()).toBeVisible()
  await page.getByRole('link', { name: '🚨 Emergency', exact: true }).click()
  await page.getByLabel('Search title or area').fill('Sample House Bellville')
  await expect(page.locator('.card')).toHaveCount(1)
  await expect(page.locator('.card .price')).toHaveText('R2,900/month')
  await expect(page.locator('.card .badge.em')).toHaveText('Emergency')
  const preview = page.waitForResponse(response => /\/api\/listings\/\d+$/.test(response.url()))
  await page.getByRole('link', { name: 'Preview', exact: true }).click()
  const body = await (await preview).json()
  expect(body.address).toBeNull()
  expect(body.owner).toBeNull()
  expect(body.desc.length).toBeLessThanOrEqual(91)
  await expect(page.getByRole('heading', { name: 'Limited preview' })).toBeVisible()
  await expect(page.getByText('Address:', { exact: true })).toHaveCount(0)

  await page.getByRole('link', { name: 'Browse', exact: true }).click()
  const sorted = page.waitForResponse(response => response.url().includes('sort=price-low') && response.url().includes('/api/listings?'), { timeout: 45000 })
  await page.getByRole('combobox', { name: 'Sort', exact: true }).selectOption('price-low')
  expect((await sorted).status()).toBe(200)
  await expect(page.locator('.card').first()).toBeVisible()
  const prices = (await page.locator('.card .price').allTextContents()).map(value => Number(value.replace(/[^\d]/g, '')))
  expect(prices).toEqual([...prices].sort((a, b) => a - b))
  await page.getByLabel('Minimum rent').fill('6000')
  await page.getByLabel('Maximum rent').fill('2000')
  await expect(page.getByRole('alert')).toContainText('minimum rent cannot exceed maximum rent')
})

test('browser registration → provider verification → pending listing → approval → two-way chat → re-review', async ({ page, browser, request }) => {
  const suffix = Date.now().toString()
  const ownerEmail = `poc-browser-${suffix}@seed.local`
  const studentNumber = `9${suffix.slice(-8)}`
  const studentEmail = `${studentNumber}@mycput.ac.za`
  const title = `Synthetic POC browser ${suffix}`
  const adminContext = await browser.newContext()
  const studentContext = await browser.newContext()
  const guestContext = await browser.newContext()
  const admin = await adminContext.newPage()
  const student = await studentContext.newPage()
  const guest = await guestContext.newPage()
  const errors = []
  for (const current of [page, admin, student, guest]) {
    current.on('pageerror', error => errors.push(error.message))
    current.on('response', response => { if (response.url().includes('/api/') && response.status() >= 500) errors.push(`${response.status()} ${response.url()}`) })
  }
  let ownerId, studentId, adminToken
  try {
    ownerId = await register(page, ownerEmail, 'landlord')
    await expect(page.getByRole('heading', { name: 'My residences', exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'listings', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Submit for approval' })).toBeDisabled()

    await login(admin, 'admin@seed.local')
    adminToken = await admin.evaluate(() => localStorage.getItem('cputhome_token'))
    await admin.getByRole('button', { name: 'providers', exact: true }).click()
    const provider = admin.getByRole('row').filter({ hasText: ownerEmail })
    await provider.getByRole('button', { name: 'Verify', exact: true }).click()
    await expect(provider.getByRole('cell', { name: 'verified', exact: true })).toBeVisible()

    await page.getByRole('button', { name: 'overview', exact: true }).click()
    await page.getByRole('button', { name: 'Refresh verification' }).click()
    await expect(page.locator('.provider-banner .badge')).toHaveText('verified')
    await page.getByRole('button', { name: 'listings', exact: true }).click()
    await page.getByLabel('Listing name').fill(title)
    await page.getByLabel('Monthly rent (R)').fill('3200')
    await page.getByLabel('Street address').fill('Synthetic POC area only; not a real address')
    await page.getByLabel('Description', { exact: true }).fill('Synthetic browser test residence near Bellville campus. All details are invented POC data.')
    await page.getByLabel('Room type').selectOption('Sharing')
    await page.getByLabel('Available from').fill('2026-10-01')
    await page.getByLabel('Residence photo URLs').fill('/images/residence-bellville.svg\n/images/residence-room.svg')
    await page.getByLabel('Emergency, available immediately').check()
    const creation = page.waitForResponse(response => response.url().endsWith('/api/listings') && response.request().method() === 'POST')
    await page.getByRole('button', { name: 'Submit for approval' }).click()
    const created = await (await creation).json()
    expect(created.status).toBe('pending')
    expect(created.emergency).toBe(true)
    expect(created.availableDate).toBe('2026-10-01')
    const ownedRow = page.getByRole('row').filter({ hasText: title })
    await expect(ownedRow.getByText('pending', { exact: true })).toBeVisible()

    await guest.goto('/')
    await guest.getByLabel('Search title or area').fill(title)
    await expect(guest.getByText('0 listing(s) found', { exact: true })).toBeVisible()
    await admin.getByRole('button', { name: 'listings', exact: true }).click()
    await admin.getByRole('button', { name: 'Refresh', exact: true }).click()
    const moderationRow = admin.getByRole('row').filter({ hasText: title })
    await moderationRow.getByRole('button', { name: 'Approve', exact: true }).click()
    await expect(moderationRow.getByText('approved', { exact: true })).toBeVisible()
    await guest.reload()
    await guest.getByLabel('Search title or area').fill(title)
    await expect(guest.locator('.card')).toHaveCount(1)

    studentId = await register(student, studentEmail, 'student', studentNumber)
    await student.getByLabel('Search title or area').fill(title)
    await expect(student.locator('.card')).toHaveCount(1)
    await student.getByRole('link', { name: 'View details', exact: true }).click()
    await expect(student.getByRole('heading', { name: title, exact: true })).toBeVisible()
    await expect(student.getByText('Synthetic POC area only; not a real address', { exact: false })).toBeVisible()
    await expect(student.locator('input[type="file"]')).toHaveCount(0)
    const question = `Student request ${suffix}`
    const reply = `Landlord reply ${suffix}`
    await student.getByLabel('Message', { exact: true }).fill(question)
    await student.getByRole('button', { name: 'Send', exact: true }).click()
    await expect(student.locator('.chat')).toContainText(question)

    await page.getByRole('link', { name: 'Messages', exact: true }).click()
    await page.getByRole('link', { name: title, exact: true }).click()
    await expect(page.locator('.chat')).toContainText(question)
    await page.getByLabel('Reply', { exact: true }).fill(reply)
    await page.getByRole('button', { name: 'Send', exact: true }).click()
    await expect(page.locator('.chat')).toContainText(reply)
    await student.getByRole('button', { name: 'Refresh messages', exact: true }).click()
    await expect(student.locator('.chat')).toContainText(reply)
    await student.reload()
    await expect(student.locator('.chat')).toContainText(question)
    await expect(student.locator('.chat')).toContainText(reply)
    await expect(student.locator('header')).toContainText('(student)')

    await page.getByRole('link', { name: 'My residences', exact: true }).click()
    await page.getByRole('button', { name: 'listings', exact: true }).click()
    await ownedRow.getByRole('button', { name: 'Mark leased', exact: true }).click()
    await expect(ownedRow.getByRole('button', { name: 'Mark available', exact: true })).toBeVisible()
    const hidden = await request.get(`${api}/listings?search=${encodeURIComponent(title)}`)
    expect((await hidden.json()).totalElements).toBe(0)
    await ownedRow.getByRole('button', { name: 'Mark available', exact: true }).click()
    await expect(ownedRow.getByRole('button', { name: 'Mark leased', exact: true })).toBeVisible()
    page.once('dialog', dialog => dialog.accept('3500'))
    await ownedRow.getByRole('button', { name: 'Edit rent', exact: true }).click()
    await expect(ownedRow.getByText('pending', { exact: true })).toBeVisible()
    const review = await request.get(`${api}/listings?search=${encodeURIComponent(title)}`)
    expect((await review.json()).totalElements).toBe(0)
    expect(errors).toEqual([])
  } finally {
    if (adminToken) {
      for (const id of [ownerId, studentId].filter(Boolean)) {
        const removed = await request.delete(`${api}/admin/users/${id}`, { headers: { Authorization: `Bearer ${adminToken}` } })
        expect(removed.status(), `cleanup synthetic test user ${id}`).toBe(204)
      }
    }
    await Promise.all([adminContext.close(), studentContext.close(), guestContext.close()])
  }
})

test('student-domain validation and role/participant checks are enforced by the live API', async ({ page, request }) => {
  await page.goto('/register')
  await page.getByLabel('Full name').fill('Synthetic invalid student')
  await page.getByLabel('Student number').fill('999999901')
  await page.getByLabel('Email', { exact: true }).fill('invalid-student@seed.local')
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Register', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('@mycput.ac.za')

  await page.goto('/login')
  await page.getByLabel('Email', { exact: true }).fill('220001001@mycput.ac.za')
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Log in', exact: true }).click()
  await expect(page.locator('header')).toContainText('(student)')
  const jwt = await page.evaluate(() => localStorage.getItem('cputhome_token'))
  const headers = { Authorization: `Bearer ${jwt}` }
  expect((await request.get(`${api}/admin/users`, { headers })).status()).toBe(403)
  const listings = await request.get(`${api}/listings?search=Sample%20House%20Bellville`, { headers })
  const id = (await listings.json()).content[0].id
  expect((await request.get(`${api}/listings/${id}/messages?studentId=220001002%40mycput.ac.za`, { headers })).status()).toBe(404)
  await page.goto('/admin')
  await expect(page).toHaveURL(/\/login$/)
})

test('failed search is retryable, bad login is visible, and invalid tokens are cleared', async ({ page }) => {
  await page.route('**/api/listings?*', route => route.abort('failed'))
  await page.goto('/')
  await expect(page.getByRole('alert')).toContainText('Could not reach the API')
  await page.unroute('**/api/listings?*')
  await page.getByRole('button', { name: 'Retry', exact: true }).click()
  await expect(page.locator('.card').first()).toBeVisible()
  await page.goto('/login')
  await page.getByLabel('Email', { exact: true }).fill('220001001@mycput.ac.za')
  await page.getByLabel('Password').fill('wrong-password')
  await page.getByRole('button', { name: 'Log in', exact: true }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await page.evaluate(() => localStorage.setItem('cputhome_token', 'invalid-token'))
  await page.reload()
  await expect(page.getByRole('link', { name: 'Login', exact: true })).toBeVisible()
  expect(await page.evaluate(() => localStorage.getItem('cputhome_token'))).toBeNull()
})
