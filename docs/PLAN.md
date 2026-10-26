# PLAN

Phase A output. Date: 2026-10-07. Status: **all questions answered on 2026-10-07; plan updated to the decisions, waiting for your approval.**
Background is in `docs/ANALYSIS.md`; the decisions (Q…) are in its section 6.

## 1. Changes to the slice order in `TODO.md`

The slices are confirmed with four adjustments:

1. **Slice 0 becomes "stabilise the baseline", not "create a skeleton".** A Spring Boot project already exists, with working auth and profiles (Q-A).
2. **Slices 6a and 6b move directly after Slice 0.** The scoring code is pure and depends on nothing in the platform. More important, its answers decide the schema of Slices 3 and 4 (skill levels, availability shape, capacity). Building the tables first and the function second risks migrations that have to be redone.
3. **Slices 1 and 2 shrink** to hardening work, because register, login, logout and profile update already exist.
4. **Slice 5b (invitations) is added** after 6c. Invitations are in the MVP scope in `CLAUDE.md` but had no slice. The standalone "weights in `application.yml`" item is folded into 6a (validation) and 6c (Spring binding).

Proposed order:

```
0 → 6a → 6b → 1 → 2 → 3 → 4 → 5 → 6c → 5b → 6d (optional) → 7 → 8 → 8b → 9 → evaluation
```

Slice 8b (platform administration) was added on 2026-10-17 at the developer's request, after the plan was approved.

Slice ids are kept so existing references stay valid.

Frontend slices F0 … F5 (section 2, "Frontend") were proposed on 2026-10-25 after Q54 … Q66 and approved the same day. They follow the evaluation: `F0 → F1 → F2 → F3 → F4 → F5`.

## 2. Slices

Every slice ends with `./mvnw clean verify` green and a commit. "IT" means an integration test against PostgreSQL in Testcontainers.

### Slice 0: Stabilise the baseline
Work: commit or stash the current working tree; delete the broken activity code (rebuilt in Slice 4); move to package-by-feature under `com.volunnear` (Q-D); add Flyway with `V1__baseline.sql` for users, volunteers, organizations and skills, and set `ddl-auto: validate`; replace H2 with Testcontainers (`postgis/postgis`); fix the 8 failing auth tests; re-enable the context-load test; remove `roles.sql`, `pom.xml.versionsBackup` and the unused dependencies (websocket, aop, openfeign with `NominatimClient`; Q-G); remove the `docs/` line from `.gitignore`, add `redisdata/`, and move `CLAUDE.md` to the repo root (Q-H); update README and the Commands section of `CLAUDE.md`.

Acceptance:
- `./mvnw clean verify` passes with zero failures on a machine with Docker.
- An empty database is brought to the full schema by Flyway alone; Hibernate validation passes.
- `GET /actuator/health` returns 200 without authentication.
- No behaviour change in auth or profile endpoints.

Tests: context loads (IT); existing 17 tests green; health endpoint (IT).

### Slice 6a: Pure scoring
Work: package `distribution.scoring` with records `GeoPoint`, `ScoringWeights`, `ScoreBreakdown` and pure functions for haversine (km), `uGeo`, `uSkill`, `uPrio`, `score`. `uSkill` is the cosine projected onto the required skills (Q10); skill vectors are doubles, filled with 0/1 for now (Q7). `uPrio` uses the fixed scale bounds 1 and 4 (Q8). `ScoringWeights` can be built from a half-distance (Q4). No Spring, no JPA imports.

Acceptance:
- Every component and the total are in `[0,1]` for all valid inputs.
- `ScoringWeights` rejects negative weights, a sum other than 1 (tolerance `1e-9`) and `λ ≤ 0`.
- The result always carries geo, skill, priority, total and the weights that were used (Q12).

Tests (unit, hand-computed):
- Reference case: `λ = 0.1 /km`, `d = 5 km` → `u_geo = e^-0.5 = 0.6065`; `S_v = (1,1,0)`, `S_t = (1,0,1)` → projected `S_v = (1,0,0)`, `u_skill = 1/√2 = 0.7071`; priority `HIGH` (3) on the scale 1..4 → `u_prio = 2/3 = 0.6667`; weights `(0.4, 0.4, 0.2)` → `R = 0.2426 + 0.2828 + 0.1333 = 0.6588`.
- Half-distance 10 km → `λ = ln 2 / 10 = 0.0693`, and `u_geo(10 km) = 0.5`.
- `u_geo`: `d = 0` → 1; very large `d` → close to 0 and not negative; missing location → 0 (Q5).
- Haversine against one known city pair, and identical points → 0.
- `u_skill`: identical vectors → 1; disjoint → 0; no skills required → 1, none of the required skills → 0 (Q9); the "extra skills" case from ANALYSIS 3.2 → 1 for both volunteers (Q10); 1 of 4 required skills → `sqrt(1/4) = 0.5`.
- `u_prio`: each enum value → 0, 1/3, 2/3, 1 (Q8).
- Weights: `(1,0,0)`, `(0,1,0)`, `(0,0,1)` return the single component; invalid weights throw.
- Property-style loop over random inputs asserting `0 ≤ R ≤ 1`.

