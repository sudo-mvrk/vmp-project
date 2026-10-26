# TODO

Status legend: `[ ]` not started, `[~]` in progress, `[x]` done, `[-]` decided not to build.
Rule: finish one slice before starting the next. Run `./mvnw clean verify` and commit before ticking.

Slice order is the one in `docs/PLAN.md`, approved on 2026-10-08: `0 → 6a → 6b → 1 → 2 → 3 → 4 → 5 → 6c → 5b → 6d → 7 → 8 → 8b → 9`. Slice 8b was added on 2026-10-17.
Rules are in `CLAUDE.md` (repo root), decisions in `docs/ANALYSIS.md` section 6, the scoring spec in `docs/FUNCTION.md`.

## Next session: start here
State at the end of 2026-10-25. Rewrite this section at the end of every session.

Read first: `CLAUDE.md` (loaded automatically), this file, then for the slice at hand its entry in `docs/PLAN.md` and the decisions in `docs/ANALYSIS.md` section 6. `docs/FUNCTION.md` only when touching scoring.

Where things stand:
- Done: all MVP backend slices (0, 6a, 6b, 1, 2, 3, 4, 5, 6c, 5b, 7, 8, 8b, 9), the **evaluation** (Q41 … Q53) and frontend Slice **F0**. 6d stays skipped (Q18). `cd backend && ./mvnw clean verify` is green with 353 tests (the evaluation runner is excluded from it).
- Monorepo since Q40: code in `backend/` (paths `src/main/...` in older `docs/` texts mean `backend/src/main/...`), Maven runs there; `docker compose`, `.env` and `docs/` are at the root. `CLAUDE.md` = root file + `backend/CLAUDE.md`.
- Git: `main` of `git@github.com:sudo-mvrk/vmp-project.git`, pushed up to `0bf061c` (F1 done, pushed 2026-10-25); anything after that is local until the developer says "push it" (check with `git status -sb`).

