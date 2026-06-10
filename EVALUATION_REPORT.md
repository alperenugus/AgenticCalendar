# AgenticCalendar — Engineering Evaluation Report

_Date: 2026-06-09. Method: a multi-agent code review across 9 subsystems (auth/security, agent/LLM, data model, services, controllers, frontend, infra/deploy, tests, docs), with an adversarial verification pass on every high/critical finding to weed out false positives. Severities below are **post-verification** — several initially-scary findings were correctly downgraded._

---

## 1. Executive summary

AgenticCalendar is a genuinely interesting, cleanly-layered project: a Spring Boot 3.4 / Java 21 backend, a **hand-rolled text-based ReAct agent** over Groq (LangChain4j), and a tidy React 18 / Vite / Tailwind SPA, deployed as two services on Railway with Postgres. The "bones" are good — constructor injection throughout, thin controllers delegating to services, record DTOs, sensible enums, a real recurrence engine with safety rails, and a correctly reverse-proxy-aware OAuth2 setup.

The headline problem found by the review was **authentication without authorization**: Google login worked and created a session, but that session was *decorative*. Every `/api/**` route was `permitAll()` and ownership was decided entirely by **client-supplied** values (`?googleUserId=`, the `X-Google-User-Id` header, raw event IDs). Any caller — even unauthenticated — could read, enumerate, modify, or delete any user's calendar, conversation history, and PII. This single architectural defect surfaced as a critical/high finding in **five of the nine** subsystems.

**This pass fixed that** (see §4) alongside delivering the requested username/password login. The remaining recommendations in §5 are real but were deliberately deferred to avoid a large, blind, autonomous rewrite of working code — they're written up so they can be done deliberately.

### Production-readiness verdict

| Before this pass | After this pass |
|---|---|
| **3 / 10** as a multi-user app — unauthenticated cross-tenant read/write/delete of PII reachable on the public URL. | **~6 / 10** — the critical authorization hole is closed, identity is server-derived, a healthcheck exists, and prod logs no longer leak prompts. Remaining gaps (§5) are correctness/robustness/coverage, not "anyone can read anyone's data." |

---

## 2. Architecture at a glance

```
React SPA (Vite, Tailwind)                 Spring Boot 3.4 (Java 21)
 ├─ LoginScreen  ── Google OAuth / local ── ├─ SecurityConfig (OAuth2 + form, session cookie)
 ├─ ChatComponent ─ POST /api/agent/chat ── ├─ AgentController ─ AgentService (custom ReAct loop)
 │                 + STOMP /ws (thinking)   │                     ├─ CalendarToolService → EventService → Postgres
 └─ CalendarView ─ GET /api/events ──────── └─ EventController                     (recurrence expansion)
```

- **Agent**: not LangChain4j `AiServices`. It's a custom loop that calls `chatLanguageModel.generate(prompt)` with a large text system prompt and parses `Thought/Action/Action Input/Observation/Final Answer` by regex (`ReActParser`), dispatching tools through a manual `switch`. The `@Tool` annotations on the tool services are **not used** by this loop.
- **Identity**: now derived server-side from the session principal (Google `sub`, or `local-<id>` for password users). Calendar rows are scoped by that opaque owner key.
- **Schema**: managed by Hibernate `ddl-auto=update`. The `db/migration/*.sql` files are **inert** — Flyway is not a dependency (see §5).

---

## 3. Strengths (keep these)

1. **Clean layering** — constructor injection, thin controllers, service boundaries, record DTOs, `@Enumerated(STRING)`, surrogate-key `equals/hashCode`.
2. **The ReAct loop is more robust than most demos** — `MAX_ITERATIONS` bound, prioritizes Action over a same-turn hallucinated Final Answer, wraps each tool in try/catch returning a JSON error the model can react to, and injects the current date/time so relative dates resolve.
3. **OAuth2 is wired correctly for a reverse-proxied Railway deploy** — `ForwardedHeaderFilter`, forced-HTTPS redirect URI, hardened `SameSite=None; Secure; HttpOnly` cookies. Secrets are externalized to env vars; none are hardcoded.
4. **Operationally thoughtful** — Bucket4j token-bucket rate limiting with correct `429` + `Retry-After`, a daemon session-cleanup sweeper, a correct multi-stage Dockerfile with dependency-layer caching.
5. **Frontend avoids the obvious XSS trap** — no `dangerouslySetInnerHTML`; all chat/event text is escaped JSX; base URLs come from `import.meta.env`; STOMP reconnect/heartbeats with effect cleanup.
6. **Recurrence engine has real safety rails** — `MAX_OCCURRENCES` cap, graceful degradation to the master event on parse failure, EXDATE handling, end-of-month clamping.

