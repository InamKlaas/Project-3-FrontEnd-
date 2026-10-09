# Frontend integration guide — wiring `src/api.js` to the live backend

> Backend availability does **not** equal frontend integration. Every method
> below is synchronous localStorage today and must become `async` HTTP in a
> future frontend-owned PR. This file maps each method; it changes no React.

## Transport rules

- Base URL from env (Vite), e.g. `VITE_API_URL=http://localhost:8080/api`.
  Note the prefix is `/api` with no version segment.
- Bearer header: `Authorization: Bearer <jwt>` from login/register answers.
  Tokens carry `{sub: userId, email, role}`; expiry 24h dev.
- Error shape everywhere: `{timestamp, status, code, message, fieldErrors,
  path}`. Map `code` to the existing UI messages (e.g. `LANDLORD_NOT_VERIFIED`).
- Roles on the wire are lowercase (`student`, `landlord`, `admin`); user
  statuses too (`verified`, `pending-email`, `pending-verification`,
  `suspended`, `rejected`); listing status (`pending`, `approved`,
  `rejected`); room types arrive as UI words (`Single`, `Sharing`,
  `Bachelor`).
- Timestamps arrive ISO-8601 strings. Mock uses epoch millis and `+id`
  coercion — backend ids are numeric `Long`, so `+id` keeps working.
- Money arrives as JSON numbers with 2 decimals (backend `BigDecimal`).

## Method map (POC scope only)

| Mock method | HTTP | Notes |
| --- | --- | --- |
| `login(email, password)` | `POST /api/auth/login {identifier, password}` → `{token, user}` | identifier accepts email or student number |
| `register(details)` | `POST /api/auth/register` → 201 `{token, user}` | students need `@mycput.ac.za` + studentNumber; landlords start pending |
| `me()` | `GET /api/auth/me` | from the token, never a param |
| `logout()` | `POST /api/auth/logout` → 204 | stateless; client drops the token |
| `approved()` | `GET /api/listings` (no params) | already approved+active+available; add Home.jsx filters as query params |
| `listings()` | `GET /api/listings?size=50` + provider feed | admin/desk views need `GET /api/providers/me/listings` (auth) |
| `listing(id)` | `GET /api/listings/{id}` | guests get truncated desc + no address; owners/admins full |
| `addListing` | `POST /api/listings` → 201, `status: pending` | owner from token; requires verified landlord or 403 |
| `updateListing` | `PATCH /api/listings/{id}` (allowlist fields; `active`, `price`, `availableDate`) | material edits re-open review |
| `removeListing` | `DELETE /api/listings/{id}` → 204 | deactivates, never hard-deletes |
| `send(listingId, student, from, text)` | `POST /api/listings/{id}/messages {text, studentEmail?}` | `from` dies — sender is the token; landlords pass `studentEmail` to reply |
| `thread(listingId, student)` | `GET /api/listings/{id}/messages?studentId=` | participant-only |
| `threadsFor(user)` | `GET /api/conversations` | role-scoped inbox summaries |
| admin approve/reject | `POST /api/admin/listings/{id}/approve`, `.../reject {reason?}` | admin token only |
| provider queue/verify | `GET /api/admin/providers?status=pending`, `PATCH .../verification {status}` | admin token only |
| `setUserStatus` | `POST /api/admin/users/{id}/disable`, `.../enable` → 204 | admin token only |

## Async conversion pattern (per screen)

Today screens call `API.x()` inline during render and mutate-then-reread
with a `tick()` counter. The HTTP version of each screen needs:

1. `useState` for data + loading/error, `useEffect` fetch on mount/param
   change, and the existing retry/empty UI states (they already exist).
2. Mutations become `async` handlers that `await` the call, surface
   `error.code` through the existing `note`/banner slots, then refetch.
3. Auth context stores `{token, user}` (sessionStorage), sends the bearer
   header, and clears on 401 exactly like the axios pattern in most apps.

Screens rendering local data without async state today: `Home`
(filter memo), `Listing` (detail + messages + viewings + applications +
reviews), `Dashboard` (tabs + tables), `Admin` (all tabs), `Messages`,
`MyStuff`, `Notifications`. Each needs the load/error/retry treatment
above; none of it is backend work.

## Deliberately demo-only (no endpoints, by design)

Favourites, viewings, applications + documents, leases/signatures, reviews,
reports moderation, announcements/audit UI, analytics, CSV exports, privacy
export/delete, notifications feed. Do not invent stub endpoints that return
fake success for these.
