# 00 — Learning roadmap (Angular/Node dev → Java backend)

Read the lessons in order. Each one points at real files in this repo.

| # | Lesson | Level | Status |
|---|--------|-------|--------|
| 01 | [Java 21 for TypeScript developers](01-java21-for-ts-devs.md) | Beginner | ✅ |
| 02 | [Spring Boot architecture & request flow](02-spring-boot-architecture.md) | Beginner | ✅ |
| 03 | [Database: JPA, Hibernate, Flyway](03-database-jpa-flyway.md) | Beginner → Intermediate | ✅ |
| 04 | [Docker & running the stack](04-docker-and-devops.md) | Beginner | ✅ |
| 05 | CRUD API, validation, pagination | Intermediate | ⏳ next |
| 06 | Testing: unit, slice, Testcontainers | Intermediate | ⏳ |
| 07 | Spring Security + JWT | Advanced | ⏳ |
| 08 | Performance: N+1, caching, virtual threads | Advanced | ⏳ |

## How to study this repo
1. Run it: `docker compose up -d --build`, open http://localhost:4200.
2. Follow one request end to end:
   `dashboard.component.ts` → `summary.service.ts` → nginx → `SummaryController` → `SummaryService`
   → `TaskRepository`/`ProjectRepository` → Postgres.
3. Open http://localhost:8080/swagger-ui.html and call the endpoint yourself.
4. Open pgAdmin (http://localhost:5050) and run `SELECT * FROM tasks;`.
5. Change something small (e.g. add a seed task in a new `V3__more_tasks.sql`), rebuild, watch the dashboard change.
