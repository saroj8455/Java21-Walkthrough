# CLAUDE.md — AI assistant guide for this repo

> Read this file first, then [MEMORY.md](MEMORY.md) (current status + next steps), then [AGENT.md](AGENT.md) (working rules).
> Together they let a fresh session pick up work in a couple of minutes.

## What this project is
A **learning project**: a full-stack app for an engineer who is experienced in **Angular 2–17 and Node.js**
and is **new to Java**. Every change must also *teach*: comment the code, compare to Angular/Node concepts,
and update the docs in [docs/](docs/).

Domain: simple **Projects → Tasks** tracker. The first feature is a dashboard **summary**.

## Stack (fixed — do not change without asking the user)
| Layer     | Tech |
|-----------|------|
| Backend   | Java 21, Spring Boot 3.5.x, Maven, Spring Data JPA (Hibernate 6), Flyway, Bean Validation, Actuator, springdoc-openapi |
| Database  | PostgreSQL 16 (+ pgAdmin 4) |
| Frontend  | **Angular 17 only** (standalone components, signals, `@if/@for` control flow), **Angular Material 17** for UI, **Bootstrap 5 grid + utilities only** for layout/spacing |
| Runtime   | Docker Compose — `docker compose up -d --build` starts everything |

## Commands
```bash
# Everything (from repo root)
docker compose up -d --build          # build + start all 4 containers
docker compose ps                     # status / health
docker compose logs -f backend        # follow API logs
docker compose down                   # stop (keeps DB data)
docker compose down -v                # stop + DELETE DB data (re-runs Flyway seed)

# Backend tests (no local Maven needed — runs in a container, caches deps in volume walkthrough-m2)
docker run --rm -v "${PWD}/backend:/workspace" -v walkthrough-m2:/root/.m2 -w /workspace maven:3.9.9-eclipse-temurin-21 mvn -B test

# Frontend dev server with hot reload (needs `npm install` in frontend/ once; backend must be on :8080)
cd frontend && npm start               # http://localhost:4200, proxies /api via proxy.conf.json
```
Local machine: Windows 11, Java 21 (Temurin) installed, **no Maven installed**, Node 26 installed
(Angular 17 officially supports Node 18/20 — Docker builds use `node:20-alpine`).

## Source control
Public GitHub repo: https://github.com/saroj8455/Java21-Walkthrough (default branch `main`).

## URLs
- App: http://localhost:4200
- API: http://localhost:8080/api/v1/summary
- Swagger UI: http://localhost:8080/swagger-ui.html (also proxied at :4200)
- Health: http://localhost:8080/actuator/health
- pgAdmin: http://localhost:5050 (server pre-registered; DB password `walkthrough`)

## Repository layout
```
backend/                       Spring Boot API
  src/main/java/com/walkthrough/api/
    WalkthroughApiApplication  entry point
    config/                    @Configuration beans (Clock, ...)
    common/                    cross-cutting: GlobalExceptionHandler (RFC 7807 ProblemDetail)
    project/                   Project entity, repository, query projections
    task/                      Task entity, enums, repository, query projections
    summary/                   feature: controller -> service -> dto/
  src/main/resources/
    application.yml            config (env vars with defaults)
    db/migration/V*.sql        Flyway migrations (schema + seed)
  src/test/java/...            JUnit 5 + Mockito + MockMvc tests
frontend/                      Angular 17 app
  src/app/core/                models (TS mirrors of DTOs) + services (HttpClient)
  src/app/features/<feature>/  routed, lazy-loaded standalone components
  nginx.conf                   serves SPA + proxies /api to backend
docker/pgadmin/servers.json    pgAdmin pre-registered server
docs/                          learning guide for the user (keep updated!)
```

## Conventions
**Backend**
- Package by **feature** (`summary/`, `task/`, ...), layered inside: `Controller → Service → Repository`.
- Controllers are thin; business logic in `@Service`; DB access only via Spring Data repositories.
- **Never return JPA entities from controllers** — map to `record` DTOs in `<feature>/dto/`.
- Constructor injection with `final` fields. No field `@Autowired`. No Lombok (explicit code while learning).
- Schema changes **only** via a new Flyway file `V<n>__description.sql`; never edit an applied migration.
  `ddl-auto: validate` — Hibernate must match the schema.
- Enums stored as `@Enumerated(EnumType.STRING)` + DB `CHECK` constraint.
- Read methods `@Transactional(readOnly = true)`; use JPQL `select new ...Record(...)` for aggregates.
- Inject `java.time.Clock` instead of calling `now()` directly (testability).
- Errors → `ProblemDetail` via `GlobalExceptionHandler`.
- API base path `/api/v1/...`. Document endpoints with `@Operation`/`@Tag`.
- Every new service gets a Mockito unit test; every new controller a `@WebMvcTest`.

**Frontend**
- Standalone components only (no NgModules). `inject()` instead of constructor DI.
- State in **signals** (`signal`, `computed`); HTTP stays Observable; unsubscribe with `takeUntilDestroyed`.
- New control flow `@if / @for (track ...)`.
- UI = Angular Material components. Layout/spacing = Bootstrap classes (`row`, `col-*`, `d-flex`, `gap-*`, `mb-*`).
  **Do not** import full `bootstrap.css` or Bootstrap JS/components.
- API URLs are relative (`environment.apiBaseUrl = '/api/v1'`) → no CORS needed.
- TS interfaces in `core/models` must mirror backend DTOs.

**Docs** — after each feature: update [MEMORY.md](MEMORY.md) (status/log/next), add or extend a lesson in [docs/](docs/).

## Gotchas
- If you change entity fields, add a migration, or the app fails at startup with a schema validation error.
- Seed data dates are relative to `CURRENT_DATE` at first DB init; `docker compose down -v` to re-seed.
- pgAdmin asks for the DB password the first time (`walkthrough`), with a "save password" option.
- Frontend `package-lock.json` was generated in `node:20-alpine` (Linux); regenerate the same way if deps change:
  `docker run --rm -v "${PWD}/frontend:/app" -w /app node:20-alpine npm install --package-lock-only`
