# VolunNearApp

Volunteer management platform with **Smart Distribution**: it recommends and assigns volunteers to activities using a multi-attribute scoring function (distance, skills, priority). The function is specified in `docs/FUNCTION.md`.

This branch (`mvp-reboot`) is the MVP rewrite of the bachelor project. It is API first; there is no frontend.

## Stack

Java 21, Spring Boot 3.3, Maven wrapper, PostgreSQL with PostGIS, Flyway, Redis (HTTP sessions), Testcontainers.

## Run locally

1. Create `.env` in the repo root: `cp .env.example .env` and fill in the values. `.env` is ignored by git.

   | Name | Value |
   |--|--|
   | `POSTGRE_DB` | database name |
   | `DB_USERNAME` | database user |
   | `DB_PASSWORD` | database password |
   | `DB_URL` | JDBC url for running on the host: `jdbc:postgresql://localhost:5432/<POSTGRE_DB>` |
   | `REDIS_PASSWORD` | Redis password (Redis is started with `--requirepass`) |

2. Start PostgreSQL and Redis from the repo root: `docker compose up -d postgres redis`. Compose reads `.env` by itself.
3. Start the application on the host with the same variables: `cd backend && set -a; . ../.env; set +a; ./mvnw spring-boot:run`

To run the application in a container instead of step 3: `(cd backend && ./mvnw package -DskipTests) && docker compose up -d --build`. The app service gets `.env` through `env_file`; compose overrides the database and Redis hosts with the service names.

Flyway creates the schema on startup (`src/main/resources/db/migration`). Hibernate only validates it.

- Health: http://localhost:8080/actuator/health
- API docs: http://localhost:8080/swagger-ui/index.html

Authentication uses a session cookie (`SESSION`) and a CSRF token (`XSRF-TOKEN` cookie, sent back in the `X-XSRF-TOKEN` header on every modifying request).

## Build and test

`cd backend && ./mvnw clean verify`

Integration tests start `postgis/postgis` in Testcontainers, so Docker must be running. They do not need Redis or `.env`.

## Documentation

- `CLAUDE.md`: project rules and conventions
- `docs/FUNCTION.md`: the scoring function
- `docs/ANALYSIS.md`: analysis and decisions
- `docs/PLAN.md`: slices with acceptance criteria
- `docs/TODO.md`: progress
