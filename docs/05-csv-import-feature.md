# 05 — Feature: PhonePe statement CSV import

> Level: Intermediate → Advanced. Read [02](02-spring-boot-architecture.md) and [03](03-database-jpa-flyway.md) first.

## What it does
1. You upload a PhonePe statement CSV on **http://localhost:4200/transactions** (drag & drop).
2. Every line is validated. **Valid rows are saved**, **invalid rows are rejected with a reason**.
3. Rejected rows can be downloaded as **`invalid_csv_statements.csv`**. Fix them in Excel and upload that file
   again — the parser accepts it as-is.
4. Uploading the same statement twice is safe: duplicates are skipped, never inserted twice.
5. If the database fails half-way, **nothing** from that file is saved (one transaction), and the failed attempt
   is still visible in the import history.
6. Many people can upload at once without slowing down the rest of the API (bulkhead + batching).

## API
| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/transactions/imports` | multipart upload, part name `file` → `201` + `ImportResultResponse` |
| `GET` | `/api/v1/transactions/imports` | latest 20 imports (history) |
| `GET` | `/api/v1/transactions/imports/{id}` | one import |
| `GET` | `/api/v1/transactions/imports/{id}/invalid-csv` | download `invalid_csv_statements.csv` |
| `GET` | `/api/v1/transactions?{filter}&page=0&size=20&sort=amount,desc` | paged search |
| `GET` | `/api/v1/transactions/stats?{filter}` | KPIs of the filtered set: counts, debit/credit totals, averages, largest, net, date span |
| `GET` | `/api/v1/transactions/counterparties?{filter}&limit=8` | most frequent counterparties in the filtered set |
| `GET` | `/api/v1/transactions/periods` | financial years (Apr–Mar) that contain data, for chips |

`{filter}` is the same on every endpoint (all optional):
`type=DEBIT|CREDIT`, `q=text`, `from=yyyy-MM-dd`, `to=yyyy-MM-dd` (inclusive), `minAmount`, `maxAmount` (inclusive).

Try it without the UI:
```bash
curl -F "file=@statement.csv;type=text/csv" http://localhost:8080/api/v1/transactions/imports
```
Example response:
```json
{ "importId": 1, "fileName": "statement.csv", "status": "COMPLETED",
  "totalRows": 695, "importedRows": 691, "duplicateRows": 0, "invalidRows": 4, "durationMs": 197,
  "invalidCsvUrl": "/api/v1/transactions/imports/1/invalid-csv",
  "invalidRowsPreview": [ { "lineNumber": 697, "reason": "Not a transaction row: expected 8 columns but found 1", "rawRecord": "..." } ] }
```

| Status | When |
|---|---|
| 201 | file processed (even if some rows were invalid — that's a normal outcome) |
| 400 | empty file, not `.csv`, header row missing, > 50,000 rows, bad `sort` |
| 413 | file larger than 5 MB |
| 429 | all import slots busy — `Retry-After` header says when to retry |
| 500 | database error — everything rolled back, body contains `importId` of the FAILED record |

## The input file
```
Transaction Statement for +91XXXXXXXXXX                 <- preamble: skipped, never stored
Duration,01 Apr 2022 - 31 Mar 2023
                                                         <- blank lines ignored
Date,Time,Transaction Details,Transaction ID,UTR,Transaction Type,Credit/debit instrument,Amount
2022-04-04,<TAB>00:29,Paid to SHOP,T2204...,2094...,Debit,XXXXXX0164,560.00
...
This is an automatically generated statement ...        <- footer lines -> reported as invalid
```

### Validation rules (`PhonePeStatementParser`)
| Column | Rule |
|---|---|
| (whole row) | must have as many cells as the header (8, or 10 for a re-uploaded invalid file) |
| Date | `yyyy-MM-dd` |
| Time | `H:mm` or `H:mm:ss`; surrounding whitespace/TAB ignored |
| Transaction Details | required, ≤ 255 chars |
| Transaction ID | required, 1–64 letters/digits/`-`, **unique within the file** |
| UTR | optional, ≤ 64 |
| Transaction Type | `Debit`/`Credit` (also `DR`/`CR`), any case |
| Credit/debit instrument | optional, ≤ 32 |
| Amount | > 0, up to 12 digits + 2 decimals; `1,234.50` allowed |

All problems of a row are reported together: `Date must be yyyy-MM-dd but was '2022-13-01'; Time must be HH:mm ...`.

**Duplicates vs invalid** — a Transaction ID that appears twice *in the same file* is invalid (data problem).
A Transaction ID that *already exists in the database* is a **duplicate**: counted, skipped, not an error.

## Architecture
```
 Angular  StatementUploadComponent ──POST multipart──► nginx ──► TransactionImportController
                                                                   │
                                                                   ▼
                                                     TransactionImportService   (NOT @Transactional)
                                                       1. validate file name/size
                                                       2. ImportConcurrencyLimiter.withPermit(...)  ◄─ bulkhead (Semaphore)
                                                       3. PhonePeStatementParser.parse(reader)      ◄─ pure Java, no DB
                                                       4. TransactionImportWriter.write(...)        ◄─ @Transactional
                                                            ├─ INSERT transaction_imports (PROCESSING)
                                                            ├─ JDBC batch INSERT ... ON CONFLICT DO NOTHING
                                                            ├─ JDBC batch INSERT transaction_import_errors
                                                            └─ UPDATE import -> COMPLETED (dirty checking)
                                                          on DataAccessException:
                                                            rollback ─► writer.recordFailure(...) ─► 500