Evaluation (finished 2026-10-25):
- Files in `docs/evaluation/` (**local only**, in `.gitignore` at the developer's request; never commit them): `METHOD.md` (aim, methods, metric formulas M1 … M10, parameters, threats to validity), `RESULTS.md` (generated tables and facts, plain Markdown for pandoc or for Claude to draft the thesis chapter), `runs.csv` (raw numbers per run).
- Re-run: `cd backend && ./mvnw test -Pevaluation` (about 1 minute, Docker needed). Same numbers on any day: the data is generated as of the fixed time 2026-11-02 08:00. Changing scales, runs or weight sets = constants in `EvaluationRunnerTest`.
- Scales S 30/10, M-scarce 120/60, M 200/40, L 1000/150 (volunteers/activities); weight sensitivity at M-scarce (Q53).
- Results: smart beats random and nearest on skill coverage in every run of every scale (L: 0.36 smart-A, 0.51 smart-B vs 0.17) and random on distance; smart-A is farther than nearest. Under scarcity smart fills better where it matters (M-scarce: priority-weighted fill 0.93 vs 0.91 / 0.90; HIGH + URGENT 0.97 smart-A, 1.00 smart-B vs 0.95 / 0.93; fill rate 0.91 smart-A vs 0.90). Skill-heavy weights give smart-A the best fill (0.92) and priority fill (0.95). Weak points to state honestly: smart-B lower participation (0.80 vs 0.91) and slightly lower fill at M-scarce; at M and L every method fills everything; seed skills are random (Q52), so absolute skill coverage is low.

Frontend (started 2026-10-25, decisions Q54 … Q75, slices F0 … F5 in `docs/PLAN.md`, approved 2026-10-25):
- **F0 done**: `frontend/` with React + Vite + TypeScript 5.9, Mantine, React Router, TanStack Query; Vite proxy; `npm run gen:api`; `api()` wrapper with CSRF; register, login, logout, role guards, menu per role, placeholder pages for F2 … F5. Rules and commands in `frontend/CLAUDE.md`. The developer ran it from IntelliJ + `npm run dev` on 2026-10-25 ("все работает").
- **F1 done** (Q67 … Q75): backend `common.config.OpenApiConfig` (response fields required, `@Schema(nullable = true)` on fields that can be null, nullable `$ref` wrapped in `allOf`, `LocalTime` as a string) with `OpenApiDocumentIntegrationTest`; volunteer profile page `/volunteer/profile` with four tabs (basic + Leaflet map picker with radius circle, skills, certifications, availability), each with its own PUT. Checked through the Vite proxy against the dev backend with curl (all four PUTs, overlap 400, lat without lon 400, radius 600 400; account `f1vol` / `pass1` was left in the dev database). Not clicked through in a browser by Claude.
- Done checks: `cd frontend && npm run lint && npm run build && npm test` (34 tests); `cd backend && ./mvnw clean verify` (353 tests).

Next session, in this order:
1. ~~Finish the rename~~ done 2026-10-26: main class `com.volunnear.VmpProject`, artifact and jar `vmp-project-0.0.1-SNAPSHOT.jar` (Dockerfile updated), test `VmpProjectTests`.
2. Slice F2 (public activities, recommendations with the score breakdown, apply) per `docs/PLAN.md`; ask its edge cases first.
3. If the developer wants: draft the thesis chapter from `docs/evaluation/METHOD.md` + `RESULTS.md` (text only; the files stay local).

Local machine (not code), 2026-10-25: the old compose project `volunnearapp` is stopped, `vmp-project` postgres and redis run, the new dev database is migrated and seeded. IntelliJ: `backend/pom.xml` was added as a Maven project with a Spring Boot run configuration (steps in `docs/LOCAL_NOTES.md`); the developer confirmed the app runs. The app does not start without `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH` (Q28).

How the developer works:
- Says "lets move to slice X" to start a slice; the plan is approved, so no new plan is needed for a slice that follows `docs/PLAN.md`.
- Edge cases the spec does not settle are asked before coding, with a recommendation (see Q13a, Q13b, Q14a, Q1a, Q1b, Q2a … Q2d, Q3a … Q3h, Q4a … Q4e, Q5a … Q5m, Q6a … Q6f). Small implementation choices are made and reported.
- Commits follow the existing style: `feat/ ...`, `ref/ ...`, `docs/ ...`, one logical step per commit.

What exists in code, so it is not rebuilt:
- Packages under `com.volunnear`: `auth`, `organization`, `volunteer`, `dictionary` (Skill), `activity`, `application`, `common` (`config`, `exception`, `validation`), `distribution` (pure: `scoring`, `model`, `feasibility`, `ranking`; wiring: `adapter` (`ProfileAdapter`, `ProfileLoader`), `dto`, `RecommendationService`, `RecommendationController`).
- The pure `distribution` subpackages have no Spring or JPA; only `adapter` touches entities. Later slices must not change the scoring or feasibility rules.
- Schema: `V1__baseline.sql` (`app_users`, `user_role`, `volunteers`, `organizations`, `skills`) , `V2__organization_own_id_and_members.sql` (organization identity id, `organization_member` with unique `user_id` and one `OWNER` per organization) `V3__volunteer_profile.sql` (volunteer `bio`, `max_active_assignments`, radius check; `certifications` dictionary; `volunteer_skill`, `volunteer_certification`, `volunteer_availability`; seeded skills and certifications) and `V4__activities.sql` (`activities` with weight override columns, `activity_required_skill`, `activity_required_certification`) and `V5__applications.sql` (`applications` with status, origin, `version`, unique volunteer + activity) and `V6__invitations.sql` (status `DECLINED`, origin `INVITATION`). There are no activity or application tables or entities; Slices 4 and 5 create them. Every schema change is a new migration.
- Auth (Slice 1): no session → 401 (`HttpStatusEntryPoint`); wrong role → 403 from `SecurityConfig` (`/volunteers/**` needs VOLUNTEER, non-GET `/organizations/**` needs ORGANIZATION); banned user → login 401 "Account is banned" (`isAccountNonLocked`); missing profile of a logged-in user → 404 (`DataNotFoundException`); `GET /api/v1/auth/csrf` returns the token (Q1a).
- Tests: integration tests extend `support.AbstractIntegrationTest` (PostGIS in Testcontainers, in-memory sessions, `MockMvc`) and send `.with(csrf())` on every modifying request. `auth.SecurityRulesIntegrationTest` logs in for real and reuses the `SESSION` cookie; copy its helpers for later ownership tests. Distribution tests build data with `distribution.Profiles`.
- Test pitfall: `csrf()` permanently swaps the shared `CsrfFilter` to a session repository, so a test of the real cookie flow needs a fresh context (see `auth.CsrfEndpointIntegrationTest`).
- Organizations (Slice 2): `Organization` has its own id; accounts link through `OrganizationMember` (plain ids, `@EmbeddedId`). `OrganizationAccess.canManage(userId, organizationId)` is the one ownership check and `organizationIdOf(userId)` finds the caller's organization; Slice 4 is its first real caller (add a `requireManage` that throws Spring's `AccessDeniedException` → 403 there). Endpoints: public `GET /organizations/{id}`, `GET` and `PUT /organizations/me` (ORGANIZATION role; own view adds `username`, Q3b). Update is partial; lat/lon both or neither (Q2b).
- Integration tests: `AbstractIntegrationTest.register(...)` and `login(...)` use the real endpoints and return the `SESSION` cookie. `migration.OrganizationMemberBackfillMigrationTest` shows how to test a migration against old data in a scratch database.

