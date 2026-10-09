# CPUT Home

Student accommodation platform for the CPUT PRT362S group project. The existing React UI now connects to a Java 21 / Spring Boot 4.0.6 / MySQL backend through `/api`.

This is an independent student prototype, not an official CPUT service. The expanded catalogue includes real residences and photos from CPUT's official virtual tours. Seed prices, availability and provider accounts remain development examples.

## Start locally

Requirements: Node.js 20+ (verified with 22.16.0), Java 21, Maven 3.9+ (or the included Maven wrapper), and MySQL 8 running locally.

### Windows launcher

Double-click **`start-cput-home.bat`** in the repository root. It installs frontend dependencies when missing, rebuilds the backend, and opens the dev backend and frontend in separate windows. It uses Maven from PATH or falls back to the included wrapper.

MySQL must already be running. The launcher loads local `KEY=value` settings from `backend/.env`; exported environment variables take precedence. It prompts for `DB_PASSWORD` only if unset, and `DB_USERNAME` defaults to `root`. Wait for the backend to report **Started**, then open **http://localhost:5173**. The API stays on **http://localhost:8080/api**. Press **Ctrl+C** in each server window to stop it.

Run `.\start-cput-home.bat --check` from PowerShell to check installed tools and available ports without starting servers.

### 1. Backend

From `backend/`, set the database password in the process environment, then run the dev profile. Flyway creates/validates the schema; the dev-only seed adds 15 properties, five accounts and a two-message thread without duplicating them or overwriting verification decisions.

PowerShell:

```powershell
$env:DB_PASSWORD = 'your-local-mysql-password'
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

macOS/Linux:

```sh
export DB_PASSWORD='your-local-mysql-password'
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Defaults: database `cput_home`, MySQL user `root`, API port `8080`. Set `DB_URL` and `DB_USERNAME` for a different local database/user. `backend/.env.example` lists supported environment variables; Spring does not automatically load a `.env` file.

Health: `http://localhost:8080/actuator/health`. Swagger: `http://localhost:8080/swagger-ui.html`.

### 2. Frontend

From the repository root, in a second terminal:

```sh
npm ci
npm run dev
```

Open **http://localhost:5173**. `VITE_API_URL` defaults to `http://localhost:8080/api`; the root `.env.example` is a template for a custom API URL. CORS permits `http://localhost:5173` by default.

## Development-only accounts

All five seeded accounts use **`SeedDemo123!`**, only in the local `dev` profile.

| Role | Email |
| --- | --- |
| Student | `220001001@mycput.ac.za` |
| Student | `220001002@mycput.ac.za` |
| Verified landlord | `verified-landlord@seed.local` |
| Pending landlord | `pending-landlord@seed.local` |
| Admin | `admin@seed.local` |

New students must use `@mycput.ac.za`. Email delivery/verification is deferred: there is no simulated confirmation button and no email-only approval endpoint. Newly registered students remain `pending-email` but can browse/message for this POC. New landlords need admin verification before creating a listing.

## Wired workflows

- Public search uses server-side filters, deterministic sorting and pagination. Emergency/type/budget filters project the cheapest **matching available room**, rather than a different room in the property.
- Guests receive limited previews from the API. Signed-in students receive full details and can start a persistent conversation.
- Providers see their own pending/approved/rejected/inactive listings; create, rename, reprice, change availability, deactivate and reactivate.
- Admins verify providers, approve/reject listings, toggle a demo accreditation flag and enable/disable/remove accounts. Core moderation decisions record actor, timestamp and reason in MySQL.
- Landlord replies and student messages use authorized, persistent REST threads; use **Refresh messages** to fetch incoming replies.
- Login sessions restore from `/auth/me`; failed requests have visible retry states. JWTs expire after 24 hours by default and are stored in browser localStorage for this POC. Logout removes the local token; it does not revoke already-issued JWTs.

Room applications, supporting-document uploads, concern reports and reviews are persisted by the API. Students track applications in **My housing**, providers decide them in **Applications**, and admins resolve concerns in **Reports**. Documents accept PDF/JPEG/PNG up to 5 MB each, with three documents per application. Files are stored under `backend/uploads/applications/` and downloaded through authenticated API requests.

Use the navigation's **Dark mode / Light mode** button to switch themes; light is the default and the choice survives reload. Liked listings remain browser-local and appear in **My housing**.

The four additional residences are St Peters, Hanover, Thibault Square / Vogue House and Ruskin House, with three real CPUT / Kuula photo URLs each. Names, tour attribution and image URLs are kept in `backend/src/main/resources/seed/cput-residences.properties`, sourced from [CPUT's official tour directory](https://www.cput.ac.za/student/support-services/dsa/residence/view-residences-in-360).

New integration checks: from `backend/`, run `mvn test -Dtest=HousingWorkflowIntegrationTest`; from the root, run `npx playwright test tests/e2e/housing-integration.spec.js` against the dev backend.

## Verification

Backend (from `backend/`, H2 test profile):

```sh
mvn verify
```

Frontend (root; MySQL/dev backend must already be running for browser tests):

```sh
npm run build
npx playwright install chromium
npm test
```

The browser suite drives the actual frontend and MySQL API, including registration → provider verification → listing approval → chat/reply → reload → rent re-review. It cleans up its successfully created synthetic accounts and properties. The failure-recovery test intentionally interrupts a request.

Verified results and limitations are in [the stage-7 evidence](docs/backend/stage-7-evidence.md). API reference: [OpenAPI](docs/backend/openapi.json), [HTTP requests](docs/backend/cput-home-poc.http), [integration guide](docs/backend/frontend-integration.md), [10-step demo](docs/backend/poc-demo-script.md).

With the current dev API running, regenerate the specification using `npm run docs:openapi`. Azure deployment is deferred by the owner's stage-7 stop line.
