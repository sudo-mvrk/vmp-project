# ANALYSIS

Phase A output. Date: 2026-10-07. Branch inspected: `mvp-reboot` (working tree, including uncommitted changes).
The questions raised by this analysis are answered in section 6 (Decisions). Sections 1 to 5 are the original analysis and still refer to them by number.

## 1. Repository state

### 1.1 What is there

The repo is not empty. `mvp-reboot` is a partial rewrite of the bachelor project:

| Area | State |
|---|---|
| Build | Spring Boot 3.3.3, **Java 21**, Maven wrapper. Compiles. |
| Tests | 17 tests, **9 pass, 8 fail**. All 8 failures are in `AuthControllerIntegrationTest` and return 403 instead of 200/201/400. The tests send no CSRF token while `SecurityConfig` enables CSRF, which is the likely cause (not verified further). |
| Auth | **Session based** (Spring Session + Redis), not JWT. History shows a deliberate switch away from JWT (commit `1484f0e`). Register volunteer, register organization, login, logout, `/me`. |
| Profiles | `Volunteer` and `Organization`, each sharing its primary key with `AppUser` (`@MapsId`). Update-profile endpoints for both. |
| Activity | Entity, DTOs, mapper, repository, service and controller exist but **do not work** (see 1.3). Uncommitted. |
| Application | `ActivityApplication` entity only. No repository, service or controller. |
| Skills | `Skill` dictionary entity with `Category` enum. Not linked to volunteers or activities. |
| Schema | `ddl-auto: update`. **No Flyway.** |
| Test DB | H2 in memory. **No Testcontainers.** |
| Geo | PostGIS + `hibernate-spatial`, `Point` (SRID 4326) on volunteer, organization and activity. Unused Nominatim Feign client. |
| Structure | Package by layer (`controller`, `service`, `repository`, ...), base package `com.volunnear`. |
| Docker | `postgis/postgis:16-3.4` (service name `postgres`, not `db`), `redis:latest`, app container. |

Other branches: `master` is the bachelor project (JWT, feedback, social links, subscriptions, radius-only recommendations). `reboot-core` and `archive/legacy-reboot-v1` are intermediate snapshots.

### 1.2 Reusable

- `AppUser`, `Role`, `AppUserRepository`, `CustomUserDetails`, `UserDetailServiceImpl` (login by username or email).
- `AuthService` registration flow and `AuthServiceTest` (8 passing unit tests).
- `Volunteer` and `Organization` entities, their services, mappers and `VolunteerServiceTest`.
- `GlobalExceptionHandler`, `ErrorResponse`, custom exceptions, `StringTrimAdvice`.
- `Skill`, `Category`, `Priority`, `ActivityStatus`, `ApplicationStatus` as a starting point.
- PostGIS Docker setup and `Point` mapping. The current mappers build `Coordinate(lon, lat)`, which is the correct axis order.
- From `master`, as reference only: `JwtTokenProvider` / `JwtTokenFilter` / `RefreshToken` (if we go back to JWT) and `EmailNotificationService` (Slice 7).

### 1.3 Not reusable or broken

Activity slice (all uncommitted):
- `VolunteeringActivityRepository extends JpaRepository<Organization, Long>`: wrong entity type.
- `VolunteeringActivityService`: dependencies are not `final`, so `@RequiredArgsConstructor` injects nothing and every call throws `NullPointerException`. The method prints the entity and returns `null`.
- `VolunteeringActivityController.createActivity` has no `@PostMapping`, so the endpoint does not exist.
- `VolunteeringActivity.id` has no `@GeneratedValue`. `activityStatus` has no `@Enumerated(STRING)`, so it is stored as an ordinal. The field `organizationId` holds an `Organization`, not an id.
- `VolunteeringActivityCreationRequest.lat/lon` are `Long`, so coordinates lose their fraction.
- No schedule, no required skills, no required certifications.

Smaller defects in code we keep:
- All three `toPoint` mappers treat `lat == 0` as "no location" and never check `lon` for null.
- `AddressDTO` puts `@NotBlank` on `Double` fields, which throws at validation time.
- `GlobalExceptionHandler.handleBadRequestsExceptions` is registered for `GeoCodingException` but its parameter type is `BadDataInRequestException`; a `GeoCodingException` cannot be bound to it.
- Services throw `UsernameNotFoundException`, which has no handler, so the client gets 500.
- `ActivityApplication.appliedAt` uses `@UpdateTimestamp`, so it changes on every status update. `ApplicationStatus` mixes `REJECT` and `CANCELED` naming.
- `VolunNearApplicationTests` (context load) is commented out.

Dead or out of scope:
- `roles.sql` is MySQL syntax from the bachelor project and is still copied by the `Dockerfile`.
- `pom.xml.versionsBackup`, outdated `README.md`.
- Dependencies with no use in the MVP: `spring-boot-starter-websocket` (chat is out of scope), `spring-cloud-starter-openfeign` + `NominatimClient` (geocoding is not in scope), `spring-boot-starter-aop`.
- Everything on `master` around feedback, social links and subscriptions.

### 1.4 Conflicts between `CLAUDE.md` and the repo

| # | `CLAUDE.md` says | Repo has |
|---|---|---|
| C1 | Java 17 | Java 21 (pom, Dockerfile, installed JDK) |
| C2 | Spring Security with JWT | Sessions in Redis with CSRF cookie |
| C3 | Flyway for all schema changes | `ddl-auto: update`, no migrations |
| C4 | Testcontainers | H2 |
| C5 | Package by feature | Package by layer |
| C6 | `docker compose up -d db` | Service is named `postgres` |
| C7 | Organizations **and membership** | One `AppUser` account is one organization |
| C8 | Volunteer "capacity" | `capacity` is a field on the activity (number of spots) |

Housekeeping:
- The spec file is `docs/FUNCTION.md`; `CLAUDE.md` and `TODO.md` call it `Function.md`. Linux is case sensitive.
- `.gitignore` has an unstaged line `docs/`, while `docs/*.md` are staged. New files in `docs/` (including this one) are ignored by git until that line is removed or they are added with `git add -f`.
- `docs/CLAUDE.md` is not loaded automatically by Claude Code. Only `./CLAUDE.md` or `.claude/CLAUDE.md` are.
- `redisdata/` is untracked and owned by uid 999. It should be in `.gitignore`.
- The working tree has 17 modified files. They should be committed or stashed before Slice 0.

## 2. Scoring function: inputs and sources

Notation in `FUNCTION.md`: `v` volunteer, `t` task (an activity).

Formatting note: `\exp(-\lambda, d(v,t))` and `\alpha, u_{geo}` contain a comma where LaTeX `\,` (thin space) lost its backslash. I read them as products: `exp(-λ · d)` and `α · u_geo`. The set braces lost their backslashes the same way. Please confirm (Q1).

| Input | Meaning | Supplied by | Exists today |
|---|---|---|---|
| `d(v,t)` | distance volunteer to activity | `Volunteer.location`, `VolunteeringActivity.location` (`Point`, SRID 4326) | Yes. Volunteer location is nullable. |
| `λ` | decay rate, unit 1/distance | configuration | No |
| `S_v` | volunteer skill vector | new `volunteer_skill` (volunteer, skill[, level]) over the `Skill` dictionary | Dictionary only |
| `S_t` | activity skill vector | new `activity_required_skill` (activity, skill[, weight]) | No |
| `P(t)` | numeric priority | `VolunteeringActivity.priority` (`Priority` enum, nullable) | Yes, as enum |
| `P_min`, `P_max` | normalisation bounds | constants of the scale, or min/max of a set of activities (Q8) | No |
| `α, β, γ` | weights | configuration | No |
| `Available(v,t)` | time fit | new volunteer availability + new activity start/end | No (entity has `// TODO: Implement Schedule`) |
| `Certified(v,t)` | certificate fit | new `certification` dictionary, `volunteer_certification`, `activity_required_certification` | No |
| `Cap(v)` | remaining volunteer capacity | new field on `Volunteer` and/or count of accepted applications | No |