- Volunteers (Slice 3): `GET`/`PUT /volunteers/me` (partial basic fields, capacity 0 … 10, radius 1 … 500 km), `PUT /volunteers/me/skills`, `/certifications`, `/availability` replace a whole list (Q3c). Public `GET /api/v1/skills` and `/api/v1/certifications`. `DictionaryService.requireSkills/requireCertifications` turn ids into entities and answer 400 for unknown ids; reuse them for activity requirements. `AvailabilityRules` rejects overlapping slots. Responses: `VolunteerProfileResponseDto` has no account data (use it for views by organizations in Slices 5 and 6c); `OwnVolunteerProfileResponseDto` adds `username` with `@JsonUnwrapped`.
- Integration tests that check what reached the database call `entityManager.flush()` and `clear()`: the test transaction never commits (see `VolunteerProfileIntegrationTest`).

- Activities (Slice 4): `Activity` entity with status methods `publish()`, `cancel()`, `requireEditable()` (409 via `ConflictException`); `ActivityWindowRules` (same day, start in the future); `ActivityService` checks ownership with `OrganizationAccess.requireManage` (403). Endpoints: `POST /activities`, `PUT /activities/{id}` (full replacement), `POST /activities/{id}/publish` and `/cancel`, public `GET /activities` (open, upcoming, paged) and `GET /activities/{id}` (drafts only for the owner), `GET /organizations/me/activities`.
- Time: `volunnear.time-zone` in `VolunnearProperties` (validated at startup) and a `Clock` bean in that zone. Use `LocalDateTime.now(clock)`, never `now()`. Times are truncated to minutes.
- `src/test/resources/application.yml` replaces the main one in tests: every new required property must be added there too.
- `ScoringWeights.requireOnSimplex(alpha, beta, gamma)` is the one weight rule; the activity override uses it.