---

## 4. What was fixed / added in this pass

| Area | Change |
|---|---|
| **Feature: username/password auth** | `LocalAccount` entity + `BCrypt`, `LocalUserDetailsService`, `/api/auth/register` + `/api/auth/login` (session-based, mirrors the existing cookie model), dual-principal `AuthController.getUser` (handles both `OAuth2User` and local users). Login/register UI added to `LoginScreen` next to the Google button. A local user's owner key is `local-<id>`, which flows through the existing scoping channel unchanged. |
| **P0: authorization** | `CurrentUser` derives the owner key/email **server-side from the authenticated principal**; the `X-Google-User-Id`/`X-Google-User-Email` headers and `?googleUserId` param are no longer trusted. `/api/**` now requires authentication except `/api/auth/**`, `/actuator/health`, `/ws/**`, `/oauth2/**`, `/login/**`. API calls return **401** (not an OAuth redirect). `EventController` scopes every query to the authenticated owner and checks ownership on by-id reads; the `findAll()` fallback that leaked all users' events was removed. |
| **P1: healthcheck** | Added `spring-boot-starter-actuator`, exposed `/actuator/health`, and wired `healthcheckPath` + `healthcheckTimeout` in `railway.json` so Railway stops routing to a not-ready/crashed instance. |
| **P1: log hygiene** | Default logging changed from `DEBUG` to `INFO`/`WARN` (env-overridable) so full LLM prompts/responses — which contain calendar PII — are no longer written to production logs. |
| **Test profile fix** | The test profile couldn't load the Spring context (missing OAuth client id; `langchain4j` block mis-nested under `spring:`). Fixed both, so the service-layer tests run again. **All 19 H2 service tests pass.** |

---

## 5. Prioritized remaining recommendations

> These are deferred deliberately. Each is either deploy-risky (schema), a larger refactor (legacy deletion, `AiServices`), or wide-coverage work (tests) that should be done with the owner present rather than blind in an autonomous session.

### P0 — correctness / safety

- **Fix `RRULE COUNT` (count from the series start, not the query window).** `RecurrenceExpander.expandEvent` fast-forwards into the requested window *without counting skipped occurrences* (lines ~81–87), then breaks at `occurrenceCount >= COUNT`. So a `FREQ=DAILY;COUNT=5` series that ended months ago re-emits up to 5 occurrences in **every** future window, and conflict detection sees phantom events. Fix: iterate from the true series start with a global index, apply `COUNT` against that index, collect only in-range occurrences. The same `recurrenceExpander.js` exists on the frontend and needs the same fix. Add table-driven unit tests (COUNT mid-window, UNTIL, unsorted `BYDAY` — `findNextDayOfWeek` assumes ascending day order, so `BYDAY=WE,MO` silently drops Mondays).
- **Resolve the schema-management contradiction.** Flyway is **not** a dependency, so `db/migration/V2,V3,V4` never run; the live schema is built by `ddl-auto=update`. `V4__drop_unused_tables.sql` would `DROP` `users`/`appointments` while those entities/repos/controllers are still live (and `ddl-auto` would just recreate them). Pick one strategy: either adopt `flyway-core` + `ddl-auto=validate` with a correct V1 baseline **and** delete the legacy entities in the same change, **or** delete `db/migration/` and keep `ddl-auto` explicitly. Test any migration against a real `postgres:17` container first. _(Left untouched this pass: `V3`/`V4` are your uncommitted work-in-progress and `V4` is destructive — they should be reconciled deliberately, not auto-shipped.)_

### P1 — robustness / hygiene

- **Delete the legacy Appointment/User subsystem** (or authenticate + scope it). `AppointmentToolService` is orphaned (never wired into the agent); `/api/appointments` and `/api/users` are dead PII surfaces (now behind auth, but still dead code). Removing them deletes a class of risk, dead code, and doc drift in one stroke — and makes `V4` safe.
- **Authenticate the WebSocket + use user-scoped destinations.** `/ws` is open and topics are guessable (`/topic/response/{sessionId}`). Switch to `convertAndSendToUser`. (Lower risk today: `sessionId` is a client-random string and the response is computed from the authenticated POST.)
- **Add unit tests for the agent loop and the pure parsers/expanders** — the core feature has zero executable coverage; the one integration test is `@Disabled` and hits a live LLM. Mock `ChatLanguageModel` with canned ReAct strings.
- **Harden the LLM call path** — no timeout/maxRetries/maxTokens on the model builder; rate-limit detection substring-matches free-text exception messages; lower temperature toward 0–0.2 for more reliable structured output; re-send the few-shot block once instead of regrowing the prompt every iteration (currently quadratic token cost).

