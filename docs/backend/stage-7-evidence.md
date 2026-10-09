# Stage-7 verification checkpoint — 9 October 2026

Branch: `poc/backend`, repository `InamKlaas/Project-3-FrontEnd-`. The owner authorized frontend wiring/testing through stage 7 and then requested a commit/stop. The final commit is available through `git log -1`.

## Verified commands and results

| Command | Result |
| --- | --- |
| `mvn -o verify "-Dspring.jpa.show-sql=false"` from `backend/` | **51 tests, 0 failures/errors/skips**; executable JAR packaged |
| `npm ci --no-audit --no-fund` | Dependencies installed from the lockfile; corrected the interrupted native Rollup installation |
| `npm test` | **4 Chromium browser tests passed**, actual React → HTTP → local MySQL |
| `npm run build` | Vite production bundle built, 49 modules transformed |
| `npm run docs:openapi` | Current running API exported **20 paths** |
| Dev JAR startup with `DB_PASSWORD` in environment | MySQL 8.0.43, V4 migration applied, JPA validated, health UP; seed created no duplicate properties |

Backend test groups: admin 7, auth 10, errors 6, repository 7, card mapper 3, listings 12, messaging 4, journey 1, seed 1.

## Browser evidence

1. Guest search sends filters to the API, shows the matching emergency-room rent, hides address/contact in preview, sorts correctly, and displays invalid-budget errors.
2. New landlord cannot submit until verified. Admin verifies; landlord creates a pending property; guest cannot find it; admin approves; student registers/finds it; student sends and landlord replies; browser reload restores the JWT identity and both messages. Availability hides/returns the property; rent editing returns it to review. Synthetic test inventory/users are removed afterward.
3. Non-CPUT student signup is denied, a student cannot access the admin account feed or another student's conversation, and UI role guards redirect unauthorized admin navigation.
4. Interrupted search displays a retryable failure, retry loads real listings, bad login displays an error, and an invalid JWT is cleared on session restoration.

The final suite passed without retries. Earlier runs exposed a cold-start timeout and a test selector that did not match the select's accessible label; both were corrected. The final frontend build was rerun independently after a concurrent build timed out under browser-test load.

## Scope and evidence limits

- Fresh H2 fixtures run before integration tests. The current live MySQL database was upgraded V3 → V4; this checkpoint does not claim a fresh-clone/fresh-V4-database run.
- Browser reload persistence is proven. A dedicated API process-restart reread for the final wiring build was not repeated; the tracker leaves that proof unchecked.
- The optional shell reset helper was repaired but its runtime was not verified on Windows. The documented local SQL reset is explicit and destructive, not an automatic startup operation.
- Existing CSS, routing structure and SVG illustrations are retained. Document/upload/lease/report/notification workflows display deferred panels; saved IDs remain browser-only. No deferred server endpoint returns fabricated success.
- Azure deployment is deferred; no public HTTPS deployment is claimed.

API contracts, demo instructions and deferred boundaries are in the root README, `frontend-integration.md`, `poc-demo-script.md`, `cput-home-poc.http` and `openapi.json`.