- Applications (Slice 5): `ActivityApplication` holds the transition table (409); `ApplicationService.accept` locks activity then volunteer (`findByIdForUpdate`, always this order) and re-checks open/not started, free spot (`ActivityRepository.countAccepted`), capacity (`ApplicationRepository.findActiveAssignments`, Q16) and time overlap (`TimeWindow.overlaps`). `@Version` on the application stops a concurrent accept and withdraw from both winning; `DataIntegrityViolationException` and optimistic-lock failures are answered with 409. `PUT /activities/{id}` takes the same activity lock and refuses `spotsNeeded` below the accepted count.
- Endpoints: `POST /activities/{id}/applications` (volunteer), `GET /activities/{id}/applications` (owner), `POST /applications/{id}/accept|reject` (owner), `POST /applications/{id}/withdraw` (applicant), `GET /volunteers/me/applications`.
- Concurrency tests: `ApplicationConcurrencyIntegrationTest` is not `@Transactional`, commits real data with a random name prefix and deletes it in `@AfterEach`; `runAtOnce` starts tasks together. Checked on 2026-10-17 that removing the row locks makes two of its tests fail. Reuse it for 5b.

- Recommendations (Slice 6c): `distribution.adapter.ProfileAdapter` (static, pure mapping entity → `VolunteerProfile`/`ActivityProfile`; the caller loads active assignments, applied ids and the accepted count). `distribution.RecommendationService` loads candidates in bulk (`VolunteerRepository.findRecommendationCandidates`, `ApplicationRepository.findActiveAssignmentsOfVolunteers` and `findAppliedActivities`, `ActivityRepository.countAcceptedByActivity` and the native `findOpenIdsWithin` with `ST_DWithin`) and calls `Ranker`. `Volunteer` collections have `@BatchSize(50)`.
- Endpoints: `GET /activities/{id}/recommended-volunteers` (owner, ORGANIZATION; 409 if not OPEN or started) and `GET /volunteers/me/recommended-activities`; `limit` 1 … 50, default 10.
- Weights: `volunnear.scoring.alpha|beta|gamma|half-distance-km` in `VolunnearProperties.Scoring` (both `application.yml` files); its constructor applies `ScoringWeights.ofHalfDistance`, so bad values stop the start (`ScoringPropertiesStartupTest` uses `ApplicationContextRunner`). `scoring().weights(a, b, g)` builds an override with the global half-distance.
- Test pitfall: JsonPath returns long decimals as `BigDecimal`; use `jsonPath(...).value(closeTo(x, eps), Double.class)`.

- Invitations (Slice 5b): an `ActivityApplication` with origin `INVITATION`; the entity holds which side decides (`requireOrigin`, `decline`, withdraw rule). `ApplicationService.invite` loads both profiles with `ProfileLoader` (shared with recommendations) and checks the `Feasibility`/`PlatformEligibility` predicates one by one for named 409s. `accept` and `acceptInvitation` share `acceptUnderLocks`. Endpoints: `POST /activities/{id}/invitations`, `POST /invitations/{id}/accept|decline`.
- `ApplicationConcurrencyIntegrationTest` has an invitation case; checked on 2026-10-18 that it fails without the row locks.

- Notifications (Slice 7): `notification.Notifier` is called by `ApplicationService` and `ActivityService.cancel` inside their transactions; it reads the recipient (volunteer `AppUser.email`, organization owner via `OrganizationMemberRepository.findOwnerEmail`), builds the text and publishes a `Notification` record as a Spring event. `NotificationDispatcher` (`@TransactionalEventListener(AFTER_COMMIT)`) hands it to `NotificationSender` (only `LoggingNotificationSender`) and logs a failure. A missing email skips with a warning (Q7c). `ActivityApplicationResponseDto.volunteerEmail` is set only for `ACCEPTED` rows (Q7d). `spring-boot-starter-mail` is removed (Q7e).
- `NotificationIntegrationTest` is not `@Transactional` (AFTER_COMMIT needs a real commit) and replaces the sender with `@MockBean`, so it runs in its own Spring context. Checked on 2026-10-23 that a plain `@EventListener` makes its rollback test fail.

- Completion and history (Slice 8): `Activity.complete()` (OPEN only, 409) and `ActivityService.complete` (owner, started, Q8a); `POST /activities/{id}/complete`. `cancel` and `complete` both take the activity row lock (`findManagedForUpdate`), so they cannot overwrite each other; `ApplicationConcurrencyIntegrationTest` has the case and was checked on 2026-10-23 to fail without the lock. Completion notifies accepted volunteers (`Notifier.activityCompleted`, Q8d). History: `GET /volunteers/me/history` (`ApplicationRepository.findHistory`, `HistoryItemResponseDto`, Q8e). Capacity release needs no code (Q16); `CompletionIntegrationTest` proves it.