### Slice 6b: Feasibility filter and ranking
Work: pure predicates `available` (weekly slots fully cover the activity window, no overlapping accepted activity; Q13), `certified` (every required certification held and not expired at the activity start; Q14), `hasCapacity` (`maxActiveAssignments` minus active accepted assignments > 0; Q16), combined into `feasible`; a separate predicate `platformEligible` (activity `OPEN`, free spots, no existing application, not banned; Q15); a ranker that filters, scores, sorts, applies the tie-break (larger `u_skill`, larger `u_geo`, lower id; Q17) and truncates to top N.

Acceptance:
- A volunteer failing any one predicate never appears in the result.
- Ordering is deterministic for equal scores.
- An empty feasible set gives an empty list.

Tests (unit): one passing and one failing case per predicate; boundary cases (slot ends exactly at activity end, certification expiring on the activity day, remaining capacity exactly 0 and 1, overlapping accepted activity); no required certifications → certified; one failing case per platform condition; all three tie-break levels; top-k truncation; empty input.

### Slice 1: Auth hardening
Work (sessions stay; Q-C): role rules in `SecurityConfig` reviewed against the endpoint list; banned users cannot log in; `UsernameNotFoundException` mapped to 401/404; registration returns 201.

Acceptance: register and login for both roles; unauthenticated → 401; wrong role → 403; banned → rejected.

Tests: IT for each of the four outcomes above; existing `AuthServiceTest`.

### Slice 2: Organizations
Work: membership basis (Q-E): a migration that gives `organization` its own generated id instead of sharing the user's id (`@MapsId` removed) and adds `organization_member (organization_id, user_id, role)`, with one `OWNER` row created at registration and backfilled for existing rows; one access-check method (for example `OrganizationAccess.canManage(userId, organizationId)`) used by every ownership rule from here on; no invite or role endpoints. Then public `GET /organizations/{id}`, `GET /organizations/me`; validation on the update DTO; fix the `lat == 0` mapper defect.

Acceptance: an organization account can read and update only its own profile; a volunteer account gets 403 on the update endpoint; every organization has exactly one `OWNER` member; no service compares user and organization ids directly.

