# CLAUDE.md — AgenticCalendar

Project-level operating guide for Claude Code. The global `~/.claude/CLAUDE.md` rules (§1 Think before coding, §2 Simplicity, §3 Surgical changes, §4 Goal-driven, §5 Verify deploys) always apply and win on conflict. This file adds project specifics.

---

## What this is

An AI calendar assistant: chat in natural language to create/move/cancel events. Two Railway services + Postgres.

- **Backend** — Spring Boot 3.4, Java 21, `com.agent.agenticcalendar`. A **hand-rolled text-based ReAct agent** (NOT LangChain4j `AiServices`) calling Groq (`llama-3.3-70b-versatile` in prod) via `chatLanguageModel.generate(prompt)`. Tools are dispatched by a manual `switch` in `AgentService.executeTool`. **The `@Tool` annotations on the tool services are dead code** — the loop does not use them; if you add a tool you must add it to the `switch` AND to the system prompt's tool list.
- **Frontend** — React 18 + Vite + Tailwind SPA (`frontend/`). Chat panel (STOMP/SockJS over `/ws`), a calendar with client-side recurrence expansion, and `LoginScreen` (Google + email/password).
- **DB** — Postgres. Schema is managed by **Hibernate `ddl-auto=update`**. ⚠️ The files in `backend/src/main/resources/db/migration/` are **INERT** — Flyway is not a dependency, so they never run. Do not assume any `V*.sql` migration has executed.

See `EVALUATION_REPORT.md` for the full code-review findings and the prioritized backlog.

## Production

- Frontend: https://agenticcalendarfrontend-production.up.railway.app/
- Backend:  https://agenticcalendarbackend-production.up.railway.app
- Health:   `GET /actuator/health` → `{"status":"UP"}`

## Identity & authorization model (important)

- Two sign-in methods: **Google OAuth2** and **username/password** (`LocalAccount` + BCrypt; `/api/auth/register`, `/api/auth/login`, session cookie).
- Every request's identity is **derived server-side** from the session principal via `security/CurrentUser`:
  - Google user → owner key = OIDC `sub`.
  - Local user → owner key = `local-<accountId>`.
- Calendar rows are scoped by that owner key (the `Event.googleUserId` column — the name is legacy; it just holds the opaque owner key).
- **NEVER trust client-supplied identity** (`X-Google-User-Id` header, `?googleUserId=` param). The frontend may still send them; the backend ignores them. This is the fix for the original cross-tenant IDOR.
- `/api/**` requires authentication **except**: `/api/auth/**`, `/actuator/health`, `/ws/**`, `/oauth2/**`, `/login/**`. Unauthenticated API calls get **401** (not an OAuth redirect).

## Build & test (run before any deploy — global §5)

```bash
# Backend — this is EXACTLY what the Dockerfile runs:
cd backend && ./mvnw clean package -DskipTests      # must produce target/agenticcalendar-0.0.1-SNAPSHOT.jar

# Backend service-layer tests (H2, offline) — context-load smoke + unit:
cd backend && ./mvnw test -Dtest='AgentServiceTest,UserServiceTest'   # all green

# Frontend:
cd frontend && npm run build
```

Test notes: `-DskipTests` skips test *execution*, not *compilation* — a broken test file still fails the build. Some pre-existing tests (`AgenticCalendarApplicationTests`, `DataInitializationTest`) connect to a real Postgres and `AppointmentServiceTest.deleteAppointment` has a brittle business-rule guard; these are known-broken and unrelated to feature work. The deploy skips tests, so they don't block it, but don't let them mask a *new* failure.

## Deploying to Railway (CLI)

Project: `AgenticCalendar` · services: `AgenticCalendarBackend` (Dockerfile, repo root), `AgenticCalendarFrontend` (Nixpacks, `rootDirectory=/frontend`), `Postgres`. Auto-deploys from GitHub `main`.

```bash
railway link --project AgenticCalendar           # once (the CLI may be linked to another project)
```

**`main` is branch-protected and requires a PR with an approving review** — you cannot push directly to `main`, and `gh pr merge --admin` is blocked (needs a second reviewer). So either (a) open a PR and have the owner merge it, or (b) deploy directly with `railway up` (the owner authorized this):

```bash
# Backend — from repo root (uses the root Dockerfile):
railway up --service AgenticCalendarBackend --detach

# Frontend — GOTCHA: `railway up` from repo root attaches the root railway.json
# (DOCKERFILE builder) to the frontend and breaks its Nixpacks build. Move it aside:
mv railway.json /tmp/railway.json.bak
railway up --service AgenticCalendarFrontend --detach        # Nixpacks + rootDirectory=/frontend
mv /tmp/railway.json.bak railway.json
```

Then **verify, don't trust** (global §5):

```bash
railway deployment list --service AgenticCalendarBackend     # newest row must be SUCCESS
curl -s https://agenticcalendarbackend-production.up.railway.app/actuator/health   # {"status":"UP"}
railway logs --deployment <id> --service AgenticCalendarBackend --lines 250 \
  | grep -iE "WARN|ERROR|FATAL|fallback|missing|not configured"   # scan startup
```

A benign WARN you'll see on boot: `constraint ... of relation "local_accounts" does not exist, skipping` — that's `ddl-auto` creating the new table; ignore it.

If you deploy via `railway up`, **the live code can drift from `main`** until the open PR is merged. Note the open PR in your summary so the owner keeps `main` in sync.

## Testing on production with Playwright (do this for every feature)

Run QA against the **production URL**, not localhost (the owner's standing instruction). Google OAuth can't be automated (consent/captcha), so **create a fake username/password account** to sign in:

1. `browser_navigate` to the frontend URL → "Create Account" tab → register a throwaway email + password (≥8 chars).
2. Verify: chat creates/lists events; the calendar renders created events; rescheduling/canceling works.
3. Confirm isolation: a second fresh account sees an empty calendar (data is per-user).

## Standing instructions from the owner

- Deploy via the Railway CLI; run all tests against production.
- Use Playwright MCP for production testing; a fake user is fine.
- Keep the docs (`README.md`, `EVALUATION_REPORT.md`, this file) updated as the app changes.
