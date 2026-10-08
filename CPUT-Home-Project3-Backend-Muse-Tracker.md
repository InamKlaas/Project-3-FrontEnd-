# CPUT Home — Project 3 Backend POC Implementation Tracker

> **For:** Muse Spark / coding agent working on PRT362S, Group IT1.
>
> **Deliverable:** A functioning **Java/Spring Boot + MySQL backend**, a reproducible **seed dataset**, and a **small Azure Linux VM deployment**. **No frontend rewrite.**
>
> **Source of truth for the existing UI:** [`InamKlaas/Project-3-FrontEnd-`](https://github.com/InamKlaas/Project-3-FrontEnd-) — branch `main`, particularly [`src/api.js`](https://github.com/InamKlaas/Project-3-FrontEnd-/blob/main/src/api.js), [`src/App.jsx`](https://github.com/InamKlaas/Project-3-FrontEnd-/blob/main/src/App.jsx), and [`README.md`](https://github.com/InamKlaas/Project-3-FrontEnd-/blob/main/README.md).
>
> **Academic context:** `PROJECT 3.docx` (team requirements), `T1 - IND.docx` (architecture/planning), `TERM 2 -IND.docx` (reported existing Java data layer). The original `varsity-connect-muse-implementation-tracker(1).md` supplies **tracking format only**. Its marketplace/cart/payment/lost-and-found modules belong to a different project and **must not be copied**.
>
> **Snapshot researched:** 8 October 2026. Re-check the repo before editing; the team may have pushed new code.
>
> **POC STOP LINE:** Complete **Stages 0–8**, then stop. Stages 9–10 are explicitly **deferred** and are **not** a prerequisite for submission.

---

## How Muse must use this file

1. Work **one stage at a time**, in order. Inspect existing code **before** proposing modifications. Do not mark an item complete on intent alone.
2. Tick `[x]` only after the implementation is present and the relevant verification has passed. Leave incomplete work `[ ]` with a short note in the **Execution log**.
3. At every gate report **what changed, files, commands run, test output, and remaining blockers**. Do not silently skip a gate.
4. If source documentation contradicts the repository, record the conflict and favor **the live frontend API needs** for DTO shape, while preserving **the academic business rules** for authorization and domain behavior. Escalate irreconcilable cases instead of inventing behavior.
5. Work on a feature branch; do not push to `main` without review. Do not claim deployment is complete until the **public HTTPS API** has been tested.
6. **Backend work only**: existing `src/pages`, `src/components`, `src/context`, `src/styles.css`, SVGs, and React routing are out of scope. You may **read** them. Put API adaptation instructions in `docs/backend/frontend-integration.md`, **not** in rewritten React components.
7. The `src/api.js` methods are currently **synchronous localStorage calls**. A live HTTP implementation would require asynchronous frontend adaptation. **Backend availability does not equal frontend integration.** Record this explicitly in the handover.

### Progress dashboard

| Stage | Scope | Required for POC | Status |
| --- | --- | --- | --- |
| 0 | Inspect actual repo + contract freeze | Yes | [x] |
| 1 | Spring Boot runnable backend + MySQL | Yes | [ ] |
| 2 | Reconcile/reuse existing entity + repository layer | Yes | [ ] |
| 3 | Auth, role authorization, student/provider identity | Yes | [ ] |
| 4 | Listings, rooms, search, emergency, admin approvals | Yes | [ ] |
| 5 | Student ↔ landlord messaging | Yes | [ ] |
| 6 | Deterministic database seed + demo walkthrough | Yes | [ ] |
| 7 | Backend tests + contract documentation + POC acceptance | Yes | [ ] |
| 8 | Deploy small Azure VM, smoke test, handover | Yes | [ ] |
| 9 | Viewing requests, favourites, notifications | No, defer | [ ] |
| 10 | Applications, files, leases, analytics, other extras | No, defer | [ ] |

---

# 0. Locked scope and architectural decisions

### What this app actually is

- **Product:** CPUT Home — a **student accommodation search and landlord platform**, not a general campus shopping marketplace.
- **Actors:** Guest (limited preview), `STUDENT`, `LANDLORD`, `ADMIN`.
- **Academic high-priority flow:** CPUT student registration → browse/search/filter approved accommodation → see details → message a landlord; verified landlord creates a listing → admin approves it → listing appears in search; emergency rooms are prioritized.
- **Keep the existing frontend, routes, imagery and copy intact.** This tracker builds services under those existing features, not a replacement frontend.
- **Phase boundary:** Backend API usable through Swagger/cURL/Bruno/Postman and deployed. **Connecting every UI control is not this task.** A future frontend integration PR can replace local `API` methods, once async contracts are agreed.

### Tech choices — source-aware

- **Java 21, Maven, Spring Boot, Spring Data JPA, MySQL**. `TERM 2 -IND.docx` reports Spring Boot **4**, package `com.accommodation`, 22 JPA entities, factories and five repositories. **Inspect and reuse actual code before choosing a Boot version, entity identifiers or package names.** Do not regenerate the domain solely because this report describes it.
- **Flyway** recommended for reproducible schema management **if migrations are not already defined**. Do not add a second competing schema creation mechanism.
- **Spring Security + BCrypt + bearer JWT** for the POC; server-side authorization mandatory. Use an explicit dev/test JWT secret from environment, never source control.
- **REST + JSON**. Use the repo's existing proposed prefix **`/api`**, not the unrelated tracker’s `/api/v1`.
- **Single-process modular Spring Boot application**; no microservices, Kafka, Redis, GraphQL, WebSockets or cloud databases are required for the POC.
- **All prices use `BigDecimal`** in Java, appropriate DECIMAL columns in MySQL. Preserve the frontend price format (numeric JSON amounts in ZAR).
- **No actual payments**. `PROJECT 3.docx` explicitly excludes payment processing and legal contract generation from initial scope. The React demo’s leases/signatures are not a mandate to implement them in this POC.

### IMPORTANT: existing implementation is not fully verified

Your Term 2 report says the backend already has `User`, `StudentProfile`, `LandlordProfile`, `Accommodation`, `RoomListing`, the three accommodation subtype tables, 22 entities in total, two factories, and five repositories. Another related repo has been referenced as `InamKlaas/Student-Accommodation-Platform`, but it was **not accessible for inspection** through the connected GitHub access. Treat the report as *previously described work*, not proof that those files are present in the target repo today.

- [ ] Find the real backend source, request repository access **if necessary**, and list actual reusable files.
- [ ] Prefer integrating existing model/repository/factory code; do not create a competing domain or duplicate tables.
- [ ] If the backend is unavailable, document this fact and only then bootstrap a minimal compatible implementation.

### Critical domain rules (from project documents)

- A `STUDENT` must register with `@mycput.ac.za` (case-normalized); enforce on the server, not merely in React.
- `ADMIN` is provisioned through configuration/seed only; **no public admin registration**.
- A landlord must pass the modeled verification rule before publishing/creating listings, as dictated by the existing domain design.
- **All listings require admin approval before becoming visible to a student/guest.** A landlord cannot approve their own listing.
- Respect the report's separate flags: **`isPublished` (admin-controlled)** and **`isActive` (landlord-controlled)**. A public listing must pass both checks **and** have an available room where required.
- The model distinguishes **`Accommodation` (property)** from **`RoomListing` (bookable unit)**. Never flatten the database just because the React UI shows a flat card.
- **Unregistered visitors see a limited preview**; messages, requests, applications and favourites require identity. Student/landlord/administrator actions require the correct role and ownership.
- Approval of an accommodation is **human admin moderation**. `nsfas` and on-campus assertions in the seed are **illustrative, not confirmed accreditation**.
- Store **no genuine South African ID images or student documents** in the public POC database/VM. Use fake synthetic data only.

### No-go list

- [ ] Do **not** build the Varsity Connect cart, checkout, payments, lost-and-found, support tickets, or unrelated community modules.
- [ ] Do **not** refactor React to TypeScript/Tailwind or move `src/` into a new folder.
- [ ] Do **not** commit real credentials, tokens, Azure keys, `.env`, real identity documents, or base64 signatures.
- [ ] Do **not** mark frontend integration as done solely because endpoints exist.
- [ ] Do **not** add online payments or claim the platform is officially endorsed/accredited by CPUT.

---

# 1. Actual frontend inventory and mapping contract

The following is derived from **the inspected frontend code**, not the old tracker. Read the files again before coding.

| Existing path | Existing behavior / `API` calls | Backend responsibility | POC? |
| --- | --- | --- | --- |
| `src/pages/Auth.jsx`, `src/context/AuthContext.jsx` | `login`, `register`, `confirmEmail`, `me`, `logout`; student number, campus, year, funding | Identity, BCrypt, JWT, role checks, student-domain validation | **Yes** |
| `src/pages/Home.jsx` | `approved`, `favs`, `toggleFav`; filters by campus, type, price, available date, NSFAS label, amenities, emergency | Public browse/filter, approved+active+available query | **Yes**, favourites later |
| `src/pages/Listing.jsx` | `listing`, `thread`, `send`, `requestViewing`, `submitApplication`, `reportListing`, `canReview`, `review` | Detail DTO + student/landlord messaging | **Yes**, other actions later |
| `src/pages/Dashboard.jsx` | `listings`, `addListing`, `updateListing`, `removeListing`; viewings, applications, leases | Owned property/room CRUD + pending review | **Yes**, extra workflows later |
| `src/pages/Admin.jsx` | `users`, `setUserStatus`, `listings`, `updateListing`, `reports`, `setAccreditation`, announcements, audit, exports | Listing approval/rejection + landlord verification | **Yes** for moderation basics |
| `src/pages/Messages.jsx` | `threadsFor`, `thread`, `send` | Authorized conversation list/thread/reply | **Yes** |
| `src/pages/MyStuff.jsx` | `favs`, `viewings`, `applications`, `leases`, `notifications` | Saved listings and workflows | Deferred |
| `src/pages/Notifications.jsx` | `notifications`, `markNotificationsRead` | In-app notification feed | Deferred |
| `src/pages/Privacy.jsx` | `exportMyData`, `deleteMyData` | Privacy data export/deletion design | Deferred; **do not collect sensitive docs in POC** |

**Frontend representation versus normalized backend model**

`src/api.js` returns flat listing cards with fields such as `id`, `owner` (email), `ownerName`, `title`, `price`, `rent`, `location`, `campus`, `type` (`Single`, `Sharing`, `Bachelor`), `available`, `availableDate`, `emergency`, `status` (`pending`, `approved`, ...), `onCampus`, `nsfas`, `gender`, `amenities`, `beds`, `image`, `gallery`, `deposit`, `utilities`, `houseRules`, `shuttle`, and `reviews`. Keep the UI-compatible response shape through **response DTO projection** from normalized `Accommodation` + `RoomListing` + associated profiles. Do **not** collapse/replace the established entity model.

**Identifiers:** Current mock `id` values are numeric. Before building endpoints, inspect the real entity PK types; choose a **stable JSON-facing ID convention** with the frontend team. Prefer existing numeric IDs if the backend already uses them, otherwise document a migration strategy for the eventual `src/api.js` adapter. Never change every frontend `id` call just to satisfy a backend preference.

**Synchronous mock/API mismatch:** Since calls such as `API.approved()` are used inline during React render and `API.login()` returns synchronously, an HTTP wrapper cannot be substituted without asynchronous frontend changes. Create a written async-adapter handoff; **do not make those frontend changes in this backend-only tracker**.

### Required initial endpoint contract (per existing README)

| Verb | Route | Visibility | POC use |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Guest | Student/landlord registration |
| `POST` | `/api/auth/login` | Guest | Login, token |
| `GET` | `/api/auth/me` | Authenticated | Session identity |
| `POST` | `/api/auth/logout` | Authenticated | Stateless client logout or token revocation if implemented |
| `POST` | `/api/auth/verify-email` | Token holder | **Optional** with real one-time token; NEVER public `email`-only approval |
| `GET` | `/api/listings` | Guest/student | Approved public search with filters |
| `GET` | `/api/listings/{id}` | Public limited; owners/admin full | Room/property detail |
| `GET` | `/api/providers/me/listings` | Landlord | Own listings, including pending/inactive |
| `POST` | `/api/listings` | Verified landlord | Create draft/pending listing |
| `PATCH` | `/api/listings/{id}` | Owner/admin, field-restricted | Edit own listing / availability; material edit re-review policy |
| `DELETE` | `/api/listings/{id}` | Owner/admin | Soft deactivate preferred |
| `GET` | `/api/admin/listings?status=pending` | Admin | Approval queue |
| `POST` | `/api/admin/listings/{id}/approve` | Admin | Approve public visibility |
| `POST` | `/api/admin/listings/{id}/reject` | Admin | Reject with reason |
| `GET` | `/api/admin/providers?status=pending` | Admin | Provider verification queue |
| `PATCH` | `/api/admin/providers/{id}/verification` | Admin | Approve/reject provider |
| `GET` | `/api/conversations` | Student/landlord | Thread summaries |
| `GET` | `/api/listings/{id}/messages?studentId=` | Authorized participant | Thread history |
| `POST` | `/api/listings/{id}/messages` | Authorized participant | Send message |
| `GET` | `/actuator/health` | Controlled public/basic health | VM smoke testing |

**Note:** The repo README proposes `GET /api/listings`, `POST /api/listings/{id}/messages`, etc.; a few `.../me/...` and provider-verification endpoints above refine the contract for safe ownership. Document those additions before integrating; do not silently invent a new base path.

### Example listing JSON expected by the existing UI

```json
{
  "id": 1,
  "title": "Demo residence — Bellville",
  "owner": "landlord@demo.com",
  "ownerName": "Demo Provider",
  "price": 3050,
  "rent": 3050,
  "location": "Bellville",
  "campus": "Bellville",
  "type": "Single",
  "available": true,
  "availableDate": "2026-10-01",
  "emergency": true,
  "status": "approved",
  "onCampus": false,
  "nsfas": false,
  "amenities": ["WiFi", "Security"],
  "image": "/images/residence-bellville.svg",
  "gallery": ["/images/residence-bellville.svg"],
  "sample": true
}
```

This is a **compatibility sketch**, not an exact domain schema. Confirm all omitted mandatory fields from `Dashboard.jsx` and `Listing.jsx`, and document DTO schemas in OpenAPI. The `/images/...` assets are **already in the React frontend**; the backend does not have to generate new images.

---

# STAGE 0 — Repo inspection, reuse audit, and API freeze

## Goal

Do not start a parallel application by accident. Resolve where the 22-entity backend exists and freeze what this frontend actually needs for the submission POC.

### Tasks

- [x] Fetch and inspect current `main` tree of `InamKlaas/Project-3-FrontEnd-`; record head SHA.
- [x] Read `src/api.js`, `src/App.jsx`, `src/context/AuthContext.jsx`, `src/pages/Auth.jsx`, `src/pages/Home.jsx`, `src/pages/Listing.jsx`, `src/pages/Dashboard.jsx`, `src/pages/Admin.jsx`, `src/pages/Messages.jsx` and `README.md`.
- [x] List the existing mock `API` method names, request fields, returned shapes, user roles, error messages and status values in `docs/backend/frontend-contract-audit.md`.
- [x] Identify existing backend sources (including possible separate repo `InamKlaas/Student-Accommodation-Platform`); record accessible URL/branch and exact entity/repository/factory paths. **Do not assume the repo exists merely because the report mentions it.**
- [x] Inspect real `pom.xml`, packages, Spring Boot version, database settings, schema scripts, test setup and build status if backend access is granted.
- [x] Compare domain entities against the documented `User`/profiles, `Accommodation`/subtypes, `RoomListing` model. Make a missing/working/broken checklist.
- [x] Agree backend code destination: preferably a `backend/` directory **alongside existing frontend source** in the current repository; if source is already maintained elsewhere, preserve it and link/run it without cloning a second duplicate into production.
- [x] Record the current contracts for numeric frontend listing IDs, role strings, status strings and timestamp types; do not hardcode wrong defaults.
- [x] Write `docs/backend/poc-scope.md` spelling out the **POC stop line** and features not to implement.
- [x] Document any conflict between report, model and UI before writing new code.

### Verification / gate

- [x] Reproducible frontend read-only inventory completed.
- [x] Existing backend located/reuse decision documented (or access blocker explicitly logged).
- [x] Target repo/branch/code location recorded.
- [x] **STAGE 0 COMPLETE**

---

# STAGE 1 — Runnable Spring Boot app + local MySQL

## Goal

Run the existing/reused backend skeleton locally with MySQL and a health endpoint. **Do not touch frontend source.**

### Tasks

- [ ] Reuse the existing Maven application if found. Only scaffold a new Java 21 Spring Boot backend if there is genuinely none accessible.
- [ ] Keep existing `com.accommodation` packages and working class names where available.
- [ ] Include only needed dependencies: Web, Validation, Data JPA, Security, MySQL driver, Actuator, tests, optional Flyway and OpenAPI.
- [ ] Add `application.yml`, `application-dev.yml`, `application-test.yml` with values loaded from environment and no committed secrets.
- [ ] Add MySQL to a local `compose.dev.yml` (or equivalent) with persisted named volume, test DB and local-only port binding if required.
- [ ] Configure Hikari pool conservatively for a small demonstration database.
- [ ] Add `/actuator/health` (public minimal health, no secrets) and Swagger/OpenAPI docs for development.
- [ ] CORS: configure allowed frontend origin `http://localhost:5173` for local development and explicit deployed frontend origin later; no unconditional `*` with credentials.
- [ ] Establish consistent errors: `{timestamp,status,code,message,fieldErrors,path}`; use controlled exception advice and HTTP statuses.
- [ ] Provide `.env.example` with **placeholder names only** and backend README (`./mvnw spring-boot:run`, local MySQL start, tests).
- [ ] Add `.gitignore` protections for env files, logs, uploaded media, build artifacts and dumps.

### Verification / gate

- [ ] `./mvnw test` passes (or verified equivalent wrapper command).
- [ ] `./mvnw package` produces a JAR.
- [ ] Development server connects to local MySQL.
- [ ] `GET /actuator/health` returns healthy response.
- [ ] No accidental change to React files.
- [ ] **STAGE 1 COMPLETE**

---

# STAGE 2 — Reuse data model, migrate schema, wire repositories

## Goal

Make the previously reported 22-entity Spring Data JPA design persistent and queryable, while projecting flat response DTOs for CPUT Home cards.

### Model / repositories

- [ ] Inspect and reuse `User`, `StudentProfile`, `LandlordProfile`, `Accommodation`, `RoomListing`, three accommodation subtype entities and existing relations.
- [ ] Inspect all other reported entities; include only their existing dependencies rather than rewriting all 22 for the POC.
- [ ] Reuse existing `UserFactory` and `AccommodationFactory` if implemented and tested.
- [ ] Reuse/verify `UserRepository`, `StudentProfileRepository`, `LandlordProfileRepository`, `AccommodationRepository`, `RoomListingRepository`.
- [ ] Confirm email uniqueness and student number uniqueness; enforce at DB level as well as service layer.
- [ ] Confirm landlord verification state, `isVerified` and `VerificationStatus` remain consistent.
- [ ] Confirm accommodation `isPublished` and `isActive` are independent. Creation must **not** publish by default.
- [ ] Confirm new `RoomListing` requires a parent `Accommodation`, positive `monthlyRent`, valid availability and deposit values.
- [ ] Use services for transactions; controllers use request/response DTOs, never serialize JPA graphs directly.
- [ ] Choose documented ID strategy compatible with the frontend (numeric IDs or a stable mapping) and verify it across nested entities.

### Database setup

- [ ] Inspect whether a schema already exists. Preserve existing table names/columns when possible; use additive migrations rather than blind recreation.
- [ ] Create/repair Flyway migration set **only if no established migration system**; reconcile initial baseline for existing DBs.
- [ ] Add indexes for normalized email, role/verification, approval/active flags, campus/location and room availability/rent.
- [ ] Avoid JPA auto-creating production tables; use schema validation after migrations.
- [ ] Document relationship `Accommodation (1) -> RoomListing (many)` and subtype joins, with short Mermaid ER sketch if useful.
- [ ] Add DTO mapper that assembles flat listing response from property + selected room. Define behavior for multiple rooms and avoid accidental duplicated cards.

### Verification / gate

- [ ] Migrations apply cleanly to a **fresh MySQL database**.
- [ ] Repeat start produces no unexpected schema mutation or duplicate data.
- [ ] JPA validation succeeds; critical repository tests pass against MySQL/Testcontainers if available.
- [ ] Tests prove unapproved/inactive records are excluded from public repository queries.
- [ ] **STAGE 2 COMPLETE**

---

# STAGE 3 — Authentication, student identity, landlord verification

## Goal

Real backend identity and authorization for three roles; no trusting role or email supplied in request bodies.

### Registration and sessions

- [ ] Implement student registration accepting frontend fields `name`, `email`, `password`, `role`, `studentNumber`, `campus`, `year`, `funding`.
- [ ] Normalize email; enforce `@mycput.ac.za` for `STUDENT` on server.
- [ ] Reject duplicate email/student number and invalid/missing mandatory fields with deterministic 400/409 responses.
- [ ] Implement landlord registration as distinct profile creation with pending verification.
- [ ] Never accept public `ADMIN` role registration, even if request JSON says `role=admin`.
- [ ] Encode passwords with BCrypt; test no plaintext password leaks through DTOs/logs.
- [ ] Implement `POST /api/auth/login` with BCrypt check, suspended-user rejection, signed JWT and minimal safe public user DTO.
- [ ] Implement `GET /api/auth/me` from server-authenticated principal, not client email.
- [ ] Implement `POST /api/auth/logout` contract (document stateless token expiry/invalidation behavior honestly).
- [ ] Record token lifetime, bearer header format and client storage recommendation in integration guide.
- [ ] Implement email verification only with a **random one-time expiring token** if time allows; do not duplicate the insecure `confirmEmail(email)` browser simulation in a public endpoint. POC seeded student can be preverified to demonstrate core workflows.

### Admin/provider authorization

- [ ] Implement admin-only pending landlord verification list and approve/reject endpoints.
- [ ] New landlord starts `PENDING`; admin must explicitly set approved/verified.
- [ ] Protect listing creation according to the existing verified-landlord rule.
- [ ] Enforce student/landlord/admin authorization at controller **and** service/ownership boundary.
- [ ] Deny one student accessing another student’s records and one landlord editing another landlord’s properties.

### Verification / gate

- [ ] Student `@mycput.ac.za` registration accepted; other domains rejected for students.
- [ ] Landlord standard email registration accepted but cannot publish before verification.
- [ ] Public admin signup rejected; password stored hashed.
- [ ] JWT login/me works; missing/invalid token receives 401; wrong role 403.
- [ ] Suspension denies login and protected actions.
- [ ] Tests verify ownership is not bypassed by forged `owner`, `role`, or `student` JSON fields.
- [ ] **STAGE 3 COMPLETE**

---

# STAGE 4 — Accommodations, room inventory, search and approvals

## Goal

A single demonstrable end-to-end backend journey: verified landlord creates accommodation/room → admin approves → guest/student can find and inspect it → emergency search prioritizes immediately available rooms.

### Create/manage listing

- [ ] Model listing request in terms of property details **plus one or more** `RoomListing` records, while accepting the existing frontend’s flat `title`, `price`, `location`, `type`, `available`, `emergency`, etc. through a compatibility DTO.
- [ ] Map UI `Single`, `Sharing`, `Bachelor` to the correct normalized existing types; explicitly record any mapping that is not one-to-one with `SHARED_ACCOMMODATION`, `PRIVATE_ROOM`, `ENTIRE_UNIT`.
- [ ] On creation, infer owner from JWT, never from client-provided `owner`; set admin publish flag false and approval `pending`.
- [ ] Require title, description, location, positive monthly rent, contact data and approved image references according to the academic rules.
- [ ] Support backend-owned listing/room editing and availability updates, restricted to the owning landlord.
- [ ] Implement owner listing feed showing pending/approved/inactive items.
- [ ] Implement deactivation rather than destructive removal by default; admin can remove public visibility.
- [ ] Decide and document whether material edits after approval require re-review (recommended). Add test for the chosen rule.

### Public search and details

- [ ] Implement `GET /api/listings` with text/location, campus, min/max rent, room type, availability/date, emergency, on-campus, gender, amenity, and NSFAS demo-filter semantics reflected in `Home.jsx`.
- [ ] Default public results to **admin published + active + available** and suitable room inventory. No pending/deactivated items in public search.
- [ ] Support `sort=priority|price-low|price-high|newest`, deterministic tie-breaking and a documented capped page size.
- [ ] For `emergency=1`, return **only truly available** emergency rooms; do not treat an emergency flag as sufficient on its own.
- [ ] Implement `GET /api/listings/{id}`: public limited preview, authenticated detailed view, pending listings visible to owner/admin only.
- [ ] Return a flat DTO compatible with `ListingCard.jsx`/`Listing.jsx` (`gallery`, `image`, `price`, `rent`, `amenities`, `availableDate`, `ownerName`, etc.) projected from normalized records.
- [ ] Preserve `sample` marker and avoid presenting synthetic `nsfas`/`onCampus` labels as verified facts.

### Admin moderation

- [ ] Implement pending listing queue and explicit `approve`/`reject` commands with optional reason.
- [ ] Admin action updates published/approval state atomically; rejected listing never appears publicly.
- [ ] Log actor/time/reason for admin decisions (small database audit trail or existing entity if available).
- [ ] Enforce ownership and backend authorization even when the UI sends forged status changes.

### Verification / gate

- [ ] Pending new listing is invisible to guest/student search and visible to owner/admin.
- [ ] Admin approval makes the listing visible **only** if also active/available.
- [ ] Landlord deactivation removes it from public search; admin unpublish works independently.
- [ ] Search returns consistent results for campus, budget, type and emergency filters; invalid ranges get 400.
- [ ] Unauthorized landlord cannot update someone else’s record or set `status=approved`.
- [ ] Search/detail DTOs match recorded frontend field names and types.
- [ ] **STAGE 4 COMPLETE**

---

# STAGE 5 — Messaging between students and landlords

## Goal

Show actual persistent, authorized, two-way messaging for one listed room. **REST polling is sufficient for POC.**

### Tasks

- [ ] Reuse existing `Message`/conversation model if present; otherwise add minimal tables for message body, sender, student participant, listing/room, createdAt.
- [ ] Implement a **thread identity** scoped to `(listingId, studentId)` as implied by `API.thread(listingId, student)` in `src/api.js`.
- [ ] Implement `POST /api/listings/{id}/messages`, using authenticated sender and recipient authorization; ignore client-supplied `from` as an identity source.
- [ ] Allow STUDENT to open a conversation with an eligible listing’s landlord; allow that listing’s LANDLORD to reply to the same student.
- [ ] Implement `GET /api/listings/{id}/messages?studentId=...` with correctly ordered messages, timestamps and safe DTOs.
- [ ] Implement `GET /api/conversations` for per-user inbox summaries (corresponds to `threadsFor(user)`).
- [ ] Prevent unrelated students, landlords and guests from reading/injecting messages into conversations.
- [ ] Validate text length and reject blank messages; paginate or cap returned history.
- [ ] Test conversation history remains after app restart (actual MySQL persistence).
- [ ] Document that WebSocket/STOMP, typing indicators, read receipts and real-time push are **out of scope** for the POC.

### Verification / gate

- [ ] Student sends; landlord sees; landlord replies; student sees both messages after reload.
- [ ] Unauthenticated and nonparticipant access returns 401/403/404 as appropriate (do not leak private details).
- [ ] No client-controlled sender spoofing; messages survive restart.
- [ ] **STAGE 5 COMPLETE**

---

# STAGE 6 — Database seed (MANDATORY and separate)

## Goal

Make a freshly deployed database useful in minutes with repeatable **clearly labelled synthetic** accounts, residences, rooms, approvals and conversations. Seed must **not** be the React localStorage payload.

### Seed design

- [ ] Create `dev`/`demo` seed profile using `ApplicationRunner`, dedicated seed command, or SQL migrations designed for repeat execution. **Do not run demo seeds in ordinary production automatically.**
- [ ] Seed role-specific demo accounts: `STUDENT`, `LANDLORD`, `ADMIN`, with known **test-only** credentials documented locally. Hash passwords; never reuse the frontend mock's `admin123`/`demo123` on a publicly accessible VM.
- [ ] Add one **approved verified landlord**, one **pending landlord**, and multiple student records using reserved/synthetic identifiers and non-real personal information.
- [ ] Seed **8–12** realistic-looking but explicitly fake accommodation records across existing UI campuses (e.g., Bellville, Cape Town / District Six, Wellington, Parow), each with at least one `RoomListing`.
- [ ] Optionally expand from the existing `campusHomes` sample names in `src/api.js`, but preserve the README disclaimer; never imply names, availability, NSFAS status or prices have been independently confirmed.
- [ ] Include **at least**: 4 public approved+active+available listings, 2 pending listings, 1 admin-rejected/unpublished listing, 1 landlord-inactive listing, 2 emergency available rooms, 1 unavailable room.
- [ ] Seed varied price bands, `Single`/`Sharing`/`Bachelor` UI projections, different campuses, amenities, date availability, and image/gallery paths that exist under the existing frontend's `public/images/`.
- [ ] Seed one student↔landlord message thread (ideally 2 messages) to show inbox history immediately.
- [ ] Set `sample=true`, synthetic addresses and descriptions (`Sample area only; verify with provider`); never invent a credible private street address.
- [ ] Use fixed natural keys/UUIDs or an idempotent existence check so a second seed does not duplicate 12 listings or three users.
- [ ] Mark dev/demo account credentials as **development only** in README. For public demo deployment, use separate **strong generated** credentials distributed privately, or seed locked/read-only showcase records with no publicly documented administrative login.
- [ ] Add `db-reset-demo.sh` or a documented reproducible reset procedure for local test environment only; guard destructive operation so it cannot run against public VM by accident.
- [ ] Keep seed implementation, input assets and scripts version controlled, but never store a real `.env` or decrypted credentials in git.

### Verification / gate

- [ ] Fresh empty DB + seed produces expected count and relationships.
- [ ] Re-running seed changes no row counts and creates no duplicates.
- [ ] Public `GET /api/listings?emergency=1` returns emergency approved active *available* seed records only.
- [ ] Pending, rejected, inactive and unavailable items are excluded from public results.
- [ ] Seeded student and verified landlord can log in with local test secrets; seeded admin can approve pending listing.
- [ ] Seeded messages can be read by their participants, not others.
- [ ] **STAGE 6 COMPLETE**

---

# STAGE 7 — POC tests, API examples and backend-only handoff

## Goal

Prove this is a functioning backend **without requiring frontend code changes**. Give the team precise instructions to wire the existing React later.

### Automated tests

- [ ] Test service logic for student email rule, landlord verification, admin approvals, active/published filters and emergency search.
- [ ] Test login, bad password, duplicate student number, forbidden role/ownership attempts, and admin-only endpoints.
- [ ] Test search query combinations, invalid budget/date ranges, multi-room DTO mapping and public preview restrictions.
- [ ] Test messaging participant restrictions and persist/reload behavior.
- [ ] Test seed repeatability and one clean DB migration.
- [ ] Add at least one Spring Boot/MockMvc API integration path for **register/login → create listing → approve → find listing → send message**.
- [ ] Run Maven tests and package with a fresh database; record command/result.

### Docs & handover

- [ ] Export a Swagger/OpenAPI description and add a minimal **Bruno collection, Postman collection, or `requests.http`** for the core workflow.
- [ ] Include JWT login and curl examples with placeholders, not hardcoded secrets.
- [ ] Write `docs/backend/frontend-integration.md`: method-by-method replacement map for `API.login`, `API.me`, `API.approved`, `API.listing`, `API.addListing`, `API.updateListing`, `API.send`, `API.thread`, `API.threadsFor`, etc.
- [ ] Explicitly document how to convert synchronous `src/api.js` to Promise-returning HTTP calls **in a future frontend PR**; note each screen that currently renders local data without async state.
- [ ] Document API field aliases/projections for `type`, `price`, `available`, `status`, `owner`, `id`, `image` and `gallery`; list any divergence from mock shape.
- [ ] Write `docs/backend/poc-demo-script.md` (10 steps maximum): register/login, approved search, emergency search, landlord listing, admin approval, student message, landlord reply.
- [ ] Document remaining frontend demo-only functions **not implemented**, rather than making stub endpoints that falsely return success.

### Final local acceptance checklist

- [ ] A clean clone with README steps starts MySQL/backend without special local knowledge.
- [ ] API documented and testable via HTTP without React/browser localStorage.
- [ ] No 500 errors in the scripted happy-path demo.
- [ ] Illegal role/ownership actions blocked by backend.
- [ ] Uploaded real documents, legal signatures, payments and sensitive data are **not part of demo**.
- [ ] **STAGE 7 COMPLETE**

---

# STAGE 8 — Deploy to a small Azure Linux VM

## Goal

One affordable, repeatable **backend-only POC deployment** with Spring Boot and MySQL on the same small VM. No Kubernetes, managed database, load balancer or Azure App Service required.

### Selected deployment shape

| Component | POC choice |
| --- | --- |
| Cloud | Microsoft Azure VM, **Ubuntu LTS**, supported region and subscription quota |
| Size | Target **Standard_B2ls_v2** — 2 vCPU, **4 GiB RAM**, if offered in the chosen region; comparable **4 GiB** burstable SKU is acceptable. Check actual Azure price/availability before provisioning. |
| Disk | Small OS/data disk with sufficient free space for Docker/MySQL and logs; start around 30–64 GiB and check quota/cost |
| Services | `cput-home-api` Spring Boot container + `mysql` container + `caddy` HTTPS reverse proxy |
| Inbound | TCP 80/443; TCP 22 limited to the developer's IP (or Bastion) |
| Private | MySQL 3306 and Spring Boot 8080 **not publicly exposed** |
| Address | Azure Public IP DNS label/FQDN or owned domain for TLS certificate |
| Persistence | Docker named volume for MySQL; backups stored **off-VM** |

This is a **demo** deployment, not a production HA claim. On a combined app+DB 4 GiB host, set conservative JVM heap and MySQL memory limits and validate under load; choose a larger VM only after measuring. Microsoft documents `Standard_B2ls_v2` as a 2-vCPU, 4-GiB B-series option; regional availability and cost vary.

### Azure provisioning

- [ ] Check Azure credits/budget, available region, B-series quota and estimate VM+disk+IP+egress cost. Configure budget alert; no claim that the VM is permanently free.
- [ ] Create dedicated project resource group (e.g. `rg-cput-home-poc`) and Linux VM via portal or Azure CLI.
- [ ] Set SSH key auth, disable password login if practical, use non-root deploy user.
- [ ] Create a DNS label or configure domain to point to Public IP; confirm DNS resolution.
- [ ] Set NSG: inbound **80/443 from internet** for HTTPS; **22 only from admin IP**; do not add public 3306/8080 rules.
- [ ] Install Docker Engine + Docker Compose plugin, configure automatic restarts and security updates.

### Application deployment

- [ ] Create backend multi-stage Dockerfile or documented JAR image build from Java 21 Maven project. Use a small JRE runtime image, non-root process when possible.
- [ ] Add `compose.prod.yml` for API, MySQL, Caddy. MySQL and API share a private Docker network; only Caddy publishes 80/443.
- [ ] Configure API to bind inside container; Caddy proxies `/api/*` and allowed health path to API. Existing frontend is **not** deployed/replaced here.
- [ ] Configure DB URL, username, password, JWT secret, allowed CORS origin and Spring profile using **private VM environment file** (`chmod 600`), never git.
- [ ] Configure health checks, `restart: unless-stopped`, log rotation and persistent DB volume.
- [ ] Enable schema migrations at startup; confirm app cannot start against incompatible schema.
- [ ] Use dev/demo seeding **only for deliberate demo deployment**, with synthetic data and strong privately held credentials. Do not expose predictable admin credentials.
- [ ] Obtain a publicly trusted HTTPS certificate using Caddy and a real DNS hostname; test TLS from a different device.
- [ ] Cap Java heap initially (e.g. `-Xmx768m` with room for JVM off-heap) and monitor JVM/MySQL memory under a short multi-request test.

### Operational basics

- [ ] Create a database backup script scheduled daily, rotate backups and transfer backup to separate storage (e.g. Azure Blob/private secured destination); confirm ability to **restore** a test snapshot.
- [ ] Create `docs/backend/azure-deploy.md` with provisioning, deploy, update, restart, logs, backup/restore, shutdown and delete-resource-group instructions.
- [ ] Record deployment URL, API health URL and repo commit SHA; **do not** record passwords or private keys.
- [ ] Test API endpoint availability after VM/container restart and after MySQL restart.
- [ ] Configure an Azure spending alert and document **stop/deallocate** when idle (storage/IP may still incur cost).

### Public VM acceptance / stage gate

- [ ] HTTPS `/actuator/health` (or equivalent documented health route) responds `200` with no secrets.
- [ ] Public `GET /api/listings` returns the seeded approved active available rooms.
- [ ] `GET /api/listings?emergency=1` returns seeded emergency inventory.
- [ ] Private role-controlled endpoint refuses anonymous access.
- [ ] Authenticated test: landlord creates → admin approves → student sees → student/landlord exchange a message.
- [ ] 3306 and 8080 are **not** reachable from the public internet.
- [ ] Backups and logs tested; secrets absent from repo; restart survives without losing database records.
- [ ] Azure resource budget documented.
- [ ] **STAGE 8 COMPLETE — POC STOP.** Do not continue into the deferred stages unless the project owner separately authorizes it.

---

# STAGE 9 — DEFERRED: Viewing requests, favourites, notifications

> **Not needed for current submission POC. Leave all checkboxes unchecked unless specifically authorized.** These are medium-priority features in the academic project and are represented by the existing React UI.

- [ ] `POST /api/listings/{id}/viewings`, `GET /api/viewings/me`, `PATCH /api/viewings/{id}` with student/owner access and pending/accepted/declined transitions.
- [ ] `GET /api/students/me/favorites`, `PUT /api/students/me/favorites/{id}` and removal behavior, stored per authenticated student.
- [ ] `GET /api/notifications`, `PATCH /api/notifications/read` using persistent in-app events for messages, viewings and approvals.
- [ ] Add seed data and tests for each optional workflow.
- [ ] **STAGE 9 COMPLETE (OPTIONAL)**

---

# STAGE 10 — DEFERRED: Other existing UI flows

> **Do not implement automatically.** The repo's mock layer contains more than the academic project's original essential scope.

- [ ] Application submission + status and secure document storage (requires privacy/security review; no genuine ID documents in a public classroom demo).
- [ ] Reviews with signed-lease prerequisite if the team decides to preserve the mock's rule.
- [ ] Listing reports, moderation queue and full account management/suspension/deletion.
- [ ] Notifications/announcements, immutable audit trail, analytics, CSV exports and privacy export/deletion.
- [ ] Provider onboarding uploads and accreditation management with verifiable claims rather than trusting mocked sample fields.
- [ ] Lease issuance and canvas signature: treat as a non-legal demonstration only, **not** legal contract generation; explicit separate approval required.
- [ ] Email verification with real email provider and one-time tokens; password reset only if needed.
- [ ] Frontend async integration (replace `src/api.js` and refactor affected React screens) as a **separate frontend-owned PR**.
- [ ] **No payment processing** without a separate scope/security/legal decision.
- [ ] **STAGE 10 COMPLETE (OPTIONAL)**

---

# Mandatory POC evidence bundle

When Stage 8 passes, produce these from the actual implementation (never invent evidence):

- [ ] Backend source committed and reviewed, with link + SHA.
- [ ] One-page architecture explanation: existing React frontend → HTTP `/api` → Spring Boot services → Spring Data JPA → MySQL.
- [ ] Entity relationship summary showing `USER`/profiles and `ACCOMMODATION`/`ROOM_LISTING`/subtypes.
- [ ] OpenAPI specification or API collection and role-based sample requests.
- [ ] Seed instructions, synthetic account-role list (passwords privately shared, not published) and repeatability test.
- [ ] Evidence for admin publish workflow and emergency filtering.
- [ ] Evidence for student↔landlord messaging and role restriction.
- [ ] `mvn test` result with number of tests/passes; record failures honestly.
- [ ] Public HTTPS API URL and health proof; Azure VM resources + ports diagram/screenshot if requested for submission.
- [ ] Exact **not implemented** list, especially browser-to-backend React migration and deferred lease/application features.

## Submission demo script (backend HTTP only)

1. Check public API health.
2. Browse seeded approved rooms, then filter emergency rooms.
3. Log in as student; show `me` and reject an unauthorized admin action.
4. Log in as verified landlord; create a new property/room (still pending).
5. Show it absent from the public list.
6. Log in as admin; approve the listing.
7. Show it now appears in student search and has full details.
8. Send a message as the student.
9. Log in as that landlord; read the thread and reply.
10. Restart API and prove room/message persistence.

---

# Execution log (Muse updates after each stage)

| Date/time (SAST) | Stage | Repo/commit | Files changed | Tests/commands | Result or blocker |
| --- | --- | --- | --- | --- | --- |
| 2026-10-08 ~21:00 | 0 | `Project-3-FrontEnd-` @ `40b5801`, branch `poc/backend` | tracker copy, `docs/backend/frontend-contract-audit.md`, `docs/backend/poc-scope.md` | read-only frontend inventory; `gh repo view` both repos | **STAGE 0 COMPLETE**. Remote backend domain deleted upstream (`639b691` et al, only skeleton left); Term 2 22-entity report unverifiable → bootstrap minimal `backend/` here. No React files touched. |

## Final status

- **Required stages passed:** `0 / 9`
- **Ready to submit:** **NO** (until Stages 0–8 pass)
- **Frontend connected to live backend:** **NO / OUT OF SCOPE** (must not be misrepresented)
- **Azure HTTPS API URL:** _Not deployed_
- **Known blockers:** _None recorded yet_

*End of backend-only CPUT Home POC tracker. Stop at Stage 8.*
