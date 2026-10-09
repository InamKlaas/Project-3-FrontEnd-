# CPUT Home POC demo script (backend HTTP only, 10 steps)

1. `GET /actuator/health` → `UP`.
2. `GET /api/listings?size=5` → seeded approved rooms (guest, no token).
3. `GET /api/listings?emergency=true` → only available emergency rooms.
4. Register + login a student, `GET /api/auth/me`, then `GET
   /api/admin/providers` as that student → 403.
5. Login as a verified landlord, `POST /api/listings` → 201, still
   `pending` and absent from the public list.
6. Login as admin, `GET /api/admin/listings?status=pending`, then
   `POST /api/admin/listings/{id}/approve`.
7. As the student, search again → the room appears with full details.
8. As the student, `POST /api/listings/{id}/messages` → 201.
9. As the landlord, read the thread, reply, student reads both messages.
10. Restart the API container/process, repeat steps 2 and 8-read — same rows.
