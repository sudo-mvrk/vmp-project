# frontend/CLAUDE.md

Web client of the backend API. Project rules, MVP scope and the working agreement are in the root `CLAUDE.md`; frontend decisions are Q54 … Q66 in `docs/ANALYSIS.md` section 6, the slices F0 … F5 in `docs/PLAN.md`.

## Stack
- React 19 + TypeScript (5.9: `openapi-typescript` does not support TypeScript 6 yet), Vite, npm
- Mantine (core, form, dates, notifications, hooks) for the UI; the look is set in `src/theme.ts` only
- React Router (`react-router`, data router in `src/router.tsx`), TanStack Query for server state; no global store
- Vitest + React Testing Library (jsdom) for tests; oxlint for lint (the default of the Vite template)

## Commands
All in `frontend/`. The backend must run for `dev` and `gen:api`.
- Install: `npm install`
- Dev server: `npm run dev` (http://localhost:5173; `/api`, `/swagger-ui` and `/v3/api-docs` are proxied to `http://localhost:8080`, another backend with `VITE_PROXY_TARGET=http://localhost:18080 npm run dev`)
- Done check of a slice: `npm run lint && npm run build && npm test`
- Regenerate API types after a backend DTO change: `npm run gen:api` (another backend with `API_DOCS_URL=http://localhost:18080/v3/api-docs npm run gen:api`); commit `src/api/schema.d.ts`

(Update this section if the commands change.)

## Architecture and conventions
- Folders by feature under `src/`: `api` (client and types), `auth`, `layout`, `pages`, `map`, `dictionary` (skills and certifications, `skillOptions`), `volunteer`, `organization`, `activity` (public list and page), `score` (the breakdown component, also for F4), `admin` (F6, Q117). Tests sit next to the code as `*.test.ts(x)`; shared test helpers are in `src/test`: `render.tsx`, `fixtures.ts` (users, activity, score, application), `fetchRoutes.ts` (`stubFetch` answers by "METHOD path", for pages that make several requests).
- Every backend call goes through `api()` in `src/api/client.ts`: it adds the CSRF header, turns error bodies into `ApiError` (`message`, `fieldErrors`) and reports a 401. Never call `fetch` directly.
- Types of requests and responses come from the generated `src/api/schema.d.ts` through short aliases in `src/api/types.ts`. Do not hand-write a type the OpenAPI document has. Response fields are required in the generated types (backend `OpenApiConfig`, Q67); a field that can be null is `T | null` only if its record component has `@Schema(nullable = true)`, so add that annotation to a new nullable response field.
- Server data is read with `useQuery` and changed with `useMutation`; after a change invalidate the affected queries. The current user is the query `currentUserKey` (`src/auth/session.ts`).
- Pages of one role sit under a `RequireRole` layout route. The backend checks every rule again; the frontend checks only for navigation.
- Show backend messages as they come: field errors next to the field (`form.setErrors(error.fieldErrors)`), other errors in an `Alert` or a notification.
- Scores are shown as the backend sends them (geo, skill, priority, total, weights); the frontend never computes or changes a score.
- UI text is English (Q63).
- Look (Q123 … Q130): colours only from the theme (`navy`, `amber`, `wine`, `sage`, `gray`; no bright Mantine colours like `red` or `teal`), a serif only for h1/h2 (Source Serif 4 on light pages, Cormorant Garamond on the deep navy ones and in the logo, Q132), icons from `@tabler/icons-react` with `stroke={1.5}` or thinner. Every working page starts with `common/PageHeader` inside `<Stack maw={1080 or 880} mx="auto" w="100%">`; a deep navy page (landing, sign-in) sets `handle: { fullBleed: true }` on its route and uses `common/dark.module.css`.
- Pure logic (request mapping, validation, option lists) lives in plain `.ts` modules next to the component (`volunteer/availability.ts`) and is unit-tested there; component files export only components.
- Leaflet does not run in jsdom: tests replace it with `vi.mock('react-leaflet', () => import('../test/leafletMock'))`, and `clickMap(lat, lng)` simulates a click on the map.