Tests: service unit test for update; unit test for the access check (owner, other organization's owner, volunteer); IT for registration creating the `OWNER` row, own-profile update, foreign role → 403, public read.

### Slice 3: Volunteer profile
Work: migrations and endpoints for volunteer skills (held or not, no levels; Q7), certifications (new dictionary, optional `validUntil`, self-declared; Q14), weekly availability slots (day of week + time range; Q13) and `maxActiveAssignments` (Q16); `radius` stays a profile field used only by the prefilter in 6c (Q6); read-only dictionary endpoints for skills and certifications; seed dictionary rows by migration.

Acceptance: a volunteer can set and read location, skills, certifications, availability and capacity; unknown skill or certification id → 400; a volunteer cannot modify another volunteer's profile.

Tests: unit tests for service validation (overlapping slots, end before start, negative capacity); IT for the full profile round trip and for the ownership rule.

### Slice 4: Activities
Work: rebuild the activity feature: migration, entity with generated id, start/end, `spotsNeeded`, required skills and certifications, priority `NOT NULL` with default `MEDIUM` (Q8), optional weight override `α, β, γ` (Q12), status stored as string; create, update, publish (`DRAFT → OPEN`), cancel, get, list.

Acceptance: only the owning organization can create or change an activity; coordinates keep their decimals; invalid time window, a multi-day window or `spotsNeeded < 1` → 400; a weight override that is partial, negative or does not sum to 1 → 400; public list returns only `OPEN` activities.

Tests: unit tests for status transitions and validation; IT for create, update by owner, update by another organization → 403, public listing.

### Slice 5: Applications
Work: apply, withdraw (volunteer); accept, reject (organization); unique (volunteer, activity); accept runs in one `@Transactional` method that locks the activity row and the volunteer row, re-checks free spots and the calculated volunteer capacity (Q16), then updates the application.

Acceptance: free spots never drop below zero and a volunteer never exceeds capacity, including under concurrent requests; illegal status transitions → 409; only the owning organization can accept or reject; only the applicant can withdraw.

Tests: unit tests for the transition table; IT for each endpoint and each ownership rule; **concurrency IT**: an activity with 1 spot, N pending applications, N threads accepting at once → exactly one `ACCEPTED`; a second concurrency IT for volunteer capacity 1 across two activities.

### Slice 6c: Adapter and recommendation endpoints
Work: adapter class mapping entities to the scoring records; `@ConfigurationProperties` for `α, β, γ` (default 0.4 / 0.4 / 0.2) and the half-distance (default 10 km), validated at startup; `GET /activities/{id}/recommended-volunteers` (owning organization, `limit` parameter) and `GET /volunteers/me/recommended-activities`.

Rules per direction:
- Activity → volunteers: uses the activity's weight override if it has one, otherwise the global weights (Q12). The volunteer's radius is not applied (Q6).
- Volunteer → activities: PostGIS `ST_DWithin` selects `OPEN` activities within the volunteer's radius before scoring (Q6); always the global weights (Q12). A volunteer without a location or radius gets no prefilter and `u_geo = 0` (Q5).

Acceptance: each item returns geo, skill, priority, total and the weights used; infeasible candidates are absent; an activity outside the volunteer's radius is absent from that volunteer's list; an override changes the volunteer ranking for its activity and does not change the activity's position in any volunteer's list; the application fails to start with invalid weights; an organization cannot request recommendations for another organization's activity.

Tests: adapter unit test (entity → record, including `Point` x/y to lon/lat); startup test with invalid weights; IT on a small fixed dataset with the expected order and scores computed by hand; IT for the radius prefilter (inside, outside); IT for the weight override in both directions; IT for the 403 rule.

### Slice 5b: Invitations
Work: an organization invites a volunteer to an activity; the volunteer accepts or declines; acceptance goes through the same transactional path as Slice 5.

Acceptance: an invitation cannot be created for an infeasible volunteer or a full activity; accepting respects spots and capacity.

Tests: IT for invite, accept, decline, ownership; one concurrency case reusing the Slice 5 harness.

### Slice 6d (optional): Batch auto-assign
**Not built for the MVP** (Q18, decided 2026-10-18). Kept here for a later revisit.

Work: greedy assignment over an organization's open activities producing a *proposal*; the organization confirms it.

Acceptance: no volunteer is proposed beyond capacity or for overlapping activities; no activity beyond its spots; nothing is persisted until confirmation.

Tests: unit test on a hand-built case where the greedy order matters, with the expected outcome written down; IT for propose and confirm.

### Slice 7: Notifications
Work: `NotificationSender` interface with a logging implementation; events on application accepted or rejected and invitation created.
Built on 2026-10-23 with six events plus activity cancelled (Q7a, Q7b) and the volunteer email on accepted rows (Q7d).

Acceptance: each event produces exactly one notification after the transaction commits; a failing sender does not roll back the business transaction.

Tests: unit test with a recording fake sender; IT that a rolled-back accept sends nothing.

### Slice 8: Completion and history
Work: organization marks an activity `COMPLETED`; capacity is released without any update, because it is calculated from non-completed activities (Q16); `GET /volunteers/me/history`.
Built on 2026-10-23 with Q8a … Q8h, including a notification to accepted volunteers (Q8d).

Acceptance: only the owner can complete; history lists only the caller's accepted, completed activities.

Tests: IT for completion, capacity release and history ownership.

### Slice 8b: Platform administration
Added 2026-10-17; decisions Q21 … Q24. Work: role `ROLE_ADMIN` on separate admin accounts with no profile (Q22); `/api/v1/admin/**` requires it; the first admin is inserted by a Flyway migration whose credentials come from environment variables (Q21); an admin lists and searches user accounts, bans and unbans a user, and deactivates or removes an organization or an activity that breaks the rules. A ban deletes all of the user's sessions in Redis (indexed session repository) and login stays refused (Q23). No audit log (Q24).

Acceptance: a non-admin gets 403 on every admin endpoint; after a ban the user's open session gets 401 on the next request and the user cannot log in; an admin cannot ban themselves or another admin; a banned volunteer is excluded from recommendations (Q15) and cannot apply; a fresh database contains the configured admin, and no credential is in git.

Tests: IT for each rule above; IT that a ban deletes an existing session (indexed in-memory session repository or a Redis container in the test).

Built on 2026-10-23. The open question (what a ban does to open applications and assignments) is answered by Q25 … Q27; details in Q28 … Q32.

### Slice 9: Seed data and demo
Work: seed profile (not a Flyway migration of production schema) with a few organizations, about 30 volunteers and 10 activities; a short demo script of HTTP calls.
Built on 2026-10-23 with Q33 … Q39; the script is `docs/DEMO.md`.

Acceptance: a clean checkout reaches a working demo with the documented commands.

Tests: one IT that the seed loads and a recommendation call returns a non-empty result.

### Evaluation
Compare the function against two baselines over the same feasible set, nearest-distance and random (Q20), on the seed data, and record the metric definitions in the thesis notes before running anything.

### Frontend (F0 … F5)
Approved 2026-10-25, decisions Q54 … Q66. Code in `frontend/` (React + Vite + TypeScript, Mantine, React Router, TanStack Query). A frontend slice = API calls + page(s) + tests and ends with `npm run lint && npm run build && npm test` green in `frontend/` (plus `./mvnw clean verify` if the backend changed) and a commit. Pages use only existing endpoints; a backend change, if one turns out to be needed, is named in the slice report and asked first.

#### Slice F0: Scaffold, API client, auth
Work: Vite project with ESLint, Vitest, Mantine, React Router, TanStack Query; Vite proxy `/api` → `localhost:8080` (Q58); `npm run gen:api` with `openapi-typescript` and the committed generated types (Q57); the `fetch` wrapper with CSRF and 401 handling (Q59); register (volunteer or organization), login, logout; the current user from `GET /auth/me` decides the role and the menu; route guards per role; layout with a role menu and empty pages for the later slices. Docs: `frontend/CLAUDE.md`, root `CLAUDE.md` layout and scope (Q66), `docs/LOCAL_NOTES.md` (Node and npm versions, how to run both sides).

Acceptance: with the backend running, `npm run dev` opens the app; a new volunteer and a new organization can register, log in, see their own menu, reload the page without losing the session and log out; a page of the other role sends to a "no access" page; a backend error message is shown to the user.

Tests: wrapper unit tests (token header on POST/PUT, token fetched when the cookie is missing, none on GET, 401 → login redirect, error body → message); route guard test.

#### Slice F1: Volunteer profile
Decisions Q67 … Q75. Work: required response fields in the OpenAPI document (Q67, backend); profile page from `GET/PUT /volunteers/me` (basic fields, capacity 0 … 10, radius 1 … 500 km); location with the Leaflet map picker (Q60); skills (no levels: binary vectors, Q7) and certifications from the dictionaries (`/skills`, `/certifications`) saved with `PUT /volunteers/me/skills` and `/certifications`; weekly availability editor saved with `PUT /volunteers/me/availability`.

Acceptance: every field saves and survives a reload; the backend's 400 messages (overlapping slots, bad radius) appear next to the form.

Tests: map picker (click sets lat/lon, fields edit the point); availability editor (add and remove slots, payload shape); profile form validation.

#### Slice F2: Activities and recommendations for volunteers
Decisions Q76 … Q86 (Q77 adds `acceptedCount` to `ActivityResponseDto`, backend). Work: public activity list (`GET /activities`, paged) and activity page (`GET /activities/{id}`, map of the place); recommended activities (`GET /volunteers/me/recommended-activities`) with the score breakdown component (Q62); apply (`POST /activities/{id}/applications`).

Acceptance: an anonymous user sees the list and the page; a volunteer sees recommendations in score order with geo, skill, priority bars, weights and total, and can apply once (409 shown as a message); with the seed data (`seed_vol08`) the demo activity "Sort food donations" appears.

Tests: breakdown component (bars in [0,1], weights and total shown as given by the backend, never recomputed); recommendation list renders the order of the response; apply button states.

#### Slice F3: Organization profile and activities
Decisions Q87 … Q97 (frontend only). Work: profile page from `GET/PUT /organizations/me` with the map picker; own activities (`GET /organizations/me/activities`); create and edit form (`POST`, `PUT /activities/{id}`: time window, place, required skills and certifications, priority, spots needed, optional weight override α, β, γ); publish, cancel and complete buttons by status.

Acceptance: an organization creates a draft, edits it, publishes it, and cancels or completes it; the buttons follow the status; the backend's 400 and 409 messages are shown (start in the past, weights not on the simplex, spots below the accepted count).

Tests: activity form (payload shape, weight override on or off, window validation messages); status → available actions.

#### Slice F4: Applications, recommended volunteers and invitations for organizations
Work: on the organization's activity page: applications (`GET /activities/{id}/applications`) with accept and reject (`POST /applications/{id}/accept|reject`), the volunteer email only for accepted rows (Q7d); recommended volunteers (`GET /activities/{id}/recommended-volunteers`) with the same breakdown component and an invite button (`POST /activities/{id}/invitations`).

Acceptance: the organization accepts and rejects applications, sees the free spots change, sees recommended volunteers with their breakdown and invites one; named 409s (no spot, capacity, overlap) are shown as messages.

Tests: applications table actions by status; invite moves the row to "invited".

#### Slice F5: Volunteer applications, invitations and history
Work: own applications (`GET /volunteers/me/applications`) split into applications and invitations by origin, withdraw (`POST /applications/{id}/withdraw`), accept and decline invitations (`POST /invitations/{id}/accept|decline`); history (`GET /volunteers/me/history`).

Acceptance: a volunteer withdraws an application, accepts or declines an invitation, and sees completed activities in the history; the five-minute demo of `docs/DEMO.md` can be shown through the UI.

Tests: actions by status and origin; history list.

## 3. Risks

| Risk | Effect | Mitigation |
|---|---|---|
| Scoring semantics change after the schema exists | Schema of Slices 3 and 4 redone | Decisions are recorded (ANALYSIS 6); 6a and 6b are still built first so the tests fix them before any profile or activity migration |
| `u_skill` is no longer plain cosine (Q10) | The thesis text and the code describe different functions | `FUNCTION.md` 1.2 is updated; a test documents that extra skills do not lower the score |
| Per-activity weight override (Q12) | Scores of different activities not comparable; organizations could inflate their position | Override used only for activity → volunteers; weights returned in the breakdown |
| Radius applied in one direction only (Q6) | An organization is shown a volunteer who would never see that activity | Accepted for the MVP; `u_geo` already ranks far volunteers low; stated in the thesis |
| Re-keying `organization` (Q-E) | Foreign keys and registration break | Own migration in Slice 2, before activities are rebuilt in Slice 4; dev database holds no real data |
| `u_prio` constant per activity (ANALYSIS 3.3) | `γ` appears to do nothing in one direction | State it in the thesis; show priority in the volunteer→activities direction |
| Overbooking under concurrency | Core correctness failure | Row lock on the activity in the accept transaction, unique constraint, concurrency IT in Slice 5 |
| Calculated capacity under concurrency (Q16) | Two accepts for one volunteer both see capacity 1 | Lock the volunteer row in the accept transaction; second concurrency IT in Slice 5 |
| Flyway baseline against an existing dev database built by `ddl-auto` | Migration fails locally | Recreate the dev database in Slice 0 (it holds no real data) |
| H2 → Testcontainers | Tests need Docker; slower build | Docker is already required for Postgres; one shared container per test run |
| Package move in Slice 0 | Large diff, merge pain with uncommitted work | Commit the working tree first; move in its own commit with no logic change |
| Sessions + CSRF for an API-first MVP (Q-C) | Friction in the demo script and tests | Test helper and demo script that log in and carry the session cookie and CSRF token |
| Time zones in availability | Wrong feasibility at day boundaries | One configured zone for the MVP (Q13) |
| Scoring all volunteers in memory (activity → volunteers) | Slow with large data | Acceptable for the MVP; only the volunteer → activities direction has a PostGIS prefilter |
| Frontend and backend types drift apart | Runtime errors in the UI | Generated types from springdoc (Q57); a changed DTO breaks `npm run build` |
| springdoc document is incomplete (missing `required`, generic responses) | Generated types are too loose | Check the generated file in F0; fix with annotations in the backend only where a page needs it, asked first |
| Session and CSRF cookies in the browser | Login or POST fail in the UI | One origin through the Vite proxy (Q58); wrapper unit tests (Q59) |
| Scope creep (geocoding, chat, websocket) | Slices do not finish | Remove unused dependencies in Slice 0; out-of-scope list in `CLAUDE.md` |

## 4. Decisions

All of Q-A … Q-H and Q1 … Q20 are answered, and Q54 … Q66 for the frontend; see `docs/ANALYSIS.md` section 6. No slice is blocked by a question.
