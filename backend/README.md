# CPUT Home backend (Java 21, Spring Boot, Maven)
# run from this directory. never commit real secrets.

## develop (mysql must be running, see compose.dev.yml at repo root)

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

health: GET http://localhost:8080/actuator/health
docs: http://localhost:8080/swagger-ui.html (dev only)

## demo seed (dev profile only, never production)

the first dev boot stocks 11 sample properties, demo accounts and one
message thread. reruns change nothing.

development-only logins (share freely inside the team, never in production):

| role | email | password |
| --- | --- | --- |
| student | `220001001@mycput.ac.za` | `SeedDemo123!` |
| student | `220001002@mycput.ac.za` | `SeedDemo123!` |
| landlord (verified) | `verified-landlord@seed.local` | `SeedDemo123!` |
| landlord (pending) | `pending-landlord@seed.local` | `SeedDemo123!` |
| admin | `admin@seed.local` | `SeedDemo123!` |

for a public demo VM, seed locked records instead or distribute strong
generated credentials privately.

## reset local demo data

`DB_URL=... DB_USERNAME=... DB_PASSWORD=... ./backend/db-reset-demo.sh`
asks twice and refuses non-localhost urls. restart the backend after to
migrate + reseed.

## test (h2, no mysql needed)

./mvnw test

## package

./mvnw package
java -jar target/cput-home-backend-0.0.1-SNAPSHOT.jar
