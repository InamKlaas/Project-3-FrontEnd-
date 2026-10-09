import { test, expect } from '@playwright/test'

const api = 'http://localhost:8080/api'
async function student(page, request) {
  const number = `8${Date.now().toString().slice(-8)}${Math.floor(Math.random() * 100)}`
  const email = `${number}@mycput.ac.za`
  const response = await request.post(`${api}/auth/register`, { data: { fullName: 'Integration Student', email, password: 'Integration123!', role: 'STUDENT', studentNumber: number, campus: 'Cape Town / District Six' } })
  expect(response.status()).toBe(201)
  const data = await response.json()
  await page.goto('/login')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill('Integration123!')
  await page.getByRole('button', { name: 'Log in', exact: true }).click()
  await expect(page.locator('header')).toContainText('(student)')
  return data.user.id
}
async function cleanup(request, id) {
  if (!id) return
  const login = await request.post(`${api}/auth/login`, { data: { identifier: 'admin@seed.local', password: 'SeedDemo123!' } })
  const { token } = await login.json()
  const response = await request.delete(`${api}/admin/users/${id}`, { headers: { Authorization: `Bearer ${token}` } })
  expect(response.ok()).toBeTruthy()
}
test('light/dark mode survives reload even when the OS prefers dark', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'dark' }); await page.goto('/')
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(244, 247, 244)')
  await page.getByRole('button', { name: 'Switch to dark mode' }).click()
  await page.reload(); await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('button', { name: 'Switch to light mode' }).click()
  await page.reload(); await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
})
test('liking and unliking survives refresh and updates My housing', async ({ page, request }) => {
  let id
  try {
    id = await student(page, request); await page.getByRole('link', { name: 'Browse', exact: true }).click()
    await page.getByLabel('Search title or area').fill('Hanover Residence')
    const card = page.locator('.card').filter({ hasText: 'Hanover Residence' }); await expect(card).toHaveCount(1)
    await card.getByRole('button', { name: 'Save listing', exact: true }).click()
    await expect(card.getByRole('button', { name: 'Remove saved listing' })).toHaveAttribute('aria-pressed', 'true')
    await page.reload(); await page.getByLabel('Search title or area').fill('Hanover Residence')
    await expect(card.getByRole('button', { name: 'Remove saved listing' })).toBeVisible()
    await page.getByRole('link', { name: 'My housing' }).click()
    await expect(page.locator('.card').filter({ hasText: 'Hanover Residence' })).toHaveCount(1)
    await page.getByRole('button', { name: 'Remove saved listing' }).click(); await page.reload()
    await expect(page.getByText('No saved listings yet.')).toBeVisible()
  } finally { await cleanup(request, id) }
})
test('real photos, application/upload, concern and review persist through the UI', async ({ page, request }) => {
  let id
  try {
    id = await student(page, request); await page.getByRole('link', { name: 'Browse', exact: true }).click()
    await page.getByLabel('Search title or area').fill('Hanover Residence')
    await page.locator('.card').getByRole('link', { name: 'View details' }).click()
    await expect(page.locator('.gallery-thumb')).toHaveCount(3)
    await expect.poll(() => page.locator('.photo-placeholder img').evaluate(image => image.complete && image.naturalWidth > 0)).toBeTruthy()
    await page.getByRole('button', { name: 'Show residence image 2' }).click()
    await expect(page.getByRole('button', { name: 'Show residence image 2' })).toHaveAttribute('aria-pressed', 'true')
    const date = new Date(); date.setDate(date.getDate() + 7)
    await page.getByLabel('Preferred move-in date').fill(date.toISOString().slice(0, 10))
    await page.getByLabel('Application message').fill('Integration test accommodation request')
    await page.getByLabel('Supporting documents').setInputFiles({ name: 'proof.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.4\nIntegration document') })
    await page.getByRole('button', { name: 'Submit application' }).click()
    await expect(page.getByRole('status')).toContainText('Application submitted')
    await page.reload(); await expect(page.getByRole('button', { name: 'proof.pdf', exact: true })).toBeVisible()
    await page.getByLabel('Concern details').fill('Integration test concern about the listing')
    await page.getByRole('button', { name: 'Send concern' }).click()
    await expect(page.getByRole('status')).toContainText('Concern sent')
    await page.getByLabel('Rating', { exact: true }).selectOption('4')
    await page.getByLabel('Your review').fill('Integration review: helpful residence information')
    await page.getByRole('button', { name: 'Publish review' }).click()
    await expect(page.locator('.review')).toContainText('4/5')
    await page.reload(); await expect(page.locator('.review')).toContainText('Integration review')
    await expect(page.getByText(/Your report #\d+ is/)).toContainText('open')
    await page.getByRole('link', { name: 'My housing' }).click()
    await expect(page.locator('.application-row')).toContainText('Hanover Residence')
  } finally { await cleanup(request, id) }
})
