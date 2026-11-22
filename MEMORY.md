# MEMORY.md — Project state & hand-over log

> Short, factual, always current. Update at the end of every task (see AGENT.md).

## Current status (2026-11-22)
**Milestone 1 — Foundation + Summary dashboard: DONE**
- Full stack runs with `docker compose up -d --build` (postgres, pgadmin, backend, frontend).
- `GET /api/v1/summary` returns live aggregates from Postgres (projects, tasks, done, overdue,
  completion %, by status, by priority, per-project progress).
- Angular 17 dashboard at http://localhost:4200 shows it (KPI cards, breakdown bars, project table).
- Backend tests: 4 passing (`SummaryServiceTest`, `SummaryControllerTest`).

## Key decisions (and why)
| Decision | Reason |
|---|---|
| Spring Boot 3.5.x (not 4.x) | Most tutorials/StackOverflow match 3.x; upgrade to 4 is a later lesson |
| Flyway + `ddl-auto: validate` | Schema is versioned SQL; Hibernate never mutates DB |
| Records for DTOs/projections, classes for entities | JPA needs mutable classes with no-arg ctor |
| No Lombok | Beginner sees all code explicitly |
| nginx proxies `/api` | Same origin → no CORS; same pattern in dev via `proxy.conf.json` |
| Bootstrap grid+utilities CSS only | Avoids Bootstrap Reboot conflicting with Material |
| Virtual threads enabled | Java 21 feature; Node-like scalability with blocking code |
| Inject `Clock` | Deterministic tests for date logic (overdue) |

## Progress log
- 2026-11-22 — Project scaffolded: backend (entities Project/Task, Flyway V1 schema + V2 seed,
  SummaryService/Controller, ProblemDetail errors, Swagger), frontend (Angular 17 + Material + Bootstrap,
  dashboard), Docker Compose, docs/01–04, CLAUDE.md, AGENT.md.
- 2026-11-22 — `git init`, `.gitattributes` (LF endings), pushed to public GitHub repo
  https://github.com/saroj8455/Java21-Walkthrough (branch `main`).

## Next steps (candidates — user decides)
1. **Tasks CRUD API** (`/api/v1/tasks`): POST/PUT/DELETE, `@Valid` request DTOs, 404 handling,
   pagination with `Pageable` → Angular task list (mat-table + paginator + sort) and create/edit dialog.
2. Projects CRUD + task filtering by project.
3. Integration tests with **Testcontainers** (real Postgres in tests).
4. Spring Security + JWT login; Angular auth interceptor + route guard.
5. Frontend unit tests (Jest/Karma) for service + dashboard.
6. Advanced: caching, MapStruct, Spring profiles, CI pipeline, Spring Boot 4 upgrade.

## Open questions
- None. Waiting for the user to pick the next feature.