Implied by the platform but absent from `FUNCTION.md`: activity is `OPEN`, activity has free spots, volunteer has no existing application for it, user is not banned (Q15).

## 3. Component analysis

### 3.1 `u_geo = exp(-λ · d)`

- **Wording against formula.** A logistic function is `1 / (1 + e^{k(d - d0)})`: flat near zero, a drop around `d0`, two parameters. The formula given is exponential decay: steepest at zero, one parameter. They are different curves. The formula is unambiguous and the word "logistic" is not, so I would implement the exponential and fix the wording (possibly "logistics cost" in the sense of travel was meant). Needs your confirmation (Q2).
- **Range.** `d ≥ 0` and `λ > 0` give `(0, 1]`, with 1 at `d = 0`. It never reaches 0. `λ = 0` gives a constant 1 and `λ < 0` leaves the range, so validation must require `λ > 0`.
- **Distance metric and unit.** Not specified. Great-circle (haversine) in kilometres is the natural choice and keeps the scoring code free of the database. `λ` is meaningless without the unit (Q3).
- **Choosing `λ`.** It is easier to reason about as a half-distance: `λ = ln 2 / d_half`. With `λ = 0.1 /km`, the score is 0.61 at 5 km, 0.37 at 10 km, 0.05 at 30 km (Q4).
- **Missing coordinates.** Volunteer location is nullable. Options: volunteer is infeasible, or `u_geo = 0`. `CLAUDE.md` requires a guard but not which one (Q5).
- **`Volunteer.radius`** exists in the entity and is not in the function. It could be a hard filter, or ignored (Q6).
- **Mapper defect.** `lat == 0` must stop meaning "no location" before distances are trusted.

### 3.2 `u_skill = (S_v · S_t) / (‖S_v‖ ‖S_t‖)`

- **Vector definition.** Not specified. Binary (has skill / needs skill) or weighted (proficiency level, importance) (Q7). The `[0,1]` range only holds if all components are non-negative.
- **Zero vectors.** A volunteer with no skills or an activity with no required skills gives `0/0`. The two cases are not symmetric in meaning: "activity needs nothing" suggests everyone fits (1), "volunteer has nothing" suggests 0 (Q9).
- **Cosine penalises extra skills.** With binary vectors, activity needs `{first aid}`:
  - volunteer `{first aid}` scores 1.0
  - volunteer `{first aid, driving, cooking, IT}` scores `1 / (1 · 2) = 0.5`

  The more qualified volunteer scores lower. This follows directly from the formula; it is not an implementation choice. A coverage measure `|S_v ∩ S_t| / |S_t|` would not behave this way, but it is a different function from the one in `FUNCTION.md`. You should decide this knowingly because it affects the thesis results (Q10).
- **Rounding.** Floating point can produce `1.0000000002`. Clamp to `[0,1]`.

### 3.3 `u_prio = (P(t) - P_min) / (P_max - P_min)`

- **Numeric mapping.** `Priority` is an enum. Assume `LOW=1 … URGENT=4`, equally spaced (Q8).
- **What `P_min`/`P_max` range over.** Two readings:
  1. Bounds of the scale (1 and 4). Constant, never divides by zero, and `R` stays comparable across requests.
  2. Min and max over the activities being compared. Then one activity alone, or several of equal priority, gives `0/0`, and the same activity gets different scores in different result sets. That contradicts the property "comparable across tasks" stated in `FUNCTION.md`.

  `CLAUDE.md` asks for an "equal min/max priority" guard, which suggests reading 2 was in mind (Q8).
- **`u_prio` does not depend on `v`.** In `argmax over v` for a fixed `t`, the term `γ · u_prio(t)` is the same for every volunteer and cannot change the ranking. It only shifts the totals. Priority influences results only when activities are ranked for a volunteer, or in batch assignment across activities. With `γ > 0`, the activity→volunteers ranking is effectively decided by `α` and `β` alone. Worth stating in the thesis (Q11).
- **Null priority.** The column is nullable today (Q8).

### 3.4 Weights

- `α, β, γ ≥ 0` and `α + β + γ = 1`. Check the sum with a tolerance (for example `1e-9`), since `0.4 + 0.4 + 0.2` is not exactly 1 in binary floating point.
- Scope: one global set, or per organization (Q12). `TODO.md` implies global in `application.yml`.

### 3.5 Feasibility filter

- **`Available(v,t)`.** Needs a time model on both sides. Open: is an activity a single time window; is volunteer availability a weekly pattern (day + time range) or concrete date ranges; must availability cover the whole activity or only overlap it; which time zone; does an already accepted overlapping activity make the volunteer unavailable (Q13).
- **`Certified(v,t)`.** Reading: every certification the activity requires is held by the volunteer. No required certifications means certified. Open: do certifications expire, and are they verified by anyone or self-declared (Q14).
- **`Cap(v) > 0`.** Not defined. Candidates: maximum number of simultaneous active assignments, or hours per week. Also open whether it is a stored counter that is decremented, or derived as `max − count(accepted, not finished)`. `CLAUDE.md` says "decrement spots and capacity", which implies stored counters (Q16).

### 3.6 Argmax

- **Single winner against several spots.** `v*` is one volunteer. An activity with `k` spots needs the top `k`. Recommendation endpoints should return a ranked list (Q17).
- **Ties.** No rule given. A deterministic tie-break is needed for reproducible tests and thesis results (Q17).
- **Empty feasible set.** Return an empty list, not an error.
- **Reverse direction.** The function defines only `argmax over v`. "Activities for a volunteer" ranks `t` by the same `R(v,t)` with the same feasibility predicate; that is an extension, and it is the direction where `u_prio` matters.
- **Batch assignment (6d).** Applying the argmax per activity in sequence is greedy and not globally optimal; the result depends on the order in which activities are processed (Q18).

### 3.7 Claimed properties

| Claim | Holds? |
|---|---|
| `R ∈ [0,1]` | Yes, given non-negative skill vectors, `λ > 0`, guarded zero cases and valid weights. |
| Comparable across tasks | Only with fixed priority bounds (reading 1 in 3.3). |
| Convex combination | Yes, if weights are validated. |
| Dimensionless attributes | Yes, provided `λ` and `d` use the same unit. |
| Well-defined argmax | Only with a tie-break rule and a defined result for the empty set. |

## 4. Domain entities and relations (proposed)

Names in *italics* are new. Items marked (Q) depend on an open question.

```
AppUser 1──1 Volunteer            (shared PK, exists)
AppUser 1──1 Organization         (shared PK, exists)

Volunteer    N──M Skill           via volunteer_skill [level (Q7)]
Volunteer    N──M Certification   via volunteer_certification [valid_until (Q14)]
Volunteer    1──N AvailabilitySlot  (Q13)

Organization 1──N Activity
Activity     N──M Skill           via activity_required_skill [weight (Q7)]
Activity     N──M Certification   via activity_required_certification

Volunteer 1──N ActivityApplication N──1 Activity
```

