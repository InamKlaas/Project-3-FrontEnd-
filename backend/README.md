# CPUT Home backend

Java 21, Spring Boot 4.0.6, Maven, MySQL 8. Run commands from this directory. Configuration uses `.properties` throughout.

## Local dev

Set `DB_PASSWORD` to your local MySQL password. Defaults: user `root`, database `cput_home`, port 8080. Use `DB_USERNAME`/`DB_URL` to override. `.env.example` documents environment variables; it is not loaded automatically.

```powershell
$env:DB_PASSWORD = 'your-local-mysql-password'
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

On macOS/Linux, export the variable and use `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. Windows wrapper: `mvnw.cmd`.

- Health: `http://localhost:8080/actuator/health`
- API: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/swagger-ui.html`
- CORS default: `http://localhost:5173`

## Seed

The dev-only runner creates five accounts, 11 synthetic properties, and a two-message thread. Six properties are initially public; emergency search returns two properties. Reruns create no duplicates and preserve existing verification decisions.

All local seed accounts use `SeedDemo123!`:

| Role | Email |
| --- | --- |
| Student | `220001001@mycput.ac.za` |
| Student | `220001002@mycput.ac.za` |
| Verified landlord | `verified-landlord@seed.local` |
| Pending landlord | `pending-landlord@seed.local` |
| Admin | `admin@seed.local` |

These predictable credentials belong only to the local development profile. Production requires its own environment configuration and does not run the dev seed automatically.

## Test/package

```sh
mvn verify
java -jar target/cput-home-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Tests use H2 without requiring MySQL. Live browser tests in the root repo additionally exercise MySQL and migrations. All schema changes use Flyway, followed by JPA validation; IDs are numeric Long and public cards are projections from normalized property/room rows.

## Reset local demo

Stop the API first. For a dedicated disposable `cput_home` database, run this through a local MySQL client and restart the dev profile to migrate/reseed:

```sql
DROP DATABASE cput_home;
CREATE DATABASE cput_home;
```

The optional `db-reset-demo.sh` prompts twice and only accepts a loopback JDBC URL. Its shell runtime has not been verified on this Windows host; use the explicit SQL procedure above when appropriate. Resetting the browser does not reset MySQL.

See the root README and `docs/backend/` for frontend contracts, demo steps and verification evidence.