- Administration (Slice 8b): package `admin` (`AdminService`, `AdminController`, `/api/v1/admin/**` only for `ROLE_ADMIN`, Q32). `V7__admin.sql` widens the role check and inserts the first admin from Flyway placeholders (`spring.flyway.placeholders.*` in both `application.yml` files; tests log in as `admin` / `adminpass1`). Ban: `AdminService.ban` sets the flag, calls `ApplicationService.releaseBannedVolunteer` (Q25, volunteer lock) or `ActivityService.cancelUpcomingOf` (Q26), and publishes `auth.UserBanned`; `auth.UserSessions` deletes the sessions after commit through `FindByIndexNameSessionRepository` (`spring.session.redis.repository-type: indexed`). Admin cancel: `ActivityService.cancelAsAdmin` (Q31). User search with a JPA `Specification` (Q29).
- Tests: `TestSessionConfig` now provides `support.IndexedMapSessionRepository` (in-memory, findable by username). `AdminIntegrationTest` is not `@Transactional` and shares the `@MockBean NotificationSender` context with `NotificationIntegrationTest`. `admin.BanEndsRedisSessionsIntegrationTest` runs its own context against a Redis Testcontainer (`redis:7.4-alpine`) with the production session setup; `AbstractIntegrationTest.POSTGRES` is public for it. Checked on 2026-10-23 that both ban tests fail when the sessions are not deleted.

- Seed and demo (Slice 9): package `seed` (`SeedDataLoader`, an `ApplicationRunner` under `@Profile("seed")`, writes the repositories; `SeedProperties` = `volunnear.seed.*`; `ActivityTemplate`). Fixed random seed, times relative to now, skips if `seed_org1` exists (Q33 … Q37). Demo guarantees: the first upcoming activity (lowest id, "Sort food donations") always has `seed_vol08` … `seed_vol12` available (full-day slot on its weekday, radius 50); `seed_vol05`/`06` are accepted on the started activity, `seed_vol01`/`02`/`07` have the completed one in their history. `docs/DEMO.md` was run end to end on 2026-10-23 against a throwaway PostGIS and Redis (all steps and all five notification types worked).
- `SeedDataIntegrationTest` runs with the `seed` profile in its own context, so the loader commits into the shared test database; it deletes the seed data in `@AfterAll`.

Hints for the next step:
- Evaluation (package `com.volunnear.evaluation` in the test sources): `Simulation` (volunteer-driven rounds for random, nearest, smart-A; activity-driven for smart-B), `Metrics` (M1 … M10), `ListQuality` (k = 5, 10), `Summary`, `Evaluation` (100 paired runs), `ReportWriter` (Markdown and CSV), and `EvaluationRunnerTest` (`@Tag("evaluation")`, seeds each scale by building `SeedDataLoader` by hand inside a `TransactionTemplate`, loads with `ProfileLoader`, asserts the database holds only the seed data). Scales and weight sets are constants in the runner. The surefire `excludedGroups` comes from the property `evaluation.excludedGroups`; the Maven profile `evaluation` switches it.

## Phase A: Analysis and plan (no feature code yet)
- [x] Read `docs/FUNCTION.md` and any existing code or docs in the repo
- [x] Write `docs/ANALYSIS.md`: entities, relations, scoring inputs, open questions
- [x] Write `docs/PLAN.md`: confirmed slice order, risks, decisions needed from the developer
- [x] Developer answers Q-A … Q-H and Q1 … Q20; recorded in `docs/ANALYSIS.md` section 6
- [x] Developer reviews and approves the plan (approved 2026-10-08)

