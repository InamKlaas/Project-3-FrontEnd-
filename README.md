# CPUT Home

Student accommodation marketplace frontend for the CPUT PRT362S group project. This is an independent student prototype, not a CPUT service. Residence names are sample references; prices, availability, accreditation, and provider details are invented demonstration data and must be verified against CPUT's official information before use.

## Run locally

Requirements: Node.js 18 or newer and npm.

```sh
npm install
npm run dev
```

Open the local URL Vite prints, normally `http://localhost:5173`. Create a production bundle with `npm run build`; preview that bundle using `npm run preview`.

## Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Student | `220000001@mycput.ac.za` | `demo123` |
| Landlord / provider | `landlord@demo.com` | `demo123` |
| Admin | `admin@cputhome.co.za` | `admin123` |

New student accounts must use `@mycput.ac.za`. Registration presents a simulated email confirmation; no email is actually sent. The landlord verification screen and admin actions are also local simulations.

## Included workflows

- Browse seeded sample residences, emergency rooms, and filters for campus, price, setting, room type, gender, NSFAS claim, amenities, availability, and sort order.
- Residence detail galleries use original local SVG illustrations in `public/images`; students can switch between exterior, room, and shared-space views. Provider listings support multiple uploaded photos.
- Student registration/profile, saved listings, messages, viewing requests, application tracker, application document inputs, notifications, reviews, reporting, and lease signature/print flow.
- Provider verification submission, listing management, viewing and application inboxes, application decisions, and lease issuance.
- Admin listing/provider verification, accreditation controls, reports, user suspension/removal, announcements, audit log, simple analytics, and CSV exports.
- Local POPIA-style data export and deletion controls.

## Data and security limitations

All mock data operations are centralized in `src/api.js`. Data persists in browser `localStorage` under `cputhome_v2`; clear that key in browser developer tools to reset the demo. Uploaded files and drawn signatures are stored as base64 strings in localStorage. Do not upload real identity, registration, funding, or ownership documents. This prototype has no backend authorization, encryption, secure upload, real email verification, or legal lease review. Client-side route guards are a UX feature, not a security boundary. User text renders as React text, not injected HTML.

## Spring Boot endpoint contract

Replace the corresponding methods in `src/api.js` with calls to these endpoints. The exact request/response DTOs and authentication scheme should be agreed with the backend team. Use server-side role checks, validation, authorization, and secure document storage.

| Frontend operation | Suggested endpoint | Purpose |
| --- | --- | --- |
| `login`, `register`, `confirmEmail`, `me`, `logout` | `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/verify-email`, `GET /api/auth/me`, `POST /api/auth/logout` | Authentication, CPUT email confirmation, session |
| `listings`, `approved` | `GET /api/listings?campus=&minPrice=&maxPrice=&roomType=&available=&emergency=&nsfas=&amenities=&sort=` | Search and filtered listing results |
| `listing` | `GET /api/listings/{listingId}` | Full listing details, provider, reviews |
| `addListing`, `updateListing`, `removeListing` | `POST /api/listings`, `PATCH /api/listings/{listingId}`, `DELETE /api/listings/{listingId}` | Provider listing management |
| Admin listing verification | `GET /api/admin/listings?status=pending`, `POST /api/admin/listings/{listingId}/approve`, `POST /api/admin/listings/{listingId}/reject` | Approve or reject with a reason |
| `favs`, `toggleFav` | `GET /api/students/me/favorites`, `PUT /api/students/me/favorites/{listingId}` | Student saved listings |
| `requestViewing`, `viewings`, `setViewing` | `POST /api/listings/{listingId}/viewings`, `GET /api/viewings/me`, `PATCH /api/viewings/{viewingId}` | Request, list, and decide viewings |
| `send`, `thread`, `threadsFor` | `POST /api/listings/{listingId}/messages`, `GET /api/conversations`, `GET /api/listings/{listingId}/messages?studentId=` | On-platform messaging |
| `submitApplication`, `applications`, `setApplication` | `POST /api/listings/{listingId}/applications` (multipart), `GET /api/applications/me`, `PATCH /api/applications/{applicationId}` | Applications, documents, provider decisions |
| `issueLease`, `leases`, `signLease` | `POST /api/leases`, `GET /api/leases/me`, `POST /api/leases/{leaseId}/sign` | Generate, view, and sign leases |
| `reportListing`, `reports`, `resolveReport` | `POST /api/listings/{listingId}/reports`, `GET /api/admin/reports`, `PATCH /api/admin/reports/{reportId}` | Fraud/misleading-listing moderation |
| `onboardProvider`, `setAccreditation` | `POST /api/providers/me/verification` (multipart), `PATCH /api/admin/providers/{providerId}/accreditation` | Provider identity/ownership and accreditation |
| `notifications`, `markNotificationsRead` | `GET /api/notifications`, `PATCH /api/notifications/read` | In-app notifications |
| `users`, `setUserStatus`, `removeUser` | `GET /api/admin/users`, `PATCH /api/admin/users/{userId}`, `DELETE /api/admin/users/{userId}` | Account management |
| `announce`, `auditLog` | `POST /api/admin/announcements`, `GET /api/admin/audit` | Announcements and immutable audit history |
| Analytics | `GET /api/admin/analytics` | Listings by campus, applications over time, emergency count |
| CSV export | `GET /api/admin/exports/listings.csv`, `GET /api/admin/exports/applications.csv` | Server-generated CSV downloads |
| `exportMyData`, `deleteMyData` | `GET /api/privacy/export`, `DELETE /api/privacy/me` | Data access and account/data deletion requests |

## Simplifications

- Email confirmation is a button; there is no mail provider or confirmation token.
- Listings and providers are seeded local sample data. The prototype cannot certify CPUT ownership or accreditation.
- Seeded residence addresses show only a sample area; exact street addresses are not invented. Providers can enter an address for their own listing.
- Lease generation uses a short demo template; signatures are a local typed name and canvas image, not a legally vetted e-signature.
- Documents are base64/localStorage records, with no file-size, malware, encryption, or access-control service.
- Notifications are local in-app records; there is no push or email delivery.
- Analytics are computed from browser seed/demo records; charts use CSS, not a chart library.
- Authentication stores demo credentials in browser data; never use real passwords with this mock API.