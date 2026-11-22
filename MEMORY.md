# MEMORY.md — Project state & hand-over log

> Short, factual, always current. Update at the end of every task (see AGENT.md).

## Current status (2026-11-22)
Branch: **`develop`** (user works directly on it; user reviews and commits themselves — **do not commit**).

**Milestone 1 — Foundation + Summary dashboard: DONE** (committed on `main`, `1520ede`)

**Milestone 2 — PhonePe statement CSV import: DONE, UNCOMMITTED on `develop` (awaiting user review)**
- Flyway `V3`: `transaction_imports`, `bank_transactions` (UNIQUE transaction_id), `transaction_import_errors`.
- `POST /api/v1/transactions/imports` (multipart `file`) → valid rows saved in ONE transaction, duplicates skipped
  via `ON CONFLICT DO NOTHING`, invalid rows stored + downloadable as `invalid_csv_statements.csv`
  (`GET /imports/{id}/invalid-csv`); re-uploading that file works.
- Bulkhead: max 3 concurrent imports, 5 s wait, then 429 + Retry-After. Hikari pool 10.
- Read API: `GET /api/v1/transactions` (paged, sort whitelist, `type`, `q`), `/stats`, `/imports` history.
- Frontend `/transactions`: drag & drop upload w/ progress, result + invalid preview + download, stats cards,
  server-side table (sort/page/filter/search), import history.
- Tests: 25 backend tests green (CsvLine, parser, limiter, import service, import controller, summary).
- Verified on real stack: real file → 691 imported / 4 invalid (PhonePe footer lines 697–700) / 0 dup;
  re-upload → 0 / 691 dup; 30 parallel uploads → all 201, ~1 s, dashboard avg 24 ms, no deadlocks;
  limit=1/0s → 1×201 + 19×429; trigger-simulated DB failure → 500, 0 rows saved, FAILED record kept.
- DB holds the user's real data (user later uploaded a 2nd statement: 1,462 transactions, FY 2022-23 + FY 2023-24).

**Milestone 2b — Filter-aware KPIs + filter chips: DONE, UNCOMMITTED on `develop`**
- Backend: `TransactionFilter` record (type, q, from, to, minAmount, maxAmount) bound from query params on ALL
  read endpoints; `/stats?{filter}` (count, debit/credit sums, avg, largest, net, span), `/counterparties?{filter}`,
  `/periods` (financial years). Aggregates via custom repo fragment `TransactionAnalyticsRepositoryImpl` (Criteria API).
- Frontend: page-scoped `TransactionFilterStore` (signals) shared by `transaction-filters` (search, type, date range,
  FY chips, amount chips, removable active chips), `transaction-insights` (6 KPI cards + top counterparty chips),
  `transaction-table`. Static all-time cards removed from the page (KPIs show all-time when unfiltered).
  `InrPipe` + `core/utils/format.ts` (en-IN grouping, timezone-safe ISO date helpers).
- Tests: 37 backend green. Verified vs SQL: q=XXXXXX8540&type=DEBIT → 16 / ₹1,18,800 (API = SQL);
  amount buckets partition the full set exactly.

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
| Import synchronous + bulkhead (not async job) | ~200 ms per 700-row file; async job documented as next step |
| JDBC batch for inserts, JPA for reads | Hibernate can't batch IDENTITY inserts; ON CONFLICT is Postgres SQL |
| Duplicate in DB = skipped (not invalid); duplicate within file = invalid | Re-upload must be safe and quiet |
| Hand-written `CsvLine` instead of a CSV library | Single-line records; no dependency; teaching value |
| Footer disclaimer lines reported as invalid | Transparent: every non-blank line after the header is accounted for |
| Real statement folder gitignored; synthetic test data | Repo is public; data is personal |

## Progress log
- 2026-11-22 — M1 scaffolded and pushed (`main`, public repo).
- 2026-11-22 — User created `develop`. M2 CSV import implemented end to end + docs 05, 06 (Java 8→21), 07 (Spring).
  `.gitignore` now excludes `PhonePe transactions/` and `invalid_csv_statements*.csv`.

## Next steps (candidates — user decides)
1. User reviews + commits M2 + M2b on `develop` (suggested: "feat: PhonePe CSV import" and
   "feat: filter-aware transaction KPIs and filter chips").
1b. Ideas from the filter work: monthly spend chart for the current filter, "download filtered CSV", category tags.
2. Testcontainers integration tests (real Postgres) for the import + rollback path (lesson 08).
3. Monthly spend chart / category tagging of transactions (e.g. by merchant keyword).
4. Tasks CRUD with `@Valid` (lesson 09).
5. Spring Security + JWT; per-user data (imports currently global).
6. Async import jobs (202 + polling) / distributed rate limit if scaled to several API replicas.

## Open questions
- None. Waiting for the user's review of M2.
