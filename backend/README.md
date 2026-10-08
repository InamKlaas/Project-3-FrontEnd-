# CPUT Home backend (Java 21, Spring Boot, Maven)
# run from this directory. never commit real secrets.

## develop (mysql must be running, see compose.dev.yml at repo root)

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

health: GET http://localhost:8080/actuator/health
docs: http://localhost:8080/swagger-ui.html (dev only)

## test (h2, no mysql needed)

./mvnw test

## package

./mvnw package
java -jar target/cput-home-backend-0.0.1-SNAPSHOT.jar
