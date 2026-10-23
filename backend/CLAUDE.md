# backend/CLAUDE.md

Backend of the monorepo. Project rules, Smart Distribution rules, MVP scope and the working agreement are in the root `CLAUDE.md`.

## Stack
- Java 21, Spring Boot 3.x, Maven (use `./mvnw` in `backend/`)
- Spring Web, Spring Security (sessions in Redis, CSRF), Spring Data JPA, Bean Validation
- PostgreSQL (Docker Compose for local), Flyway for all schema changes
- JUnit 5, Mockito, Testcontainers for integration tests
- springdoc-openapi for API docs

## Commands
Maven runs in `backend/`; `docker compose` and `.env` are at the repo root.
- Build and test: `cd backend && ./mvnw clean verify` (integration tests start PostgreSQL in Testcontainers, so Docker must be running)
- Run tests only: `cd backend && ./mvnw test`
- Start local DB and Redis (repo root): `docker compose up -d postgres redis`
- Run app: `cd backend && set -a; . ../.env; set +a; ./mvnw spring-boot:run` (`.env` is ignored by git; `.env.example` lists the variables)
- Run everything in containers: `(cd backend && ./mvnw package -DskipTests) && docker compose up -d --build`
- Run with demo data: `SPRING_PROFILES_ACTIVE=seed` in the environment (see `docs/DEMO.md`)

(Update this section if the commands change.)

## Architecture and conventions
- Package by feature under the base package `com.volunnear`: `auth`, `organization`, `volunteer`, `activity`, `application`, `distribution`, `notification`, `admin`. Shared code: `dictionary` (skills and certifications) and `common` (`config`, `exception`). `seed` holds the demo data loader (profile `seed` only). DTOs of a feature live in its `dto` subpackage.
- Integration tests extend `support.AbstractIntegrationTest` (shared PostGIS container, in-memory sessions) and send a CSRF token on every modifying request. The `csrf()` post-processor permanently swaps the shared CSRF repository for a session-based one, so a test of the real cookie flow needs its own context and must not use `csrf()`.
- Layers per feature: `controller` -> `service` -> `repository`. No business logic in controllers.
- Controllers use DTOs (records). Never expose JPA entities in the API.
- Every schema change is a new Flyway migration. Never edit an applied migration.
- Authorization is checked in the service layer: a caller may only modify resources of their own organization or profile. Add a test for each rule.
- Multi-step state changes (accept application, check free spots and volunteer capacity) run in one `@Transactional` method and must be safe against concurrent requests.
- Constructor injection only. No field `@Autowired`.
- Configuration through `application.yml` and `@ConfigurationProperties`, not hard-coded constants.
