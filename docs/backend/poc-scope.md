# POC scope and stop line — CPUT Home backend (stage 0)

## Destination decision

Backend lives in `backend/` inside `InamKlaas/Project-3-FrontEnd-`
(alongside the existing frontend source), on branch `poc/backend`.
The separate `InamKlaas/Student-Accommodation-Platform` repo was inspected:
at `origin/main 639b691` its `backend/src` holds only the Spring skeleton
(`AccommodationApplication`, properties, one test). The previously reported
22-entity domain (`User`, `Student`, `Room`, `Review`, `Report`, `Property`,
`Message`, `Landlord`, …) was deleted upstream in a series of delete commits
— there is nothing left to reuse. So per the tracker fallback, the POC
bootstraps a minimal compatible domain here instead of cloning a duplicate.

The Term 2 report (`TERM 2 -IND.docx`) describing 22 entities, factories
and five repositories is treated as previously described work, not proof of
present files. Package is `com.cputhome` (fresh), not `com.accommodation`.

## Current POC stop line (stages 0–7, owner override)

Auth (student `@mycput.ac.za` rule, landlord pending verification, seeded
admin), accommodation + room listings with admin approve/reject and
landlord activate/deactivate, public search with emergency priority,
student↔landlord messaging over REST polling, deterministic seed, tests,
contract docs and frontend HTTP wiring with browser wire-up tests. The owner lifted the backend-only restriction and deferred Azure stage 8.

Deferred server workflows: viewings, favourites, notifications, applications/documents,
leases/signatures, reviews, reports, full analytics/exports, privacy tooling,
announcements, OAuth and payments. Saved IDs remain browser-only; basic existing
admin controls and listing CSV use real POC feeds.

## Conflicts recorded before code

1. Flat UI cards vs normalized model: resolved by response-DTO projection
   (`Accommodation` + `RoomListing` → flat card). Never flatten the DB.
2. UI `Single`/`Sharing`/`Bachelor` vs `SHARED_ACCOMMODATION`/`PRIVATE_ROOM`/
   `ENTIRE_UNIT`: mapping recorded in stage 4; card shows starting room price.
3. Numeric mock ids vs backend ids: backend uses numeric `Long` PKs, stable
   for the existing `+id` coercion.
4. `nsfas`/`onCampus`/`sample` labels are illustrative claims, never
   verified facts. Seed keeps the README disclaimer; no real addresses.
5. Email verification: mock `confirmEmail(email)` is insecure by design.
   Backend offers one-time tokens only if time allows; seeded demo student
   is preverified. No public email-only approval endpoint, ever.
6. No genuine SA ID images or student documents in any seed, test, or VM.
   Synthetic data only.
