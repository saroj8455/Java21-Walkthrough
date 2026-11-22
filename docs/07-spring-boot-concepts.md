# 07 — Spring Boot concepts used in this project

Each concept: what it is, the Angular/Node equivalent, and the file to read.

| Concept | Angular / Node equivalent | Where |
|---|---|---|
| Auto-configuration, `@SpringBootApplication` | Nest `AppModule` + sensible defaults | `WalkthroughApiApplication` |
| DI: `@Service`, `@Component`, constructor injection | `@Injectable()`, `inject()` | every service |
| `@Configuration` + `@Bean` | providers with `useFactory` | `AppConfig` (Clock) |
| `@ConfigurationProperties` records | typed `ConfigService` / env schema | `ImportProperties` |
| `@RestController`, mappings | Express router / Nest controller | `*Controller` |
| Multipart upload `@RequestPart MultipartFile` | `multer` | `TransactionImportController.upload` |
| `ResponseEntity` (status, headers) | `res.status(201).set(...).json()` | upload (201 + Location), CSV download |
| `Pageable`, `@PageableDefault` | manual `?page&limit` parsing | `TransactionController.search` |
| Spring Data repositories | TypeORM `Repository<T>` | `*Repository` |
| Derived queries | — | `countByStatus`, `findTop20ByOrderByStartedAtDesc` |
| JPQL `@Query` + constructor projections | query builder + DTO mapping | `ProjectRepository`, `BankTransactionRepository` |
| Specifications | conditional `qb.andWhere(...)` | `TransactionSpecifications` |
| Binding query params into a record | parsing `req.query` by hand | `TransactionFilter` |
| Custom repository fragment + Criteria API | raw query builder for aggregates | `TransactionAnalyticsRepository(Impl)` |
| `JdbcTemplate` batch updates | `pg` + multi-row insert | `TransactionImportWriter` |
| `@Transactional`, rollback, proxies | `BEGIN/COMMIT/ROLLBACK` by hand | `TransactionImportWriter`, `SummaryService` |
| Dirty checking | — (ORM-specific) | `TransactionImportWriter.write` |
| Flyway migrations | knex / prisma migrate | `db/migration/V*.sql` |
| `@RestControllerAdvice` + `ProblemDetail` | error middleware | `GlobalExceptionHandler` |
| Actuator health | `/health` route | `/actuator/health` (Docker healthcheck) |
| springdoc OpenAPI | `@nestjs/swagger` | `/swagger-ui.html` |
| Test slices `@WebMvcTest`, `@MockitoBean` | supertest + jest mocks | `*ControllerTest` |

---

## Dependency injection
```java
@Service
public class TransactionImportService {
    private final PhonePeStatementParser parser;          // final: set once, never reassigned
    private final TransactionImportWriter writer;

    public TransactionImportService(PhonePeStatementParser parser, TransactionImportWriter writer, ...) {
        this.parser = parser;                              // Spring passes the beans in
        this.writer = writer;
    }
}
```
Beans are **singletons** by default (one instance per app, like `providedIn: 'root'`), so they must be
stateless or thread-safe: `ImportConcurrencyLimiter` holds a `Semaphore`, which is thread-safe by design.