| Entity | Fields | Notes |
|---|---|---|
| `AppUser` | id, username, email, password, banned, createdAt, roles | exists |
| `Volunteer` | id, firstName, lastName, dateOfBirth, location, locationName, countryCode, city, region, radius (Q6), *maxActiveAssignments* (Q16) | exists + 1 field |
| `Organization` | id, organizationName, description, websiteUrl, location, city, fullAddress, countryCode | exists |
| `Skill` | id, skillName, category | exists |
| *`Certification`* | id, name | dictionary |
| *`AvailabilitySlot`* | id, volunteer, dayOfWeek, startTime, endTime | shape depends on Q13 |
| `Activity` (`VolunteeringActivity`) | id, organization, title, shortDescription, fullDescription, priority, location, *startsAt*, *endsAt*, *spotsNeeded* (rename of `capacity`, see C8), status, updatedAt | rebuild |
| `ActivityApplication` | id, volunteer, activity, status, appliedAt, *decidedAt*, *origin* (APPLICATION or INVITATION) | unique (volunteer, activity) |

Not modelled as entities in the MVP: notifications (log stub), history (accepted applications on `COMPLETED` activities).

Pure domain records for scoring (no JPA, no Spring):

```
GeoPoint(lat, lon)
VolunteerProfile(id, GeoPoint location?, Map<skillId, weight> skills,
                 Set<certId> certifications, availability, remainingCapacity)
ActivityProfile(id, GeoPoint location, Map<skillId, weight> requiredSkills,
                Set<certId> requiredCertifications, start, end, priority)
ScoringWeights(alpha, beta, gamma, lambda)
ScoreBreakdown(geo, skill, priority, total)
```

## 5. Data needed per component

| Component | From volunteer | From activity | From config |
|---|---|---|---|
| `u_geo` | location (lat, lon) | location (lat, lon) | `λ`, distance unit |
| `u_skill` | skill ids [+ level] | required skill ids [+ weight] | zero-vector rule |
| `u_prio` | none | priority | numeric mapping, `P_min`, `P_max` |
| Available | availability slots, accepted activities' time windows | startsAt, endsAt | time zone |
| Certified | certification ids [+ valid_until] | required certification ids | none |
| Cap | maxActiveAssignments, count of active accepted applications | none | none |
| Platform filter | banned flag, existing applications | status, free spots | none |
| Aggregate | | | `α`, `β`, `γ` |

## 6. Decisions

All questions were answered by the developer on 2026-10-07. "Changed" marks an answer that differs from the original recommendation. Where sections 1 to 5 disagree with this section, this section wins.

### Project setup (Slice 0)

- **Q-A. Evolve `mvp-reboot`.** No fresh skeleton.
- **Q-B. Java 21.**
- **Q-C. Keep sessions.** No JWT; Redis and CSRF handling stay.
- **Q-D. Package by feature now**, as one behaviour-free refactor commit in Slice 0, base package `com.volunnear`.
- **Q-E. Membership table now (changed).** One account per organization in the MVP, but the basis for several accounts is built in from the start so no key migration is needed later:
  - `Organization` gets its own generated id; the `@MapsId` link to `AppUser` is removed.
  - `organization_member (organization_id, user_id, role)` exists from the start, with exactly one `OWNER` row per organization created at registration.
  - Every ownership check goes through one method (for example `OrganizationAccess.canManage(userId, organizationId)`) that reads the member table.
  - No invite or role-management endpoints in the MVP.
- **Q-F. Keep PostGIS `Point`.** Scoring distance is computed in pure Java; PostGIS does the radius prefilter (Q6).
- **Q-G. Remove all three unused dependencies:** websocket, aop, openfeign + `NominatimClient`.
- **Q-H. Track `docs/` in git and move `CLAUDE.md` to the repo root.** Remove the `docs/` line from `.gitignore`, add `redisdata/`.

### Scoring (Slice 6a)

- **Q1. Commas are multiplication.** `FUNCTION.md` now writes them as `\,`.
- **Q2. `u_geo` is exponential decay.** `FUNCTION.md` no longer says "logistic".
- **Q3. Distance is haversine, in km.**
- **Q4. `λ` is configured as a half-distance, default 10 km** (`λ = ln 2 / 10 ≈ 0.0693 /km`).
- **Q5. Volunteer without location: `u_geo = 0`, still listed.**
- **Q6. `Volunteer.radius` is not used in scoring (refined).** It is a PostGIS prefilter (`ST_DWithin`) in the volunteer→activities direction only: open activities within the volunteer's radius are selected, then scored. In the activity→volunteers direction the radius is not applied, so an organization can be shown a volunteer whose own radius would not include the activity. The `d_max` variant in `FUNCTION.md` 1.1 is not used.
- **Q7. Binary skill vectors.** The scoring record holds doubles so levels can be added later without changing the function.
- **Q8. Priority (revised the same day).** Mapping `LOW=1 … URGENT=4`; priority is `NOT NULL` with default `MEDIUM`. `P_min = 1` and `P_max = 4` are the fixed bounds of the scale, giving `0, 1/3, 2/3, 1`. `FUNCTION.md` 1.3 is updated. The first answer was pool-based bounds with `u_prio = 1` for equal priorities; it was dropped because the spec did not define the pool, the score of an activity would change as other activities open or close, and thesis results would not be reproducible.
- **Q9. Zero skill vectors.** Activity requires no skills → `u_skill = 1`. Volunteer has none of the required skills → `u_skill = 0`.
- **Q10. Cosine is projected onto the required skills (changed; the function itself changes).** `u_skill` is the cosine between `S_t` and `S_v` restricted to the dimensions where `S_t > 0`. Extra unrelated skills no longer lower the score; only missing skills do. With binary vectors this equals `sqrt(k / n)`, where `n` is the number of required skills and `k` the number of them the volunteer has. `FUNCTION.md` 1.2 is updated. The example in 3.2 now gives 1 for both volunteers.
- **Q11. `u_prio` stays in the formula in both directions.** It cannot change the ranking of volunteers for one activity; this is stated in the thesis, and the breakdown in the API shows it.
- **Q12. Weights (changed).**
  - Global default in `application.yml`: `α = 0.4`, `β = 0.4`, `γ = 0.2`, validated at startup (non-negative, sum 1).
  - An organization can override the weights for a concrete activity. All three are given together and must be non-negative and sum to 1, otherwise 400. Absent means the default applies.
  - The override is used only when ranking volunteers for that activity. Activities shown to a volunteer are always ranked with the global weights, so an organization cannot inflate its position.
  - The score breakdown returns the weights that were used.

### Feasibility (Slice 6b, schema of Slices 3 and 4)

- **Q13. Availability.** An activity has one `startsAt`/`endsAt`. A volunteer has weekly slots (day of week + time range). Available means the slots fully cover the activity window, in one configured time zone. An accepted activity that overlaps makes the volunteer unavailable. Multi-day activities are out of scope.
- **Q14. Certifications.** Optional `validUntil`, checked against the activity start. Self-declared, no verification.
- **Q15. Platform conditions** (activity `OPEN`, free spots, no existing application, not banned) are part of the feasible set as a separate named predicate, next to the `FUNCTION.md` filter.
- **Q16. `Cap(v)` is the remaining capacity of volunteer `v`, counted in activities and calculated.** The volunteer stores `maxActiveAssignments`; `Cap(v)` = that minus the accepted applications on activities not yet completed or cancelled. No stored counter is decremented.

Edge cases answered on 2026-10-08, while building Slice 6b:

- **Q14a. `validUntil` is inclusive.** A certification is valid through the end of its `validUntil` day; it fails only when `validUntil` is before the date of the activity start.
- **Q13a. Slots of one day combine.** Slots that touch or overlap are merged before the coverage check, so 09:00-12:00 and 12:00-15:00 cover 10:00-14:00. Any gap inside the window fails.
- **Q13b. Windows are half-open `[start, end)`.** An accepted activity that ends at the minute another starts does not block it. No travel time is modelled.

### Assignment (Slices 6c and 6d)

