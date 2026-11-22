# 06 — Java 8 → 21 features used in this project

Every feature below is **actually used in this repo**: each section says where to look.
JS/TS comparisons are there to help you map what you already know.

| Java | Feature | Where in this repo |
|---|---|---|
| 7 | try-with-resources | `TransactionImportService.parse` |
| 8 | Lambdas, method references | everywhere: `.map(BankTransactionResponse::from)` |
| 8 | Functional interfaces (`Supplier`, `Function`) | `ImportConcurrencyLimiter.withPermit`, `PageResponse.from` |
| 8 | Streams API + Collectors | `SummaryService`, `TransactionQueryService.stats`, `TransactionImportWriter` |
| 8 | `Optional` | `TransactionType.fromStatementValue`, `TransactionImportService.getImport` |
| 8 | `java.time` | `LocalDate/LocalTime/LocalDateTime/Instant/Duration/Clock` |
| 8 | `Map.putIfAbsent` | duplicate detection in `PhonePeStatementParser` |
| 9 | `List.of` / `Set.of` immutable collections | `EXPECTED_HEADER`, `SORTABLE` |
| 10 | `var` local type inference | `PhonePeStatementParser.parse` |
| 11 | `String.strip()`, `isBlank()` | `PhonePeStatementParser` |
| 14 | Switch expressions (`->`, multiple labels) | `TaskStatus.displayName`, `TransactionType.fromStatementValue` |
| 15 | Text blocks `"""` | JPQL/SQL queries, test CSV fixtures |
| 15 | `String.formatted(...)` | error messages |
| 16 | Records (+ compact constructors) | all DTOs, `StatementRow`, `ImportProperties` |
| 16 | `Stream.toList()` | everywhere instead of `collect(Collectors.toList())` |
| 17 | Sealed interfaces | `ParsedRow` |
| 21 | Pattern matching for `switch` + record patterns | `StatementParseResult.from` |
| 21 | Sequenced collections (`getFirst`, `reversed`) | `SummaryService`, parser test |
| 21 | Virtual threads | `application.yml`, `ImportConcurrencyLimiter` + its test |
| 21 | `Math.clamp` | `TransactionQueryService.topCounterparties` |
| 8 | `Collectors.groupingBy` + `summingLong` + `TreeMap` | `TransactionQueryService.financialYears` |

---

## Java 7 — try-with-resources
Anything that implements `AutoCloseable` declared in `try (...)` is closed automatically, even on exceptions.
```java
// TransactionImportService.parse
try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
    return StatementParseResult.from(parser.parse(reader, properties.maxRows()));
} catch (IOException ex) {
    throw new InvalidUploadException("Could not read the uploaded file: " + ex.getMessage());
}
```
JS analogy: a `try { ... } finally { stream.close() }` that you can't forget to write.

## Java 8 — lambdas & method references
```java
rows.stream().map(row -> row.transactionId())      // lambda      (x => x.transactionId)
rows.stream().map(StatementRow::transactionId)     // method ref  (same thing, shorter)
```
A lambda can be passed wherever a **functional interface** (an interface with one abstract method) is expected.

## Java 8 — functional interfaces: write your own higher-order function
```java
// ImportConcurrencyLimiter
public <T> T withPermit(Supplier<T> work) {   // Supplier<T> = () => T
    ...acquire...
    try { return work.get(); }
    finally { permits.release(); }
}

// caller (TransactionImportService)
return limiter.withPermit(() -> runImport(file, fileName));
```
Common ones: `Supplier<T>` `() => T`, `Function<A,B>` `(a) => b`, `Consumer<T>` `(t) => void`,
`Predicate<T>` `(t) => boolean`.