## Type-safe configuration
```yaml
app:
  import:
    max-concurrent-imports: 3
    acquire-timeout: 5s
```
```java
@ConfigurationProperties(prefix = "app.import")
public record ImportProperties(@DefaultValue("3") int maxConcurrentImports,
                               @DefaultValue("5s") Duration acquireTimeout, ...) { ... }
```
Enabled by `@ConfigurationPropertiesScan`. Env vars override YAML via relaxed binding:
`APP_IMPORT_MAXCONCURRENTIMPORTS=1` (that's how the 429 load test was run).

## Controllers, multipart, ResponseEntity
```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ImportResultResponse> upload(@RequestPart("file") MultipartFile file) {
    ImportResultResponse result = importService.importStatement(file);
    return ResponseEntity.created(URI.create("/api/v1/transactions/imports/" + result.importId())).body(result);
}
```
Limits live in `application.yml` (`spring.servlet.multipart.max-file-size: 5MB`) → Spring answers 413 by itself.

File download = bytes + `Content-Disposition: attachment`:
```java
return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("invalid_csv_statements.csv").build().toString())
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv);
```

## Paging, sorting, filtering
```java
@GetMapping
public PageResponse<BankTransactionResponse> search(
        @RequestParam(required = false) TransactionType type,          // "DEBIT" -> enum automatically
        @RequestParam(name = "q", required = false) String text,
        @PageableDefault(size = 20, sort = "transactedAt", direction = Sort.Direction.DESC) Pageable pageable) { ... }
```
`?page=2&size=50&sort=amount,desc` becomes a `Pageable`. `spring.data.web.pageable.max-page-size: 100` caps it.
Sort fields are whitelisted in `TransactionQueryService` (unknown field → 400, not 500).

Optional filters with **Specifications** (null = "no condition"):
```java
Specification<BankTransaction> filter = Specification.allOf(
        TransactionSpecifications.hasType(type),        // null when type == null
        TransactionSpecifications.matchesText(text));   // null when text is blank
repository.findAll(filter, pageable);                   // SELECT ... WHERE ... ORDER BY ... LIMIT ... + COUNT
```

## One filter object for many endpoints
```java
public record TransactionFilter(TransactionType type, String q,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        BigDecimal minAmount, BigDecimal maxAmount) { ... }

@GetMapping("/stats")
public TransactionStatsResponse stats(@ParameterObject TransactionFilter filter) { ... }
```
A non-simple parameter without annotation is a *model attribute*: Spring calls the record's constructor with
matching query parameters (missing → `null`, wrong format → 400). `@ParameterObject` just documents the fields in Swagger.

## Custom repository fragments + Criteria API (aggregates with a dynamic WHERE)
Spring Data can page a `Specification`, but cannot `SUM/COUNT ... GROUP BY` it. Add your own methods:
```java
public interface TransactionAnalyticsRepository {                       // 1. fragment interface
    List<TransactionTypeTotal> totalsByType(Specification<BankTransaction> filter);
}

class TransactionAnalyticsRepositoryImpl implements TransactionAnalyticsRepository {   // 2. "Impl" suffix
    public List<TransactionTypeTotal> totalsByType(Specification<BankTransaction> filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TransactionTypeTotal> query = cb.createQuery(TransactionTypeTotal.class);
        Root<BankTransaction> root = query.from(BankTransaction.class);
        query.select(cb.construct(TransactionTypeTotal.class, root.get("type"), cb.count(root), cb.sum(amount), cb.max(amount)))
             .where(filter.toPredicate(root, query, cb))       // the SAME spec the table uses
             .groupBy(root.get("type"));
        return entityManager.createQuery(query).getResultList();
    }
}

public interface BankTransactionRepository extends JpaRepository<...>, JpaSpecificationExecutor<...>,
        TransactionAnalyticsRepository { }                    // 3. callers see one repository
```

## Data access: three tools, each for its job
| Tool | Use it for | Example |
|---|---|---|
| Derived query | simple lookups | `countByDueDateBeforeAndStatusNot(date, DONE)` |
| JPQL `@Query` + `select new Record(...)` | fixed aggregates without loading entities | `countByMonth()` |
| Criteria API in a custom fragment | aggregates with a *dynamic* filter | `totalsByType(spec)` |
| `JdbcTemplate` | bulk inserts, DB-specific SQL (`ON CONFLICT`) | `TransactionImportWriter` |

Why not JPA `saveAll` for 700 rows? With `IDENTITY` ids Hibernate must insert one row at a time to read each
generated id → 700 round-trips. A JDBC batch sends 500 rows per round-trip.

## Transactions
```java
@Transactional                       // read-write: rollback on any RuntimeException
public TransactionImport write(...) { ... }

@Transactional(readOnly = true)      // reads: consistent snapshot, Hibernate skips dirty checking
public SummaryResponse getSummary() { ... }
```
How it works: Spring creates a **proxy** around the bean.
```
caller ──► [proxy: BEGIN] ──► real write() ──► [proxy: COMMIT | ROLLBACK on exception]
```
Consequences:
1. Calling a `@Transactional` method from **the same class** skips the proxy → no transaction. Put it in another bean.
2. Only **unchecked** exceptions roll back by default (`rollbackFor` changes that).
3. Keep transactions **short**: do file parsing, HTTP calls etc. *outside* (see `TransactionImportService`).

**Dirty checking**: an entity loaded or saved inside a transaction is *managed*. Change its fields and
Hibernate writes an `UPDATE` at commit — no `save()` call needed (`anImport.complete(...)`).

## Error handling with ProblemDetail
```java
@ExceptionHandler(TooManyImportsException.class)
public ResponseEntity<ProblemDetail> handleTooManyImports(TooManyImportsException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    problem.setProperty("retryAfterSeconds", seconds);
    return ResponseEntity.status(429).header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds)).body(problem);
}
```
Domain code throws meaningful exceptions (`BadRequestException`, `ResourceNotFoundException`, ...); one
advice class maps them to HTTP. The Angular `toErrorMessage()` helper reads `detail` from the same JSON.

## Resilience: the bulkhead pattern
Limit how many expensive operations run concurrently so they cannot exhaust shared resources
(DB connections), and fail fast with 429 instead of queueing forever. Implemented with a plain `Semaphore`
in `ImportConcurrencyLimiter`. Production alternative: Resilience4j `@Bulkhead` / `@RateLimiter`.

## Testing pyramid used here
| Test type | Annotation | Starts | Example |
|---|---|---|---|
| Unit | `@ExtendWith(MockitoExtension.class)` | nothing | `TransactionImportServiceTest`, `PhonePeStatementParserTest` |
| Web slice | `@WebMvcTest(X.class)` + `@MockitoBean` | MVC only | `TransactionImportControllerTest` |
| Integration (next) | `@SpringBootTest` + Testcontainers | everything + real Postgres | lesson 08 |

Run: see `CLAUDE.md` → "Backend tests" (Maven in Docker, no local install needed).