- **Q17. Ranked top-N list** with breakdown and a limit parameter; `v*` is the first element. Tie-break as in `FUNCTION.md` 4: larger `u_skill`, then larger `u_geo`, then lower id for a deterministic order.
- **Q18. Batch auto-assign (6d) stays optional.** Decide after 6c.
  Decided on 2026-10-18, after 6c and 5b: **6d is not built for the MVP.** Organizations assign through the recommendation list and invitations (5b). The greedy order-dependence noted in section 5 stays a thesis remark; 6d can be revisited after the evaluation.

### Scope

- **Q19. Invitations are Slice 5b, after 6c.** An organization invites from the recommendation list.
- **Q20. Evaluation baselines: both** nearest-distance and random, over the same feasible set.

### Auth (Slice 1)

Answered on 2026-10-17, before building Slice 1:

- **Q1a. CSRF token endpoint.** `GET /api/v1/auth/csrf` is open to everyone and returns the token with its header and parameter names. The cookie repository stays as it is. A client calls it once before its first modifying request (registration, login).
- **Q1b. Missing profile of a logged-in user is 404.** 401 is kept for failed logins and requests without a session; a wrong role is 403 from the security rules before the service runs.

### Organizations (Slice 2)

Answered on 2026-10-17, before building Slice 2:

- **Q2a. The public profile shows the organization, not the account.** `GET /organizations/{id}` and `GET /organizations/me` return id, name, description, website, city, address, country and lat/lon. No username or email.
- **Q2b. Location on update: both or neither.** lat and lon both absent keep the stored location; both present set it; only one of them is 400. Latitude 0 is a valid coordinate. Clearing a location is not supported in the MVP.
- **Q2c. `locationDescription` is removed from the update DTO.** It had no column and was dropped silently.
- **Q2d. An account is a member of at most one organization** in the MVP (`organization_member.user_id` is unique), so `/organizations/me` is unambiguous.

### Volunteer profile (Slice 3)

Answered on 2026-10-17, before building Slice 3:

- **Q3a. The username appears only in the caller's own profile** (`GET`/`PUT .../me`). Any view of a profile by someone else, for example an organization looking at a volunteer in an application or a recommendation, has no username or email.
- **Q3b. Q3a applies to organizations too.** `GET`/`PUT /organizations/me` include the username; the public `GET /organizations/{id}` does not (refines Q2a).
- **Q3c. Skills, certifications and availability are each replaced as a whole** with `PUT /volunteers/me/skills`, `/certifications` and `/availability`; the body is the complete list and an empty list clears it. Basic fields stay on `PUT /volunteers/me`.
- **Q3d. Availability slots:** start before end; overlapping slots of the same day are 400, touching slots are allowed (they merge, Q13a). A slot ends at 23:59 at the latest; an activity that ends at midnight is multi-day and never feasible (Q13). Documented limitation of the MVP.
- **Q3e. Capacity `maxActiveAssignments`:** default 1 for new and existing volunteers, allowed 0 … 10, otherwise 400. 0 means "not taking assignments now", so the volunteer is never recommended.
- **Q3f. A certification sent with `validUntil` in the past is 400.** A stored one that expires later stays and is ignored by feasibility (Q14a).
- **Q3g. `radius` is in kilometres, 1 … 500, optional.** Absent means no prefilter in 6c: the volunteer sees activities at any distance, ranked by `u_geo`.
- **Q3h. `bio` gets a `TEXT` column** (it was in the DTOs but had no column).

### Activities (Slice 4)

Answered on 2026-10-17, before building Slice 4:

- **Q4a. Times are local date-times without offset** (`"2026-11-14T09:00"`), in the one zone configured as `volunnear.time-zone` (default `Europe/Prague`), stored as `timestamp without time zone`. Matches `TimeWindow` and the weekly slots (Q13).
- **Q4b. An activity always has a location** (lat and lon required). Online activities are a later extension.
- **Q4c. `PUT /activities/{id}` replaces the whole activity.** Editable in `DRAFT` and `OPEN`; in any other status an edit is 409. Slice 5 adds: `spotsNeeded` cannot go below the number of accepted volunteers.
- **Q4d. Transitions and time checks.** Publish `DRAFT → OPEN`; cancel `DRAFT`/`OPEN → CANCELLED`; any other transition is 409. `IN_PROGRESS` has no transition in the MVP; Slice 8 completes from `OPEN`. Create, update and publish need a start in the future (400). Start and end are on the same day; an end at midnight is the next day and is rejected, the latest end is 23:59 (Q13, Q3d).
- **Q4e. Visibility.** Public `GET /activities` lists `OPEN` activities that have not started, sorted by start, paged (`page`, `size`), no filters. Public `GET /activities/{id}` for anything but `DRAFT`; a draft is visible only to its organization, others get 404. `GET /organizations/me/activities` lists the caller's activities in every status.

### Applications (Slice 5)

Answered on 2026-10-17, before building Slice 5:

- **Q5a. Statuses `PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`.** The organization moves `PENDING` to `ACCEPTED` or `REJECTED`. The volunteer withdraws from `PENDING`, or from `ACCEPTED` while the activity has not started; that frees the spot and the capacity. The organization cannot revoke an acceptance in the MVP. Any other transition is 409.
- **Q5b. One application per volunteer and activity, ever** (unique constraint), also after a withdrawal or rejection. Matches the "no existing application" platform condition (Q15).
- **Q5c. Applying needs:** the activity `OPEN`, not started, with a free spot; the volunteer holds every required certification, valid on the activity date (Q14a). Otherwise 409. Availability slots and capacity are not checked when applying; a pending application uses no capacity.
- **Q5d. Accepting, in one transaction** that locks the activity row and then the volunteer row (always in this order): the activity is still `OPEN` and not started, a spot is free, the remaining capacity is above 0 (Q16), and no other active accepted activity of the volunteer overlaps in time (Q13). Otherwise 409.
- **Q5e. Cancelling an activity leaves its applications as they are.** Pending ones can no longer be accepted (the activity is not `OPEN`); accepted ones stop counting against capacity because Q16 skips cancelled activities.
- **Q5f. Views.** The owner lists the applications of an activity with the volunteer profile without username or email (Q3a); contact comes with Slice 7. A volunteer lists their own applications.

### Invitations (Slice 5b)

Answered on 2026-10-18, before building Slice 5b:

- **Q5g. An invitation is a row in `applications` with `origin = INVITATION`**, starting `PENDING`. It shares the one-row-per-pair rule (Q5b), the "already applied" platform condition (Q15) and the spot and capacity counting. `applied_at` is the moment of the invitation. Migration V6.
- **Q5h. The volunteer declines with the new status `DECLINED`**, so "the organization said no" (`REJECTED`) and "the volunteer said no" stay distinct.
- **Q5i. Each side decides what the other started.** The organization accepts or rejects an `APPLICATION`, the volunteer accepts or declines an `INVITATION`; the wrong combination is 409. A pending invitation is declined, not withdrawn (withdraw is 409); an accepted invitation is withdrawn like an accepted application (Q5a). The organization cannot take back a pending invitation in the MVP.
- **Q5j. An invitation needs the filter of the recommendations:** activity `OPEN` and not started, a free spot, no existing row for the pair, volunteer not banned, available, certified, capacity above 0. The volunteer need not be in the current top-N. Each failed condition is 409 with its own message (a banned volunteer gets a neutral "cannot be invited"); an unknown volunteer is 404.
- **Q5k. Accepting an invitation is the Slice 5 accept:** the same locked method (activity, then volunteer) and the same checks (Q5d). Availability slots and certifications are not re-checked.
- **Q5l. More pending invitations than free spots are allowed.** The first accept takes the spot; later ones are 409 "no free spots".
- **Q5m. Endpoints:** `POST /activities/{id}/invitations` with `{ "volunteerId" }` (organization), `POST /invitations/{id}/accept|decline` (volunteer, application id). No new lists: invitations appear in the existing application lists with their `origin`.

