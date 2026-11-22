# 00 — Learning roadmap (Angular/Node dev → Java backend)

Read the lessons in order. Each one points at real files in this repo.

| # | Lesson | Level | Status |
|---|--------|-------|--------|
| 01 | [Java 21 for TypeScript developers](01-java21-for-ts-devs.md) | Beginner | ✅ |
| 02 | [Spring Boot architecture & request flow](02-spring-boot-architecture.md) | Beginner | ✅ |
| 03 | [Database: JPA, Hibernate, Flyway](03-database-jpa-flyway.md) | Beginner → Intermediate | ✅ |
| 04 | [Docker & running the stack](04-docker-and-devops.md) | Beginner | ✅ |
| 05 | [Feature: PhonePe CSV import (transactions, concurrency, batching)](05-csv-import-feature.md) | Intermediate → Advanced | ✅ |
| 06 | [Java 8 → 21 features used in this project](06-java-8-to-21-features.md) | Reference | ✅ |
| 07 | [Spring Boot concepts used in this project](07-spring-boot-concepts.md) | Reference | ✅ |
| 08 | Integration tests with Testcontainers | Intermediate | ⏳ |
| 09 | CRUD API with validation (`@Valid`) for tasks | Intermediate | ⏳ |
| 10 | Spring Security + JWT | Advanced | ⏳ |
| 11 | Async jobs, caching, observability | Advanced | ⏳ |

## How to study this repo
1. Run it: `docker compose up -d --build`, open http://localhost:4200.
2. Follow one request end to end:
   `dashboard.component.ts` → `summary.service.ts` → nginx → `SummaryController` → `SummaryService`
   → `TaskRepository`/`ProjectRepository` → Postgres.
3. Do the same for an upload: `statement-upload.component.ts` → `TransactionImportController`
   → `TransactionImportService` → `PhonePeStatementParser` → `TransactionImportWriter` (lesson 05).
4. Open http://localhost:8080/swagger-ui.html and call the endpoints yourself.
5. Open pgAdmin (http://localhost:5050) and run `SELECT * FROM transaction_imports;`.
6. Keep 06 and 07 open as references while reading code.
