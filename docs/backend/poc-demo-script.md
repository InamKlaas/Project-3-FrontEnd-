# CPUT Home POC demo — 10 steps

Start MySQL, the dev-profile API, and Vite using the root README. Use separate browser profiles/private windows for roles. All sample claims are illustrative.

1. Check `http://localhost:8080/actuator/health` → `UP`; open `http://localhost:5173` as a guest.
2. Browse/filter residences; click Emergency and inspect **Sample House Bellville**. The matching emergency room starts at R2,900. A guest sees limited details.
3. Register a synthetic student using `@mycput.ac.za`, or log in as `220001001@mycput.ac.za`. Full details and messaging are now available.
4. Register a synthetic landlord. The dashboard shows pending verification and disables listing submission.
5. Log in as `admin@seed.local`, open Providers, and verify that landlord.
6. Refresh the landlord's verification, then submit a residence with a synthetic address, room/rent and `/images/` photo references. It appears in the owner's feed as pending and is absent publicly.
7. Refresh the admin queue and approve it. Guest/student search now finds it.
8. As the student, send a question from the detail page. As the landlord, open Messages and reply. Student Refresh messages/reload shows both persisted messages.
9. Change the landlord's availability: mark leased removes the property from public results; mark available returns it. Editing rent returns it to pending review.
10. Show denied student admin access and an unrelated conversation request (403/404), then inspect the browser test result and backend test summary. Process-restart persistence can additionally be checked by restarting only the API and rereading an existing thread; this final check is not claimed by the browser reload test.

Local seeded passwords are listed in the README. The runnable HTTP alternative is `cput-home-poc.http`; browser automation is `npm test`.
