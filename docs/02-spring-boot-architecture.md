# 02 — Spring Boot architecture & request flow

## Spring in one paragraph
Spring is a **dependency injection container** (exactly like Angular's injector) plus a big set of
integrations (web, data, security ...). Spring **Boot** adds *auto-configuration*: it looks at the
libraries in `pom.xml` and wires sensible defaults (sees Postgres driver + JPA → creates a connection
pool and Hibernate). If you know **NestJS**, you already know 80% of the ideas.

| Angular / NestJS | Spring |
|---|---|
| `@Injectable()` service | `@Service` |
| `@Controller('summary')` + `@Get()` (Nest) | `@RestController` + `@RequestMapping("/api/v1/summary")` + `@GetMapping` |
| providers / `useFactory` | `@Configuration` class with `@Bean` methods |
| `constructor(private s: Service)` | constructor injection (same!) |
| environment.ts / ConfigModule | `application.yml` + env vars |
| HttpInterceptor / exception filter | `@RestControllerAdvice` |
| TypeORM repository | Spring Data `JpaRepository` |
| class-validator DTO | Bean Validation (`@NotBlank`, `@Valid`) — coming in lesson 05 |

## Layered architecture (per feature)
```
HTTP  ─►  Controller  ─►  Service  ─►  Repository  ─►  PostgreSQL
          (web/JSON)      (business     (data access,
           thin!          rules, tx)     queries)
                              │
                              └─► returns DTO (record)  ─► JSON
```
- **Controller** — maps HTTP to a method call. No logic. `summary/SummaryController.java`
- **Service** — business rules, transactions, combining repositories. `summary/SummaryService.java`
- **Repository** — interface; Spring generates the implementation. `task/TaskRepository.java`
- **Entity** — a row in a table (`task/Task.java`). **DTO** — the JSON contract (`summary/dto/*`).

### Why package by feature?
`summary/`, `task/`, `project/` each hold their own controller/service/repo, like Angular feature
folders. When a feature grows you find everything in one place, and can later split it into a module.

## Following GET /api/v1/summary end to end
1. Angular `DashboardComponent.load()` calls `SummaryService.getSummary()` → `HttpClient.get('/api/v1/summary')`.
2. Browser hits `localhost:4200/api/v1/summary`; **nginx** (frontend container) proxies to `backend:8080`.
3. Spring's `DispatcherServlet` (its router) finds `SummaryController.getSummary()`.
4. Controller calls `SummaryService.getSummary()` — `@Transactional(readOnly=true)` opens one DB transaction.
5. Service runs 6 small aggregate queries through the repositories (count, group by, join).
6. Service builds the `SummaryResponse` record; Jackson serializes it to JSON.
7. If anything throws, `GlobalExceptionHandler` turns it into an RFC 7807 `ProblemDetail` JSON (HTTP 500),
   and Angular shows its `detail` in the error card.

## Configuration
`backend/src/main/resources/application.yml` — `${DB_URL:jdbc:postgresql://localhost:5432/walkthrough}`
means: use env var `DB_URL`, else the default. Docker Compose sets the env vars, so the same jar runs
locally and in a container (12-factor style).

## Error format (ProblemDetail)
```json
{ "type": "about:blank", "title": "Internal Server Error", "status": 500,
  "detail": "Something went wrong. Please try again later.", "instance": "/api/v1/summary" }
```