```

### Files
| Layer | File |
|---|---|
| Migration | `backend/src/main/resources/db/migration/V3__create_bank_transactions_and_imports.sql` |
| Web | `transaction/importing/TransactionImportController.java`, `transaction/TransactionController.java` |
| Orchestration | `transaction/importing/TransactionImportService.java` |
| Parsing | `CsvLine.java`, `PhonePeStatementParser.java`, `ParsedRow.java` (sealed), `StatementParseResult.java` |
| Persistence | `TransactionImportWriter.java` (JDBC batch), `TransactionImport.java` (entity), `BankTransaction.java` |
| Concurrency | `ImportConcurrencyLimiter.java`, `ImportProperties.java` |
| Export | `InvalidCsvExporter.java` |
| Read side | `TransactionQueryService.java`, `TransactionSpecifications.java`, `BankTransactionRepository.java` |
| Errors | `common/GlobalExceptionHandler.java` (+ `TooManyImportsException`, `ImportFailedException`, `InvalidUploadException`) |
| Frontend | `features/transactions/*` — page, `statement-upload`, `transaction-table`, `import-history` |

## Deep dive 1 — "all or nothing" transactions
```java
@Transactional                                   // TransactionImportWriter
public TransactionImport write(String fileName, StatementParseResult result, Instant startedAt) {
    TransactionImport anImport = importRepository.save(TransactionImport.started(fileName, startedAt));
    int inserted = insertTransactions(anImport.getId(), sorted);   // JDBC batch
    insertErrors(anImport.getId(), result.invalidRows());
    anImport.complete(...);                      // no save(): dirty checking -> UPDATE at commit
    return anImport;
}
```
- Spring wraps the bean in a **proxy**. The proxy runs `BEGIN` before the method and `COMMIT` after it,
  or `ROLLBACK` if a `RuntimeException` escapes.
- `JdbcTemplate` and JPA use **the same connection** inside this transaction (Spring binds it to the thread),
  so the JDBC inserts and the JPA entity are committed or rolled back together.
- **Self-invocation pitfall:** if `TransactionImportService` called its own `@Transactional` method, the call
  would bypass the proxy and no transaction would start. That's why the writer is a separate bean.
- After a rollback, `recordFailure()` runs in a *new* transaction so the FAILED attempt is remembered.

Verified for real: a temporary trigger made the 2nd of 2 rows fail → the API returned 500, **0 rows** were saved,
and a FAILED import record existed.

## Deep dive 2 — many users uploading at the same time
| Risk | Protection |
|---|---|
| 50 uploads grab all DB connections, dashboard stalls | **Bulkhead**: `Semaphore(3)` → max 3 imports at once; others wait ≤ 5 s, then **429 + Retry-After**. Pool = 10 → ≥ 7 connections always free for reads. |
| Slow client holds a connection while uploading | Parsing happens **before** the transaction; the service is not `@Transactional`. A connection is held only for the short write. |
| Row-by-row inserts are slow | `JdbcTemplate.batchUpdate` in batches of 500 (Hibernate cannot batch IDENTITY inserts). |
| Same file uploaded twice / at the same moment | `UNIQUE(transaction_id)` + `ON CONFLICT DO NOTHING` — the database settles races atomically. |
| Two overlapping uploads deadlock | Rows are **sorted by transaction id** before inserting → every upload takes row locks in the same order. |
| Huge files exhaust memory | 5 MB multipart limit, nginx `client_max_body_size`, `max-rows: 50000`, line-by-line reading. |
| Threads | Java 21 **virtual threads**: a request waiting on the semaphore or the DB costs almost nothing. |

Measured locally: **30 simultaneous uploads** of the real 700-line file → all `201` in ~1 s total, no errors,
no deadlocks, row count unchanged (all duplicates); `/api/v1/summary` averaged **24 ms** during the burst.
With the limit forced to 1 slot / 0 s wait: 1 × `201`, 19 × `429`.

Tuning (`application.yml` → `app.import`, or env vars):
```yaml
app:
  import:
    max-concurrent-imports: 3   # env IMPORT_MAX_CONCURRENT
    acquire-timeout: 5s
    batch-size: 500
    max-rows: 50000
```

**Limits of this design (advanced):** the semaphore is per container. With 3 API replicas you get 3 × 3 slots.
For a global limit, or for very large files, move to an **async job**: `POST` returns `202 Accepted` + job id,
a worker processes the queue, the UI polls `GET /imports/{id}`. The `transaction_imports` table already has a
`PROCESSING` status for that.

## Deep dive 3 — the invalid CSV round trip
`invalid_csv_statements.csv` = the 8 original columns + `Line Number` + `Error Reason`, UTF-8 with BOM
(so Excel shows ₹ and names correctly), CRLF line endings.
The parser only checks that the **first 8 header cells** match and that every row has **as many cells as the
header**, so the exported file can be corrected and uploaded again without deleting the extra columns.

## Deep dive 4 — filters, chips and live KPIs
**Goal:** whatever filter is active, the KPI cards, counterparty chips and the table describe *the same rows*.

**Backend — one filter, three queries.** `TransactionFilter` (a record) is bound straight from the query string,
validated (`from ≤ to`, `min ≤ max`), and turned into one `Specification`. All endpoints reuse it:
```java
public Specification<BankTransaction> toSpecification() {
    return Specification.allOf(hasType(type), matchesText(q), transactedBetween(from, to), amountBetween(minAmount, maxAmount));
}
```
- Table → `repository.findAll(spec, pageable)` (Spring Data).
- KPIs → `TransactionAnalyticsRepositoryImpl.totalsByType(spec)` + `dateRange(spec)`: Criteria API
  `SELECT type, count, sum, max ... WHERE <spec> GROUP BY type` — 2 aggregate queries, no rows loaded.
- Counterparties → `GROUP BY details ORDER BY count DESC, sum DESC LIMIT 8`.

Verified against plain SQL: `q=XXXXXX8540&type=DEBIT` → 16 rows, ₹1,18,800 spent in both API and SQL; the four
amount buckets add up to exactly the total count (no gaps/overlaps).

**Frontend — a page-scoped signal store.**
```
TransactionsPageComponent   providers: [TransactionFilterStore]   <- one store per page visit
 ├─ TransactionFiltersComponent    writes:  search, type, from/to, min/max     (chips, datepicker)
 ├─ TransactionInsightsComponent   reads:   store.query -> stats + counterparties (forkJoin)
 └─ TransactionTableComponent      reads:   store.query + own paging/sort      -> search
```
| UI element | Source |
|---|---|
| Search, Debit/Credit toggle, date-range picker | writes store signals |
| **Period chips** (FY 2023-24, FY 2022-23 + counts) | `GET /periods`; click sets `from/to` |
| **Amount chips** (Under ₹500, ₹500–5k, ₹5k–20k, ₹20k+) | static buckets in the store |
| **Active filter chips** (removable ✕, "Clear all") | `store.chips` computed from the filter |
| **KPI cards**: Transactions (+ % of total), Spent, Received, Net, Largest debit, Period | `GET /stats?{filter}` |
| **Top counterparty chips** (click = filter by it, click again = remove) | `GET /counterparties?{filter}` |

Each consumer: `toObservable(store.query)` → `debounceTime(250)` → `distinctUntilChanged` → `switchMap`
(older requests are cancelled when filters change quickly). The table resets to page 1 via an `effect` whenever
the filter changes.

## Privacy
- `PhonePe transactions/` and `invalid_csv_statements*.csv` are in `.gitignore`. **Never commit real statements**
  — the GitHub repo is public.
- The preamble (phone number) is never stored. Tests use synthetic data only.

## Exercises
1. Add a `GET /api/v1/transactions/stats/monthly?{filter}` endpoint (Criteria `groupBy` year/month) and a chart.
2. Add a "Download filtered CSV" button that exports exactly the rows matching the current filter.
3. Write an integration test with **Testcontainers** that uploads a file against a real PostgreSQL.
4. Convert the import to an async job (see "Limits of this design").