## Phase B: Baseline and scoring core
- [x] Slice 0: Stabilise the existing project: commit current work, package by feature, Flyway baseline, Testcontainers instead of H2, fix the 8 failing auth tests, health endpoint, remove dead files and unused dependencies
- [x] Slice 6a: Pure scoring code from `FUNCTION.md` with hand-computed unit tests, including weight validation
- [x] Slice 6b: Feasibility filter (availability, certification, capacity) and ranking with tie-break, with tests

## Phase C: Platform
- [x] Slice 1: Auth hardening (register and login exist; role rules, banned users, error mapping, tests)
- [x] Slice 2: Organizations (profile exists; read endpoints, ownership tests; own id instead of `@MapsId`, `organization_member` with one `OWNER` row, single access-check method)
- [x] Slice 3: Volunteer profile (location, skills, certifications, availability, capacity)
- [x] Slice 4: Activities, rebuilt (location, time, required skills and certs, priority, spots needed)
- [x] Slice 5: Applications (apply, accept, reject, withdraw; spot and capacity checks in one transaction; concurrency test)

## Phase D: Smart Distribution integration
- [x] Slice 6c: Entity to domain adapter, weights alpha, beta, gamma, lambda in `application.yml` validated at startup, recommendation endpoints (activity -> volunteers, volunteer -> activities) with score breakdown
- [x] Slice 5b: Invitations (organization invites from the recommendation list; volunteer accepts or declines)
- [-] Slice 6d: Optional batch auto-assign (greedy first), organization reviews result. Not built for the MVP (Q18, 2026-10-18)

## Phase E: Finish
- [x] Slice 7: Email notifications (log stub first; Q7a … Q7f)
- [x] Slice 8: Completion and volunteer history (Q8a … Q8h)
- [x] Slice 8b: Platform administration (separate `ROLE_ADMIN` accounts, first admin from a Flyway migration with env credentials, ban deletes Redis sessions, no audit log; Q21 … Q32)
- [x] Slice 9: Seed data and five-minute demo script (Q33 … Q39, `docs/DEMO.md`)
- [x] Evaluation: compare recommendations against a simple baseline on seed data (for the thesis); run on 2026-10-25 with M-scarce (Q51)

## Phase F: Frontend (Q54 … Q66, `docs/PLAN.md` section 2 "Frontend")
- [x] Slice F0: Scaffold, API client with CSRF, auth (register, login, logout), role guards, `frontend/CLAUDE.md` (2026-10-25)
- [x] Slice F1: Volunteer profile (map picker, skills, certifications, availability), required response fields in the OpenAPI document (Q67 … Q75, 2026-10-25)
- [ ] Slice F2: Activities and recommendations for volunteers (score breakdown, apply)
- [ ] Slice F3: Organization profile and activities
- [ ] Slice F4: Applications, recommended volunteers and invitations for organizations
- [ ] Slice F5: Volunteer applications, invitations and history

