# Frontend integration — wired stage-7 POC

The project owner authorized frontend HTTP wiring through stage 7. `src/api.js` and the POC screens now use asynchronous HTTP, retaining the existing routes, CSS and SVG assets.

## Transport

- Base URL: `VITE_API_URL`, default `http://localhost:8080/api`; 30-second request timeout and abortable reads.
- Bearer JWT from login/register; stored under `cputhome_token` in localStorage. `/auth/me` restores identity on reload; 401 clears the matching token and session. This is stateless authentication, not a server session. Default dev expiry: 24 hours.
- Logout removes the local token immediately and calls `/auth/logout`; previously issued JWTs remain valid until expiry unless the account is disabled/removed.
- Errors: `{timestamp,status,code,message,fieldErrors,path}`. UI shows failures rather than claiming a mutation succeeded. Network failures are retryable.
- IDs are numeric `Long`; money is a JSON number backed by Java `BigDecimal`; dates/timestamps use ISO strings.
- Roles/statuses are lowercase on the wire. User `fullName` maps to UI `name`. Listing `desc`, `type`, `price`, `owner`, `image` and `gallery` already follow the flat UI projection.

## Actual method map

| Adapter | HTTP | Notes |
| --- | --- | --- |
| `login`, `register`, `me`, `logout` | `/api/auth/*` | Login body `{identifier,password}`; register maps UI name to `fullName`; no fake email confirmation |
| `search(filters)` | `GET /api/listings` | Home sends all filters, page and size; cap 50, UI page size 12 |
| `listings`, `approved` | `GET /api/listings` | Collect all public pages; never a provider/admin feed |
| `providerListings` | `GET /api/providers/me/listings` | Landlord only, including pending/inactive rows |
| `listing(id)` | `GET /api/listings/{id}` | Guest previews omit address/contact and full cost/rules; owner/admin can read private drafts |
| `addListing` | `POST /api/listings` | Flat form maps to property + RoomSpec; includes availability/date/emergency; owner inferred from JWT |
| `updateListing` | `PATCH /api/listings/{id}` | Description/image aliases normalized; price/date edits return approved listings to pending |
| approved/rejected status patch | `POST /api/admin/listings/{id}/approve` or `/reject` | Admin-only command; status is not an owner PATCH field |
| `removeListing` | `DELETE /api/listings/{id}` | Soft deactivation; PATCH `{active:true}` reactivates |
| `send` | `POST /api/listings/{id}/messages` | `{text,studentEmail}`; `from` is ignored, sender from JWT; landlord reply carries student email |
| `thread` | `GET /api/listings/{id}/messages?studentId=<email>` | Legacy parameter name means email, not numeric ID; participant-authorized |
| `threadsFor` | `GET /api/conversations` | JWT-scoped inbox; Refresh fetches new messages |
| `adminListings` | `GET /api/admin/listings` | All statuses; optional pending/approved/rejected filter |
| `users` | `GET /api/admin/users` | Paged safe account summaries; adapter collects all pages |
| `verifyProvider` | `PATCH /api/admin/providers/{id}/verification` | `{status:"VERIFIED"}` or `"REJECTED"` |
| `setAccreditation` | `PATCH /api/admin/providers/{id}/accreditation` | Required Boolean; demo claim independent of verification |
| `setUserEnabled` | `POST /api/admin/users/{id}/enable` or `/disable` | 200 updated summary, not 204 |
| `removeUser` | `DELETE /api/admin/users/{id}` | Removes owned inventory/threads/profiles; self/admin removal forbidden |
| `exportCSV('listings')` | Uses the live admin listing feed | Client-generated CSV, not a server export endpoint |

## Projection and search semantics

`Accommodation` remains a property with multiple `RoomListing` children and a subtype. An unfiltered card uses the cheapest available room, or the cheapest overall for a provider's fully-booked property. A filtered public card uses the cheapest room that passes **all** room-level filters. Price sorting uses that same room set. The emergency badge means at least one matching available room offers emergency accommodation.

Public visibility requires admin publication, landlord activity, approved status, an enabled owner and an available room. Guests receive limited DTOs from both search and detail. Availability dates missing from a room are treated as unspecified/immediate for `availableBy`.

## Screen handling

`AuthContext` handles boot loading, failed session restoration, retry and expiry. `Home` debounces and cancels filtered reads. `useAsyncResource` protects route/user changes from stale responses in Listing, Dashboard, Admin, Messages and MyStuff. Mutations are awaited before refetching; busy actions are disabled.

## Deferred boundaries

Browser-only saved IDs still work with live detail reads. Legacy local helper methods are retained in the adapter, but viewing/application/document/lease/review/report/onboarding/notification/announcement workflows are replaced by explicit deferred panels. No UI action reports a local mock mutation as server success. Privacy controls only export/clear browser demo data, not an API account. Full audit reporting is deferred; core moderation decisions are persisted in `moderation_decisions`.