### Recommendations (Slice 6c)

Answered on 2026-10-17, before building Slice 6c:

- **Q6a. Recommendations for an activity that is not `OPEN` or has started are 409**, like applying (Q5c); the ranking would otherwise return an empty list without a reason. The ownership check (403) comes first, so a draft is 409 only for its own organization.
- **Q6b. `limit` defaults to 10 and is 1 … 50**, otherwise 400, in both directions. No paging: the list is a top-N (Q17).
- **Q6c. Volunteer → activities loads only `OPEN` activities with `startsAt` after now**, the rule of the public list (Q4e); the radius prefilter applies on top.
- **Q6d. The radius prefilter measures on a sphere:** `ST_DWithin(a.location::geography, point::geography, radius_km * 1000, false)`, so it agrees with the haversine distance of the scoring to a few metres. An activity exactly at the radius is inside.
- **Q6e. Response items.** Activity → volunteers: `{volunteer: VolunteerProfileResponseDto, score}` (no username, Q3a). Volunteer → activities: `{activity: ActivityResponseDto, score}`. `score` = `{geo, skill, priority, total, weights: {alpha, beta, gamma, halfDistanceKm}}`, not rounded. No distance in km.
- **Q6f. Candidate volunteers are loaded with a database prefilter** that skips banned volunteers and those with `maxActiveAssignments = 0`; every other rule is checked by the pure `Ranker`. Enough for MVP data sizes.

### Notifications (Slice 7)

Proposed on 2026-10-18 and answered on 2026-10-23 ("ok" to all), before building Slice 7:

- **Q7a. Events.** Six. To the volunteer: application accepted, application rejected, invitation received. To the organization: invitation accepted, invitation declined, an accepted volunteer withdrew. A new pending application does not notify; the organization sees it in its list. (`docs/PLAN.md` lists only accepted, rejected and invitation created.)
- **Q7b. Activity cancelled.** Yes. Cancelling notifies every volunteer whose application or invitation for the activity is `PENDING` or `ACCEPTED`, one notification each. Not in `docs/PLAN.md`.
- **Q7c. Recipient address.** `AppUser.email` of the volunteer, and of the organization's `OWNER` member. The column allows null; a missing email skips the notification with a log warning, not an error.
- **Q7d. Contact after acceptance (closes "contact comes with Slice 7" in Q5f).** The organization's application list shows the volunteer's email only for `ACCEPTED` rows, other rows stay without it (Q3a); the "accepted" notification to the volunteer includes the organization's email.
- **Q7e. Sender.** Only the logging sender, one structured log line per notification (recipient, subject, body). Remove the unused `spring-boot-starter-mail` dependency. A real SMTP sender can come later behind the same `NotificationSender` interface if the demo needs it.
- **Q7f. Delivery.** The service publishes a Spring event inside the transaction; a `@TransactionalEventListener(AFTER_COMMIT)` sends it synchronously; a sender failure is caught and logged, with no retry and no outbox table. Plain English texts, one subject and a short body per event type.

Tests: a unit test per event with a recording fake sender; an IT that a rolled-back accept (409) sends nothing; an IT that a throwing sender leaves the accept committed; an IT for the email shown only on accepted rows.

### Completion and history (Slice 8)

Answered on 2026-10-23, before building Slice 8:

- **Q8a. Completion** is `OPEN → COMPLETED`, only by the owning organization (403 otherwise) and only once `startsAt` has passed (409 before). It does not wait for `endsAt`, so an organization that finishes early can close the activity. Any other status is 409.
- **Q8b. Pending applications and invitations stay as they are** when the activity completes: no status change, no new status. They can no longer be accepted (accept needs an open activity that has not started) and are not in the history.
- **Q8c. No attendance tracking.** Every `ACCEPTED` volunteer of a completed activity took part; there is no no-show marking.
- **Q8d. Completion notifies every `ACCEPTED` volunteer**, one notification each ("Activity completed", a short thank-you), through `Notifier` after commit (Q7f). Pending volunteers get nothing. Extends Q7a.
- **Q8e. History.** `GET /volunteers/me/history` lists the caller's accepted applications on `COMPLETED` activities, newest activity first, without paging. Item: `applicationId`, `activityId`, `title`, `city`, `startsAt`, `endsAt`, `organizationId`, `organizationName`. No totals.
- **Q8f. Only the volunteer sees their history.** No new endpoint for organizations: `GET /organizations/me/activities` and `GET /activities/{id}/applications` already cover them.
- **Q8g. Completion is manual only**, no scheduler. Known limitation: an activity the organization never completes keeps its accepted volunteers' capacity used (Q16 unchanged).
- **Q8h. `IN_PROGRESS` stays unused**: no transition into it in the MVP.

### Seed data and demo (Slice 9)

Answered on 2026-10-23, before building Slice 9:

- **Q33. Seed data is loaded by a Java `ApplicationRunner` under the Spring profile `seed`**, writing through the repositories. It runs only with that profile and skips when seed data already exists, so a restart duplicates nothing. Not SQL: activities must lie in the future relative to the start, and passwords are hashed at load time.
- **Q34. Data shape.** Around Prague (50.08, 14.42) within about 25 km; generated with a fixed random seed, so the evaluation is reproducible. Volunteers: 1 … 4 skills, some certifications, weekly availability, capacity 1 … 3, radius 5 … 50 km. Activities: in the next 1 … 14 days, mixed priorities, 1 … 4 required skills, some with a required certification, 1 … 5 spots, a couple with a weight override.
- **Q35. Ready-made states for the demo:** a few `PENDING` and `ACCEPTED` applications, one activity that has started (to complete in the demo) and one completed activity (so some volunteers have a history). Possible because the runner writes the repositories directly; the API never creates past activities.
- **Q36. Accounts** `seed_org1…3` and `seed_vol01…30`, password `password1`, emails `@example.com`. The admin comes from `.env` (V7); seed does not create one. The `seed` profile is for local work and demos only.
- **Q37. Sizes are configurable** in `volunnear.seed.*` (organizations, volunteers, activities, random seed), defaults 3 / 30 / 10, so the evaluation can use more volunteers without code changes.
- **Q38. Demo script** `docs/DEMO.md`: copy-paste `curl` calls (CSRF through a cookie jar), about five minutes: organization login, its activities, recommendations with the score breakdown, invitation, volunteer login and accept, notification in the log, completion and history, admin ban. Swagger UI as an alternative.
- **Q39. Docker.** The `Dockerfile` no longer copies `application.yml` or forces the `dev` profile; the profile comes from `SPRING_PROFILES_ACTIVE`. Compose does not enable `seed` by default; `docs/DEMO.md` shows how.

### Repository layout (after Slice 9)

Decided on 2026-10-23, after Slice 9:

- **Q40. Backend and frontend live in one new repository** (monorepo), so both sides change in one commit and one Claude Code session sees both. The developer creates the new repository, copies the backend into it and archives the old repositories; the git history of `VolunNearApp` stays in the old repository and is not carried over. Layout: `backend/` (everything that was at the root: `src/`, `pom.xml`, `mvnw`, `.mvn/`, `Dockerfile`, `.dockerignore`), `frontend/`, and shared at the root: `docs/`, `docker-compose.yml`, `.env.example`, `.gitignore`, `CLAUDE.md` (shared rules) with `backend/CLAUDE.md` and `frontend/CLAUDE.md` for the stack rules of each side.
- The follow-ups (frontend stack, first frontend scope, the `CLAUDE.md` rule) were answered on 2026-10-25: Q54 … Q66, section "Frontend" below.

### Platform administration (Slice 8b)

Answered on 2026-10-17, when Slice 8b was added to the plan:

- **Q21. The first admin is created by an SQL script that runs automatically on the application database**, as a Flyway migration. Proposed detail, to confirm at the start of the slice: username, email and the bcrypt hash of the password come from environment variables through Flyway placeholders, so no credential is committed. The migration runs once; changing the password later is a manual step.
- **Q22. An admin is a separate account with its own role `ROLE_ADMIN`.** It has no volunteer or organization profile, and the role is never added to a normal account.
- **Q23. A ban ends every session of the user in Redis and blocks all access.** The user's sessions are deleted at the moment of the ban, and login is refused (Slice 1). Implementation note: deleting sessions by user needs the indexed Redis session repository (`spring.session.redis.repository-type: indexed`, `FindByIndexNameSessionRepository`); the in-memory `MapSessionRepository` used by the tests is not indexed, so the tests need an indexed replacement or a Redis container. What a ban does to open applications and assignments: Q25 … Q27.
- **Q24. No audit log of admin actions for the MVP.**

Answered on 2026-10-23, at the start of Slice 8b (closes the open part of Q23 and details Q21, Q22):

- **Q25. Banning a volunteer releases everything not yet started:** pending applications become `WITHDRAWN`, pending invitations `DECLINED`, accepted applications on activities that have not started `WITHDRAWN`, and the organization gets the "volunteer withdrew" notification. Started and completed activities are not touched, so the history stays.
- **Q26. Banning an organization account cancels its activities that have not started** (`DRAFT` and `OPEN`) through the normal cancel path, with the "Activity cancelled" notifications (Q7b). Started and completed activities stay.
- **Q27. Unbanning only clears the flag.** Nothing withdrawn or cancelled is restored; the user can log in again.
- **Q28. First admin (detail of Q21).** Migration `V7` inserts it from Flyway placeholders fed by `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH` (a bcrypt hash, never the plain password). A missing variable stops the start. `.env.example` lists them; the test `application.yml` has test values.
- **Q29. `GET /admin/users`** with optional `query` (substring of username or email, case-insensitive), `role` and `banned`; paged like `GET /activities`. Item: `id`, `username`, `email`, `roles`, `banned`, `createdAt`.
- **Q30. `POST /admin/users/{id}/ban` and `/unban`.** Banning oneself or another admin is 409 with a clear message. Repeating a ban or unban is 200 without change.
- **Q31. No deletes.** Deactivating an organization = banning its owner account (Q26). Removing an activity = `POST /admin/activities/{id}/cancel`, the organization's cancel with notifications, without the ownership check. Users, organizations and activities are never deleted, which keeps history and links (and settles the "deleting a user leaves the organization" item).
- **Q32. `ROLE_ADMIN` opens only `/api/v1/admin/**`**; on volunteer and organization endpoints an admin gets 403 like any other wrong role.

### Evaluation

Answered on 2026-10-24, before building the evaluation (details Q20). The full method is in `docs/evaluation/METHOD.md`.

- **Q41. What each method models.** The bachelor's version showed volunteers an unsorted list of activities, so **random** (the volunteer takes any fitting activity) is the old solution. **nearest** is a careful volunteer of the old solution who picks the closest activity themselves: a stronger opponent. **smart** is the scoring function. No third baseline (the old list had no order, not even by date).
- **Q42. The comparison is a simulation of the whole distribution**, not of one API answer. Volunteer-driven process: volunteers act in a random order, round by round; in each round every volunteer with free capacity takes one activity with a free spot and is accepted at once; it stops when a round assigns nobody. Two smart variants: **smart-A** (main) is the same process, the volunteer takes the top-1 of `Ranker.rankActivities`; **smart-B** is the argmax of `FUNCTION.md` section 4, activities in order of priority (high first), then start time, then id, each filling its spots with the top-1 of `Ranker.rankVolunteers`.
- **Q43. One feasible set for every method:** `Feasibility` + `PlatformEligibility` + the volunteer's radius (the candidate set of the recommendation endpoint). The old system did not check feasibility, so the comparison is conservative: it favours the baselines. Stated in the thesis.
- **Q44. Metrics are raw quantities, none of them equal to a utility or to R:** fill rate, share of fully staffed and of empty activities, skill coverage and share of fully qualified assignments, distance (mean and median, km), priority-weighted fill and fill of HIGH + URGENT activities, participation (share of volunteers with at least one assignment). Skill coverage and distance are related to what the function optimises; fill, priority fill and participation are not. Stated in the thesis.
- **Q45. List quality at k = 5 and 10:** for each volunteer, the first k of the unsorted list, the k nearest and the top-k recommendations, compared by mean skill coverage, mean distance and share of HIGH + URGENT activities.
- **Q46. Data:** the seed generator (random seed 42) at three scales (organizations / volunteers / activities): S 3 / 30 / 10, M 5 / 200 / 40, L 10 / 1000 / 150.
- **Q47. 100 runs** with different volunteer orders. In one run all methods share the order (paired comparison). Reported: mean ± standard deviation, 95% confidence interval, and in how many runs smart beats each baseline.
- **Q48. Weights:** main results with the global weights of `application.yml` (0.4 / 0.4 / 0.2, half-distance 10 km). Sensitivity table at scale M with distance-heavy 0.6 / 0.2 / 0.2 and skill-heavy 0.2 / 0.6 / 0.2.
- **Q49. Code in the test sources**, package `com.volunnear.evaluation`: pure simulation and metrics with hand-computed unit tests, and a runner test tagged `evaluation` that seeds Testcontainers PostgreSQL and reads the profiles with `ProfileLoader`. Excluded from `./mvnw clean verify`; no production code or scoring rule changes.
- **Q50. Output:** `docs/evaluation/METHOD.md` (written by hand before running: aim, hypothesis, methods, metric formulas in `$...$`, parameters, threats to validity), `docs/evaluation/RESULTS.md` (generated, overwritten on each run: setup, numbered tables with captions and a "how to read" line, plain facts without conclusions, plain Markdown only, so it converts with pandoc and can be given to Claude to draft the thesis chapter) and `docs/evaluation/runs.csv` (raw numbers per run).

Answered on 2026-10-25, after the first run:

- **Q51. A fourth scale M-scarce** (5 / 120 / 60): at M and L every method fills almost every spot, so the fill-type metrics could differ only on the small S. M-scarce tests them at a useful size.
- **Q52. The seed keeps random skills**, independent of the activities. Making the data fit the method would weaken the result; the low absolute skill coverage is stated in `METHOD.md` section 8.
- **Q53. The weight sensitivity table (Q48) runs at M-scarce, not M** (decided 2026-10-25): at M every method fills everything, so only skill coverage and distance could move; under scarcity the effect of the weights on fill and priority fill is visible too.

### Frontend

Answered on 2026-10-25, before planning the frontend slices (follow-ups of Q40):