## Java 8 — Streams & Collectors
Streams are lazy pipelines over collections (like JS array methods, but nothing runs until a terminal op).
```java
// TransactionQueryService.stats - List -> Map
Map<TransactionType, TransactionTypeTotal> byType = repository.totalsByType().stream()
        .collect(Collectors.toMap(TransactionTypeTotal::type, Function.identity()));

// TransactionImportWriter - flatten int[][] and sum the positives
return Arrays.stream(counts).flatMapToInt(Arrays::stream).filter(c -> c > 0).sum();

// TransactionImportService - first 20, mapped to DTOs
result.invalidRows().stream().limit(20).map(row -> new InvalidRowResponse(...)).toList();
```
| JS | Java |
|---|---|
| `arr.map(f)` | `list.stream().map(f).toList()` |
| `arr.filter(p)` | `.filter(p)` |
| `arr.slice(0, 20)` | `.limit(20)` |
| `arr.flat()` | `.flatMap(...)` |
| `arr.reduce((a,b)=>a+b, 0)` | `.mapToInt(...).sum()` / `.reduce(0, Integer::sum)` |
| `Object.fromEntries(...)` | `.collect(Collectors.toMap(k, v))` |
| `arr.join(', ')` | `.collect(Collectors.joining(", "))` |

## Java 8 — Optional
A container that is either empty or holds a value; it makes "may be missing" part of the type.
```java
// TransactionType - returning Optional instead of null
public static Optional<TransactionType> fromStatementValue(String value) { ... }

// PhonePeStatementParser - react only when empty
TransactionType type = TransactionType.fromStatementValue(data.get(5))
        .orElseGet(() -> { errors.add("Transaction Type must be Debit or Credit ..."); return null; });

// TransactionImportService.getImport - map or throw (Spring Data returns Optional from findById)
return importRepository.findById(importId)
        .map(ImportSummaryResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("Import", importId));
```
Rule of thumb: use `Optional` as a **return type**, not for fields or parameters.

## Java 8 — java.time
| Type | Meaning | Used for |
|---|---|---|
| `LocalDate` | date, no time/zone | task due dates, statement date |
| `LocalTime` | time of day | statement time |
| `LocalDateTime` | date + time, no zone | `bank_transactions.transacted_at` (statement has no zone) |
| `Instant` | exact moment in UTC | `started_at`, `generatedAt` |
| `Duration` | amount of time | `acquire-timeout: 5s`, `durationMs` |
| `Clock` | injectable "now" | tests freeze time with `Clock.fixed(...)` |
```java
LocalDateTime.of(date, time);                       // combine
LocalTime.parse("9:05", DateTimeFormatter.ofPattern("H:mm[:ss]"));   // [..] = optional part
Duration.between(started, finished).toMillis();
```
Never use the old `java.util.Date` / `Calendar`.

## Java 9 — immutable collection factories
```java
public static final List<String> EXPECTED_HEADER = List.of("Date", "Time", ...);
private static final Set<String> SORTABLE = Set.of("transactedAt", "amount", "details", "type");
```
`add()` on these throws — like `Object.freeze`. `List.copyOf(x)` makes an immutable copy (used in records).

## Java 10 — `var`
```java
var lines = new BufferedReader(reader);   // PhonePeStatementParser: inferred, still statically typed (like TS `const`)
```
Use it when the type is obvious from the right-hand side.