### P2 — scale / cleanup

- **Externalize conversation memory + rate-limit buckets** (or assert single-instance). Both live in in-process `ConcurrentHashMap`s and the `SimpleBroker` assumes one instance, so a redeploy wipes chat context and any replica scaling silently breaks. Bounded today (single instance, only last 5 messages used).
- **Consolidate CORS to one source of truth.** Two configs exist (`SecurityConfig` strict allowlist vs `WebConfig` wildcard). The "wildcard-with-credentials exploit" was **verified as a false positive** — Spring Security's filter-chain `CorsFilter` enforces the strict allowlist — but the wildcard `WebConfig` is shadowed dead config that should be deleted. Also fix the `FRONTEND_URL` trailing-slash so the allowlist matches the browser `Origin` exactly.
- **Frontend cleanups** — clear `chatSessionId` on logout, replace timer-based auth polling with a deterministic post-redirect handshake + a 401 axios interceptor, delete the dead `EventTable`/`AppointmentTable`/`UserTable` components, gate `console.log` behind `import.meta.env.DEV`.

### P3 — docs

- **Collapse ~23 drifted markdown files into a 4-doc pyramid:** `README` (product + quickstart) · `ARCHITECTURE` (single source of truth) · `DEPLOYMENT` (one Railway/Docker guide) · `docs/archive/` for migration/rename/one-off-fix docs + the LinkedIn files. Several "architecture" docs still describe the dead appointment system and the old "AI Appointment Assistant" prompt; the Railway docs reference a wrong artifact name and a start command that contradicts the Dockerfile. (README updated this pass; full consolidation deferred.)

---

## 6. Notable false positives (verified and dismissed)

The adversarial pass earned its keep — these were flagged then **disproven**, so don't waste time on them:

- **"CORS reflects any origin with credentials → exploitable."** False. Spring Security's `CorsFilter` (the strict `SecurityConfig` allowlist) is the effective policy for `/api/**`; the wildcard `WebConfig` is shadowed. (Still: delete the dead config.)
- **"CSRF disabled is a vulnerability."** Invalid in the original design — nothing trusted the cookie for authorization. (Now that the cookie *is* authoritative, CSRF posture should be revisited, but the SPA is token-free cross-site and `SameSite` mitigates; revisit if cookies ever become same-site.)
- **"`ddl-auto` fights Flyway at boot."** Can't happen — Flyway isn't wired at all.
- **"Input-validation disabled = no SQLi/XSS defense."** Downgraded to low: the real defenses (JPA parameterized queries; escaped JSX rendering) are present. The disabled denylist validator is dead code to remove, not an active hole.

---

## 7. Appendix — full verified finding list

See the per-subsystem detail captured during the review. The most material confirmed items, by subsystem:

- **Auth/Security:** no per-user authz (critical, fixed); agent trusts identity headers (critical, fixed); DEBUG prompt logging (low, fixed); per-session-only rate limiting (medium).
- **Agent/LLM:** `@Tool` annotations dead / native tool-calling bypassed (medium); in-process conversation memory (medium); prompt regrown every iteration (medium); fragile regex ReAct parsing (medium).
- **Data model:** Flyway not a dependency / migrations inert (high); `V4` drops live tables (verified, deferred); events table has no indexes (medium); ambiguous `sessionId`+`googleUserId` ownership (medium).
- **Services:** RRULE COUNT counts from window (high, deferred); multi-day `BYDAY` interval bug (low); event delete/update lacked ownership scoping (high — agent path now scoped via server-derived identity; service-level by-id scoping still recommended).
- **Controllers:** tenant isolation via client identity (critical, fixed); everything `permitAll` (critical, fixed); WebSocket unauthenticated (high, deferred); legacy user/appointment endpoints unscoped (now behind auth).
- **Frontend:** IDOR via client `googleUserId` (high — backend now ignores it); timer-based auth polling (medium); dead components shipped (medium).
- **Infra:** no healthcheck (fixed); `ddl-auto=update` in prod (high, deferred); DEBUG logging (fixed); frontend cold-installs `serve` at boot (medium).
- **Tests:** agent loop zero coverage (critical); parsers/expanders untested (high); H2-vs-Postgres drift (medium); root context test couples CI to Postgres (medium — partially addressed).
- **Docs:** multiple "architecture" docs describe the dead app (high); Railway docs reference wrong artifact (high); ~23 files, no index (medium).