- **Q54. Stack:** React + Vite + TypeScript in `frontend/`, package manager npm (Node 26 and npm 12 on the development machine).
- **Q55. UI library: Mantine** (forms, tables, date and time pickers, notifications), so little UI code is written by hand. Look is secondary to function in the MVP.
- **Q56. Routing and data:** React Router for pages, TanStack Query for server state (cache, loading and error states). No global store such as Redux.
- **Q57. API types are generated** from the springdoc document `/v3/api-docs` with `openapi-typescript` (`npm run gen:api`, needs the backend running); the generated file is committed, so a changed backend DTO breaks the frontend build instead of failing at runtime.
- **Q58. Development setup:** the Vite dev server proxies `/api` to `http://localhost:8080`, so the browser sees one origin and the `SESSION` and `XSRF-TOKEN` cookies work without CORS. The existing CORS setting stays. Deploying the frontend (Docker, nginx, serving from Spring) is not part of the first version.
- **Q59. One `fetch` wrapper** for every call: before a modifying request it reads the `XSRF-TOKEN` cookie (calling `GET /api/v1/auth/csrf` if the cookie is missing) and sends it as `X-XSRF-TOKEN`; a 401 sends the user to the login page; error bodies of the backend are shown as messages.
- **Q60. Coordinates are picked on a Leaflet map** (`react-leaflet`, OpenStreetMap tiles): a click sets the point, two number fields show and edit it. No geocoding (address search). Used for the volunteer profile, the organization profile and activities.
- **Q61. First version covers:** register (volunteer or organization), login, logout; volunteer: profile with skills, certifications, availability and capacity, recommended activities with the score breakdown, apply, own applications with withdraw, invitations with accept and decline, history; organization: profile, activities (create, edit, publish, cancel, complete), applications with accept and reject, recommended volunteers with the score breakdown and invite; public: activity list and activity page. No admin screens: the admin uses Swagger or the API (changed by Q117: a small admin panel).
- **Q62. Score breakdown display:** three bars in [0,1] for geo, skill and priority, the weights α, β, γ used, and the total. It is the main visual argument for Smart Distribution in the defence.
- **Q63. UI language: English**, no i18n. Seed data and backend messages are in English.
- **Q64. Tests and definition of done:** Vitest + React Testing Library for logic (the API wrapper with CSRF, the breakdown component, key forms); no end-to-end tests. A frontend slice is done when `npm run lint && npm run build && npm test` pass in `frontend/`, plus `./mvnw clean verify` when the backend changed.
- **Q65. Frontend slices:** one slice = API calls + page(s) + tests, in the order F0 … F5 of `docs/PLAN.md`. After F2 the volunteer side can be shown.
- **Q66. `CLAUDE.md`:** the root file drops the frontend from "out of scope" and describes `frontend/` in the layout; `frontend/CLAUDE.md` holds the stack, commands and conventions (like `backend/CLAUDE.md`). Done in F0.

Answered on 2026-10-25, before Slice F1:

- **Q67. Response fields are required in the OpenAPI document.** A backend `OpenApiCustomizer` marks every property of the `*ResponseDto` schemas as required (a record always serialises every component); a field that can really be `null` carries `@Schema(nullable = true)` and becomes `T | null` in TypeScript. All response DTOs are annotated at once; a backend test checks the document.
- **Q68. Map:** Leaflet with OpenStreetMap tiles and the "© OpenStreetMap contributors" attribution. Without a stored point the map centres on the seed area (Prague, 50.08 / 14.42, zoom 11).
- **Q69. The volunteer's radius is drawn as a circle** around their point, to show the prefilter of the recommendations.
- **Q70. Address fields** (`locationName`, `city`, `region`, `countryCode`) are optional text inputs; `countryCode` is upper-cased and checked for two letters like the backend. No geocoding.
- **Q71. No "remove location" in the UI:** the partial update reads `null` as "keep" (Q2b), so the point can be moved but not cleared. The backend is not changed.
- **Q72. The profile page has four sections**, each with its own Save and its own `PUT`: basic fields, skills, certifications, availability.
- **Q73. Skills** in a `MultiSelect` grouped by category; **certifications** as rows of certification + optional `validUntil` (no past dates), each certification at most once. Skills have no levels (binary vectors, Q7).
- **Q74. Availability editor:** rows of day + start + end, add and remove, sorted by day and time. The client checks only `end > start` and the latest end 23:59; overlaps are left to the backend (`AvailabilityRules`), its 400 is shown above the list.
- **Q75. Capacity and radius** as number inputs with the backend limits (0 … 10, 1 … 500 km) and a one-line hint of what each one does.

Answered on 2026-10-26, before Slice F2 (Q77 changed by the developer, the rest as recommended):

- **Q76. Organization name:** `ActivityResponseDto` keeps only `organizationId`. The list shows no organization; the activity page loads the public `GET /organizations/{id}` for the name.
- **Q77. `acceptedCount` in `ActivityResponseDto`** (backend change): the number of `ACCEPTED` applications, so the UI shows "N of M spots free" or "Full". Filled everywhere the DTO is built: one `countAccepted` for a single activity, one grouped query (`ActivityRepository.acceptedCounts`) for the public list, the organization's own list and the recommendations. A pending application does not count.
- **Q78. "Already applied"** is read from `GET /volunteers/me/applications` on the activity page: any row for the activity (also withdrawn or rejected, Q5b) shows its status as a badge instead of the Apply button.
- **Q79. Apply** sits on each recommendation and on the activity page; success shows a notification and refreshes the recommendations (the activity leaves them, Q15) and the own applications. A 409 of the backend is shown as its message.
- **Q80. Other roles:** an anonymous user sees "Log in to apply" (back to the page after login); organizations and admins see no apply control.
- **Q81. Empty recommendations:** a static list of the reasons (availability, certifications, capacity, radius, free spot, not applied yet) with links to the profile and the public list; no client-side diagnosis. A profile without a point gets a separate hint, because then `u_geo` = 0 for every activity.
- **Q82. Recommendation count:** a select of 10 / 25 / 50, default 10 (Q6b); no paging.
- **Q83. Score breakdown component** (`src/score/ScoreBreakdown.tsx`, reused in F4): three bars for distance (α), skills (β), priority (γ) with the weight of this score next to each, the values and the total rounded to two decimals for display only, and the half-distance in a tooltip on the distance bar. Correction to the proposal: no "activity-specific weights" marker, because volunteer → activities always uses the global weights (Q12, `RecommendationIntegrationTest`); the weights shown are those of `score.weights`.
- **Q84. Public list:** no filters (the backend has none), 20 per page with paging; each entry shows title, priority, date and time, place, free spots, short description and required skills. No map in the list.
- **Q85. Activity page `/activities/:id`:** read-only Leaflet map with the activity's point only, full description, address, required skills and certifications, free spots, apply. A 404 (also a draft of another organization, Q4e) shows the "not found" page.
- **Q86. Times** are shown as the backend sends them (one configured zone, Q13), without conversion: "Sat 7 Nov 2026, 09:00–12:00".

Answered on 2026-10-26, before Slice F3 (all as recommended):