## Java 11 — String helpers
`strip()` (Unicode-aware `trim()`, removes PhonePe's leading TAB), `isBlank()`, `repeat()`, `lines()`.

## Java 14 — switch expressions
```java
// TransactionType - a switch that RETURNS a value; several labels per case; no fall-through
return switch (value.strip().toLowerCase(Locale.ROOT)) {
    case "debit", "dr" -> Optional.of(DEBIT);
    case "credit", "cr" -> Optional.of(CREDIT);
    default -> Optional.empty();
};
```
Over an enum (`TaskStatus.displayName`) it must be **exhaustive** — a new constant without a case won't compile.

## Java 15 — text blocks & `formatted`
```java
private static final String INSERT_TRANSACTION = """
        INSERT INTO bank_transactions (transaction_id, transacted_at, ...)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (transaction_id) DO NOTHING
        """;

"Duplicate Transaction ID in this file (first seen on line %d)".formatted(firstLine);
```
Text blocks are also used for CSV fixtures in `PhonePeStatementParserTest`. No interpolation (unlike JS template
literals) — use `formatted`.

## Java 16 — records
Immutable data classes: constructor, accessors, `equals/hashCode/toString` are generated.
```java
public record CountItem(String key, String label, long count) {}
item.key();    // accessor - no "get" prefix
```
**Compact constructors** validate or normalise before fields are set:
```java
public record StatementRow(String transactionId, LocalDateTime transactedAt, ...) {
    public StatementRow {
        Objects.requireNonNull(transactionId, "transactionId");
    }
}

public record ImportProperties(int maxConcurrentImports, Duration acquireTimeout, ...) {
    public ImportProperties {
        if (maxConcurrentImports < 1) throw new IllegalArgumentException("...");  // fails at startup
    }
}
```
Records can be generic (`PageResponse<T>`) and even declared inside a class (`InvalidCsvExporter.ErrorRow`).
Records are **not** JPA entities (entities need mutability and a no-args constructor).

## Java 16 — `Stream.toList()`
`stream.toList()` returns an unmodifiable list — shorter than `collect(Collectors.toList())`.

## Java 17 — sealed interfaces
```java
public sealed interface ParsedRow permits ParsedRow.Valid, ParsedRow.Invalid {
    int lineNumber();
    record Valid(int lineNumber, StatementRow row) implements ParsedRow {}
    record Invalid(int lineNumber, List<String> cells, String reason) implements ParsedRow {}
}
```
TypeScript: `type ParsedRow = Valid | Invalid` (a discriminated union). The compiler knows every subtype.

## Java 21 — pattern matching for switch + record patterns
```java
// StatementParseResult.from
switch (parsed) {
    case ParsedRow.Valid(int line, StatementRow row) -> valid.add(row);   // type check + destructure
    case ParsedRow.Invalid rejected -> invalid.add(rejected);             // type check + bind
}   // no default: ParsedRow is sealed, so the compiler knows the switch is exhaustive
```
Before Java 21 this needed `if (parsed instanceof ParsedRow.Valid v) { ... } else if ...` chains.

## Java 21 — sequenced collections
```java
.toList().reversed();                       // SummaryService.priorityBreakdown
result.validRows().getFirst();              // PhonePeStatementParserTest
```
`List`, `Deque`, `LinkedHashMap` ... now share `getFirst/getLast/reversed/addFirst`.

## Java 21 — virtual threads
```yaml
spring.threads.virtual.enabled: true        # application.yml: every HTTP request runs on a virtual thread
```
```java
Thread busyImport = Thread.ofVirtual().start(() -> limiter.withPermit(...));   // ImportConcurrencyLimiterTest
```
A virtual thread is cheap (thousands are fine). When it blocks — waiting for the semaphore, the DB, or the
network — the JVM parks it and frees the carrier OS thread. You write simple blocking code and get
Node-like scalability. Waiting uploads in the bulkhead therefore cost almost nothing.

## Java 21 — `Math.clamp`
```java
int safeLimit = Math.clamp(limit, 1, MAX_COUNTERPARTIES);   // was: Math.max(1, Math.min(limit, MAX))
```

## Java 8 — grouping in memory (`groupingBy`)
```java
// TransactionQueryService.financialYears: months -> financial years (Apr-Mar), summed, sorted
TreeMap<Integer, Long> countByStartYear = repository.countByMonth().stream()
        .collect(Collectors.groupingBy(
                m -> FinancialYearResponse.startYearOf(m.year(), m.month()),   // key
                TreeMap::new,                                                   // sorted map
                Collectors.summingLong(MonthCount::count)));                    // value
countByStartYear.descendingMap();   // newest first
```
JS: `Object.groupBy(months, fyOf)` followed by summing each group.

## Concurrency utilities (since Java 5, still essential)
| Class | Used for |
|---|---|
| `Semaphore` | bulkhead: limit concurrent imports (`ImportConcurrencyLimiter`) |
| `CountDownLatch` | coordinate threads in a test (`ImportConcurrencyLimiterTest`) |
| `Thread.currentThread().interrupt()` | restore the interrupt flag after catching `InterruptedException` |

## Money: `BigDecimal`
```java
BigDecimal amount = new BigDecimal("560.00");
totalCredit.subtract(totalDebit);           // methods, not operators
amount.signum() <= 0;                       // compare sign
assertThat(x).isEqualByComparingTo("560");  // 560.00 equals 560 by value (equals() also compares scale!)
```
Never `double` for money — same `0.1 + 0.2` problem as JavaScript numbers.
