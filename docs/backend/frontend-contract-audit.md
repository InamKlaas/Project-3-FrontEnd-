# Frontend contract audit — CPUT Home POC (stage 0)

Source: `InamKlaas/Project-3-FrontEnd-` @ `40b5801` (main, 2026-10-06).
All calls below are **synchronous localStorage** (`cputhome_v2`). A live
HTTP backend needs async adaptation in a future frontend PR — backend
availability does not equal frontend integration.

## Identity and roles

- Roles are lowercase strings: `student` | `landlord` | `admin`.
- Account statuses: `verified`, `pending-email`, `pending-verification`,
  `suspended`, `rejected`.
- `API.me()` returns the session user or null; safe shape is
  `{email, role, name, status, campus}` — password never leaves the store.
- `API.login(email, password)` — exact email match (lowercased), plaintext
  compare in mock; suspendeds get `This account is suspended.`
- `API.register({name, email, password, role, studentNumber, campus, year,
  funding})` — students must end with `@mycput.ac.za`, password min 6,
  duplicate email rejected. Students start `pending-email`, landlords start
  `pending-verification`.
- `API.confirmEmail(email)` — flips `verifiedEmail`, students become
  `verified`. Simulation only; never expose an email-only approval endpoint.

## Listing card shape (flat, as rendered)

`id` numeric. `owner` (email), `ownerName`, `title`, `price` = `rent`
(number, ZAR), `location`, `campus`, `type` (`Single`|`Sharing`|`Bachelor`),
`available` bool, `availableDate` (YYYY-MM-DD), `emergency` bool,
`status` (`pending`|`approved`|`rejected`), `onCampus` bool, `nsfas` bool
(sample-claim only), `gender`, `amenities[]`, `beds`, `image`, `gallery[]`,
`deposit`, `utilities`, `houseRules`, `shuttle`, `desc`, `address`,
`sample` bool, `reviews[]` (`{student, rating, text}`).

## Listing operations used by the UI

- `listings()`, `approved()` (status approved AND available),
  `listing(id)` (numeric coercion with `+id`).
- `addListing` requires title/price/location/desc; always creates
  `status: 'pending'`, `sample: false`; owner comes from the session.
- `updateListing(id, patch)` — arbitrary merge, no ownership check.
- `removeListing(id)` — hard delete.
- Home filters (all client-side): text over title+location, campus, room
  type, min/max rent, available-by date (`available && availableDate <= d`),
  emergency flag, onCampus string compare, nsfas string compare, gender,
  amenity includes; sorts `priority` (emergency first), `price-low`,
  `price-high`, `newest` (id desc).
- Detail visibility rule: approved, or owner/admin sees anything; guests get
  a truncated description and no address/cost/amenities/rules/provider.

## Conversations

- Thread identity is `(listingId numeric, student email)`.
- `send(listingId, student, from, text)` notifies the other side.
- `thread(listingId, student)`, `threadsFor(user)` — students see their own
  threads; landlords see threads on their listings (`listing.owner`).
- Message shape: `{id, listingId, student, from, text, ts}` (epoch millis).

## Admin surface used by Admin.jsx

- `users()` (password stripped), `setUserStatus(email, status)`,
  `removeUser(email)` (also removes their listings), `setAccreditation`.
- Listing approve = `updateListing(id, {status:'approved'})`; reject adds
  `status:'rejected'` + `rejectionReason` (prompted).
- `reports()` / `resolveReport(id, status)`; `announce(text)` fans out to
  `notifications`; `audit(action, actor)` / `auditLog()`; `exportCSV`.
- Analytics read `users`, `listings`, `applications` only.

## Deferred UI (no backend in POC)

Favourites, viewings, applications, leases/signatures, notifications feed,
reviews (lease-gated in mock), reports UI actions beyond resolve, privacy
export/delete, announcements feed. These stay demo-only; no stub endpoints
that falsely return success.

## Timestamps, money, ids

- Mock timestamps are epoch millis (`Date.now()`); dates are strings
  (`YYYY-MM-DD`, datetime-local inputs). Backend uses instants; DTOs carry
  ISO-8601 strings.
- Money is plain numbers in ZAR. Backend stores `BigDecimal`/DECIMAL.
- Mock ids are numeric auto-increment. Backend POC uses numeric `Long`
  identities to match (`+id` coercion keeps working).