## Discovered work
- [x] Fix `lat == 0` treated as "no location" and lost location on partial update in both mappers (organization Slice 2, volunteer Slice 3)
- [x] `.gitignore`: `docs/` line is gone; `redisdata/` added (Slice 0)
- [x] Move `CLAUDE.md` from `docs/` to the repo root so Claude Code loads it (Slice 0)
- [x] `CLAUDE.md`: compose service name is `postgres`, not `db` (Slice 0)
- [x] Local dev database recreated on 2026-10-08; the app starts against it, Flyway applies V1 and `/actuator/health` returns 200 (Slice 0)
- [ ] `testcontainers.version` is pinned to 1.21.4 in `pom.xml` because the version managed by Boot 3.3.3 cannot talk to Docker 29; drop the pin when Spring Boot is upgraded
- [x] Registration, login and logout need a CSRF token: `GET /api/v1/auth/csrf` now serves it (Q1a, Slice 1)
- [x] Unused leftovers removed: `TokenRefreshException`, `AuthErrorException`, `BadUserCredentialsException` (Slice 1), `spring-boot-starter-mail` (Slice 7), `ValidPhoneNumber` and the RestTemplate/apache `logging.level` block (Slice 9)
- [x] `docker-compose.yml` uses `.env` for every service: required-variable checks, Redis really requires `REDIS_PASSWORD`, the app container gets `.env` via `env_file`; `.env.example` and `.dockerignore` added (2026-10-08)
- [x] `Dockerfile` no longer copies `application.yml` or forces profile `dev`; profiles come from `SPRING_PROFILES_ACTIVE` (Q39, Slice 9)
- [x] `ApplicationStatus` renamed to `PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN` (Q5a, Slice 5)
- [x] `VolunteerProfile` / `ActivityProfile` records exist in `distribution.model` (Slice 6b)
- [x] `Ranker` takes one `ScoringWeights` per call: the caller passes the activity's override for activity → volunteers and the global weights for volunteer → activities (Slice 6c)
- [x] Adapter contract for `VolunteerProfile` (Slice 6c): `activeAssignments` = windows of accepted applications on activities that are not completed or cancelled (its size is the used capacity, Q16); `appliedActivityIds` = every activity with an application or invitation from this volunteer; times converted to the one configured zone
- [x] A slot cannot end at 24:00 and an activity ending at midnight counts as multi-day: slots documented in Q3d (Slice 3), activities rejected with a clear message in Q4d (Slice 4)
- [x] The adapter must map the `Priority` enum as it is; the numeric scale 1..4 lives only in `ScoringFunction.priorityValue` (Slice 6c)
- [x] Per-activity weight override: stored and validated in Slice 4; used only for activity → volunteers in Slice 6c
- [x] PostGIS radius prefilter for volunteer → activities (Slice 6c, Q6d)
- [x] Deleting a user no longer deletes its organization (V2 dropped the shared-key cascade). Settled by Q31: nothing is deleted; an organization is deactivated by banning its owner (Slice 8b)
- [x] `Volunteer` had no `bio` column and the response DTO had the typo `datoOfBirth` (fixed in Slice 3, Q3h)
- [x] A ban does not end the sessions the user already has: the ban now deletes them after commit (Q23, Slice 8b)
- [ ] A login that has already passed the password check when a ban commits can still save a new session after the sessions were deleted (a window of milliseconds). Closing it needs a per-request ban check; accepted for the MVP
- [ ] Suspected, not verified: `ApplicationService.acceptUnderLocks` reads `application.getActivity()` before `findByIdForUpdate`, so the lock query returns the already loaded entity without refreshing it, and `requireOpenAndNotStarted` may see a status from before the lock (e.g. a cancel that committed in between). Spots and capacity are safe (they are queries). Check with a test; fix with `entityManager.refresh(activity, PESSIMISTIC_WRITE)` or by locking before the activity is loaded
- [ ] Recommendation candidates are passed to `IN (...)` queries; PostgreSQL allows at most 32767 bind parameters, so above ~30k volunteers the activity → volunteers query must be chunked or turned into a join (after the evaluation, if ever)
- [ ] Login of a banned user answers "Account is banned" even with a wrong password (Spring checks the lock before the password), so it reveals that the account exists and is banned. Accepted for the MVP

- [x] Generated API types: every response field was optional (springdoc); fixed by Q67 in F1
- [ ] A new nullable component of a response record needs `@Schema(nullable = true)` by hand, or the generated type claims it is never null (Q67); the OpenAPI test checks only a few fields
- [ ] The production bundle is one chunk of ~640 kB (195 kB gzip; Vite warns above 500 kB). Fine for the MVP; split by route with `lazy` if it matters
- [ ] `frontend/public/favicon.svg` is still the Vite logo

- [ ] Completion is manual only (Q8g): an activity the organization never completes keeps its accepted volunteers' capacity used. Possible later fix: a scheduled job or an "ended" filter in the capacity query (would change Q16)
- [x] `cancel` did not lock the activity row; with `complete` added it could lose an update. Both now take the lock (Slice 8)

## Open questions
- None open. F2 edge cases are asked at its start.