- **Q87. Organization profile:** one form with one `PUT /organizations/me`: name, description, website, address, city, country code, and the map picker of F1 without a radius circle. The point can be moved, not cleared (Q71). An emptied website is sent as `""` (it passes `@URL`), an empty country code is left out.
- **Q88. Own activities list:** a table (title, date and time, status, accepted / spots) with a client-side filter Active (DRAFT, OPEN, also started) / Finished (COMPLETED, CANCELLED) / All, default Active, and a "New activity" button.
- **Q89. Pages:** `/organization/activities/new`, `/organization/activities/:id` (the owner's page: details and status actions; F4 adds applications and recommended volunteers) and `/organization/activities/:id/edit`. Another organization's activity on the owner's page sends to its public page.
- **Q90. Create saves a draft** and opens the owner's page; publishing is its own button. No "save and publish" (two requests, half of which could fail).
- **Q91. Time input:** one date and two times, so an activity cannot span days (Q13); local times of the backend zone without conversion (Q86). The client checks only end > start; a start in the past is the backend's 400, shown above the form.
- **Q92. Weight override:** a switch, off by default (global weights); three inputs 0 … 1 in steps of 0.05 with the running sum; the client refuses a sum other than 1 (the backend's tolerance 1e-9). The hint says the override only ranks volunteers for this activity (Q12).
- **Q93. Priority** select LOW … URGENT, default MEDIUM, always sent.
- **Q94. A new activity starts at the organization's place** (point, city, address) when the profile has one; skills grouped by category and certifications in multi-selects.
- **Q95. Actions by status:** DRAFT: edit, publish, cancel; OPEN before the start: edit, cancel; OPEN after the start: complete, cancel; COMPLETED and CANCELLED: none. "Started" uses the browser's clock only to choose the buttons; the backend decides and its 409 is shown.
- **Q96. Cancel and complete ask for confirmation** in a modal (they cannot be undone and email volunteers); publish does not.
- **Q97. Editing an open activity is allowed** (backend rule); the form notes that applicants are not notified of changes. `spotsNeeded` below the accepted count is the backend's 409.

Answered on 2026-10-26, before Slice F4 (all as recommended):

- **Q98. Applications and recommended volunteers are two tabs on the owner's page** (`/organization/activities/:id`), "Applications (N)" and "Recommended volunteers"; a draft shows neither.
- **Q99. Applications table:** volunteer (name, city, the email only for `ACCEPTED`, Q7d), time and origin (application or invitation), status. Order: pending, accepted, then the rest, each group by time of applying.
- **Q100. Row actions:** a pending `APPLICATION` has Accept and Reject; a pending `INVITATION` shows "Waiting for the volunteer" (Q5i); others none. Reject needs only a pending application (also after the start, backend rule); Accept is disabled with the reason when the activity is not open, has started or has no free spot. Capacity and overlap 409s are shown as messages.
- **Q101. Reject asks for confirmation** (final by Q5b, emails the volunteer); accept and invite do not.
- **Q102. Volunteer profile in a modal** from the name: city, bio, skills, certifications with validity, availability. No date of birth.
- **Q103. Recommended volunteers:** 10 / 25 / 50 (like Q82), each with rank, name, city, skills, `ScoreBreakdown` and Invite; a line says whether the activity's custom weights or the platform's were used (in this direction the override applies, Q12).
- **Q104. After an invite** the recommendations and applications are read again: the volunteer leaves the recommendations (Q15) and appears as a pending invitation. Named 409s are shown as messages.
- **Q105. No request for recommendations** when the activity is not open or has started (it would be the 409 of Q6a); a text explains. An empty list of a full activity says that a free spot is needed.
- **Q106. After accept, reject or invite** the activity, its applications, its recommendations and the own list are read again (all queries under `['activity', id]`), so free spots stay right.
- **Q107. Availability in the profile modal** is a list per weekday ("Sat 08:00–14:00"); whether it covers the activity is not highlighted, the backend has already checked it for recommendations.

Answered on 2026-10-26, before Slice F5 (all as recommended):

- **Q108. `/volunteer/applications`** has two sections, Invitations first (they wait for the volunteer), then Applications. A row: activity title (link to `/activities/:id`), date and time, own status, the activity's status when it is not `OPEN`, actions.
- **Q109. Filter Current / Past**, default Current: current = own status `PENDING` or `ACCEPTED` on an activity that is not `COMPLETED` or `CANCELLED`; past = the rest.
- **Q110. Actions:** a pending application: Withdraw; an accepted one: Withdraw before the start, "Started: cannot withdraw" after; a pending invitation: Accept and Decline, Accept disabled with the reason when the activity is not open or has started. Free spots, capacity and overlaps are not in `MyApplicationResponseDto`; their 409 is shown as a message.
- **Q111. Withdraw and decline ask for confirmation** (final by Q5b; withdrawing an accepted application says the spot is freed); accept does not.
- **Q112. Rows of cancelled or completed activities have no actions**, only a badge of the activity's status (the backend would allow withdrawing an accepted application of a cancelled activity before its date; it serves no purpose).
- **Q113. `/volunteer/history`:** the backend's order (newest first), each with title (link), date and time, organization and city; a count "N activities completed" (the list length, not a score); a hint when empty.
- **Q114. The menu counts pending invitations** that can still be accepted next to "My applications", from the cached own-applications query, because email is only a stub.
- **Q115. The activity page keeps only the status badge** with a link "Manage in My applications"; the actions live on one page.
- **Q116. After withdraw, accept or decline** the own applications, the recommendations (free capacity changed) and the activity (accepted count) are read again.

Answered on 2026-10-26, before Slice F6 (admin panel, after the MVP slices; all as recommended):

- **Q117. Scope change: a small admin panel in the frontend**, only on the existing endpoints of Slice 8b: users (search, ban, unban) and cancelling an activity. No deletes, analytics or audit log (Q24, Q31). Changes Q61 and the out-of-scope list of `CLAUDE.md`.
- **Q118. Admin menu "Users" and "Activities"**; after login an admin lands on `/admin/users`. `/admin/**` pages sit under `RequireRole role="ADMIN"`.
- **Q119. Users table:** username, email, role, registration date, status (Active / Banned); search field (300 ms debounce, substring of username or email, Q29), role filter, status filter; 20 per page.
- **Q120. Ban for active volunteers and organizations, Unban for banned ones; no button for admins** (oneself included), whose ban the backend refuses (Q30).
- **Q121. Ban asks for confirmation with the consequences of the role:** volunteer: sessions end, applications and invitations of activities that have not started are withdrawn or declined, organizations are emailed (Q23, Q25); organization: its activities that have not started are cancelled with emails (Q26). Unban does not ask; its notification says nothing is restored (Q27).
- **Q122. "Cancel as admin" on the public activity page** for an `OPEN` activity (also one that has started; the backend allows it), after a confirmation that volunteers and the organization are emailed (Q31). Activities are found through the public list; there is no admin list of all activities (no such endpoint), and drafts stay invisible to admins (Q4e).

Answered on 2026-10-26, before the design pass (D1 … D3; brief of the developer: a noble site in Navy Blue with translucent layers, strict and business-like but welcoming, a restrained refined readable font, nothing bright or flashy; references: "Discover", "Wealth", "Journal X"; all as recommended):

- **Q123. Fonts:** Cormorant Garamond (600) only for page titles and hero headings (h1, h2), Manrope for everything else, both with Cyrillic.
- **Q124. Fonts are self-hosted** through `@fontsource/cormorant-garamond` and `@fontsource/manrope`, so the defence works without internet.
- **Q125. Icons: `@tabler/icons-react`** (thin line icons, Mantine's companion set).
- **Q126. Dark and light parts:** the landing page, login and registration are deep navy with translucent glass elements; the working pages are light (cool off-white page, white cards with thin borders, navy text and accents); the header is translucent navy with blur everywhere.
- **Q127. Hero without a photo:** a deep navy gradient with a faint dot grid and a glass card with a sample score breakdown.
- **Q128. Top navigation instead of the sidebar:** logo left, the role's links in the middle, user and log out right; a burger and a drawer on phones.
- **Q129. Logo:** the monogram VMP in a thin ring with "Volunteering Management Platform" in small spaced capitals (monogram only on small screens); browser title "VMP — Volunteering Management Platform".
- **Q130. Muted colours** for priorities and statuses in one family (theme colours `navy`, `amber`, `wine`, `sage`, gray); skill tags as thin outlined pills.
- **Q131. Readability fix after the developer's review (2026-10-26):** Manrope 400 and Mantine's pale grey faded on white (Firefox on Linux). Variant B of a side-by-side sample (Manrope 400 vs 500, IBM Plex Sans, Inter) was chosen: body text weight 500, input labels 600, secondary ("dimmed") text `#4a5568`, set through the `MantineProvider` `cssVariablesResolver` (Mantine writes its colour variables at runtime, after any CSS file).
- **Q132. Page titles on the light pages in Source Serif 4 600** (`@fontsource/source-serif-4`, self-hosted), also the total of the score breakdown; Cormorant's hairlines faded on white. Chosen from a side-by-side sample (Cormorant 600/700, Source Serif 4, Lora, Manrope 700/800). Cormorant stays on the deep navy pages (landing, sign-in) and in the logo, where light on dark does not fade. Changes Q123 for the light pages.

