# CLAUDE.md

## Project
Volunteer management platform with **Smart Distribution**: it recommends and assigns volunteers to activities using a multi-attribute scoring function. The function is specified in `docs/FUNCTION.md` and is the source of truth for the matching logic.

This is a master's degree project (an update of a bachelor's project) and an MVP. Prefer simple, working, tested code over completeness.

## Layout
Monorepo: `backend/` (Spring Boot API; its stack, commands and conventions are in `backend/CLAUDE.md`), `frontend/` (React web client of that API; stack, commands and conventions in `frontend/CLAUDE.md`), and shared `docs/`, `docker-compose.yml`, `.env.example` at the root. Paths to code in older `docs/` texts (`src/main/...`) mean `backend/src/main/...`.

## Smart Distribution rules
- Implement exactly what `docs/FUNCTION.md` specifies: components `u_geo`, `u_skill`, `u_prio`, weights on the simplex, feasibility filter, argmax.
- The scoring code is pure, with no database access. It works on plain domain records and is mapped from entities in a separate adapter class.
- Every component must stay in [0,1]. Guard edge cases: zero skill vectors, missing coordinates.
- Always return the score breakdown (geo, skill, priority, total), not only the total.
- Unit-test scoring with hand-computed examples before wiring it into endpoints.
- If the function or its edge cases are ambiguous, list the ambiguity and ask. Do not silently choose. Decisions already taken are in `docs/ANALYSIS.md` section 6.

## MVP scope
In scope: a web frontend for volunteers and organizations (first version: `docs/ANALYSIS.md` Q61), accounts and roles, volunteer profiles (location, skills, certifications, availability, capacity), organization profiles, activities (with spots needed), applications and invitations, recommendations, email notifications (stub is fine), completion and history.

Out of scope for now: chat, ratings, payments, mobile app, analytics dashboards, learning weights from data, social features, admin screens in the frontend (the admin uses the API).

## Working agreement
0. At the start of a session read `docs/TODO.md`: its "Next session: start here" section says where the work stands. Rewrite that section at the end of the session.
   Local notes on how the developer works and on this machine are in `docs/LOCAL_NOTES.md`. The file is not committed (it is in `.gitignore`) and is imported here so it loads with this file: @docs/LOCAL_NOTES.md
   Never commit or force-add it. Update it in the same session when a preference or a machine fact changes.
1. For any task larger than a small edit, propose a plan first and wait for approval.
2. Work in small vertical slices. Backend slice = migration + entity + repository + service + controller + tests; frontend slice = API calls + page(s) + tests.
3. Run `cd backend && ./mvnw clean verify` before declaring a backend slice done, and `cd frontend && npm run lint && npm run build && npm test` for a frontend slice (both when both changed). Report failures honestly.
4. Do not add features, dependencies or abstractions beyond the current slice.
5. Keep `docs/TODO.md` up to date: tick completed items and add newly discovered work.
6. Explain non-obvious decisions briefly in your reply. The developer is learning the codebase.
