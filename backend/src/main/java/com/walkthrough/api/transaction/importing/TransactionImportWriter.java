package com.walkthrough.api.transaction.importing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Writes one parsed statement to the database - <b>all or nothing</b>.
 *
 * <h2>Why a separate bean from TransactionImportService?</h2>
 * {@code @Transactional} works through a Spring <b>proxy</b>: Spring wraps this bean, and the wrapper
 * opens/commits/rolls back the transaction around each call that comes <i>from outside</i>.
 * If the service called its own {@code @Transactional} method ({@code this.write(...)}), the call would
 * bypass the proxy and NO transaction would start ("self-invocation" pitfall). Two beans avoid that.
 *
 * <h2>Why JdbcTemplate for the inserts instead of JPA {@code saveAll}?</h2>
 * With IDENTITY ids Hibernate must INSERT rows one by one to learn each id, so it cannot batch.
 * {@link JdbcTemplate#batchUpdate} sends 500 rows per round-trip and lets PostgreSQL resolve duplicates
 * with {@code ON CONFLICT DO NOTHING}. Both share the same connection and the same transaction,
 * because Spring's transaction manager binds the connection to the current thread.
 */
@Component
public class TransactionImportWriter {

    /** Text block (Java 15): multi-line SQL without "+" concatenation. */
    private static final String INSERT_TRANSACTION = """
            INSERT INTO bank_transactions
                (transaction_id, transacted_at, details, utr, type, instrument, amount, import_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (transaction_id) DO NOTHING
            """;

    private static final String INSERT_ERROR = """
            INSERT INTO transaction_import_errors (import_id, line_number, raw_record, reason)
            VALUES (?, ?, ?, ?)
            """;

    private final TransactionImportRepository importRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ImportProperties properties;
    private final Clock clock;

    public TransactionImportWriter(TransactionImportRepository importRepository, JdbcTemplate jdbcTemplate,
                                   ImportProperties properties, Clock clock) {
        this.importRepository = importRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * One DB transaction for the whole file. Any {@code RuntimeException} thrown inside
     * (e.g. {@code DataAccessException}) makes Spring ROLL BACK: no transactions, no error rows,
     * no import record are left behind - the database looks as if the upload never happened.
     */
    @Transactional
    public TransactionImport write(String fileName, StatementParseResult result, Instant startedAt) {
        // IDENTITY id -> Hibernate INSERTs immediately, so the generated id is available right away.
        TransactionImport anImport = importRepository.save(TransactionImport.started(fileName, startedAt));
        long importId = anImport.getId();

        // Same lock order for every concurrent upload -> two overlapping uploads can't deadlock each other.
        List<StatementRow> sorted = result.validRows().stream()
                .sorted(Comparator.comparing(StatementRow::transactionId))
                .toList();

        int inserted = insertTransactions(importId, sorted);
        insertErrors(importId, result.invalidRows());

        // No save() needed: anImport is a MANAGED entity. Hibernate's "dirty checking" notices the
        // changed fields and issues an UPDATE automatically when the transaction commits.
        anImport.complete(result.totalRows(), inserted, sorted.size() - inserted,
                result.invalidRows().size(), clock.instant());
        return anImport;
    }

    /** Runs AFTER {@link #write} rolled back, in its own fresh transaction, so the failure is remembered. */
    @Transactional
    public TransactionImport recordFailure(String fileName, StatementParseResult result, Instant startedAt,
                                           String errorMessage) {
        return importRepository.save(TransactionImport.failed(fileName, startedAt, result.totalRows(),
                result.invalidRows().size(), errorMessage, clock.instant()));
    }

    /** @return how many rows were really inserted (rows skipped by ON CONFLICT report 0). */
    private int insertTransactions(long importId, List<StatementRow> rows) {
        // int[batch][row] update counts. The lambda fills the "?" placeholders for one row.
        int[][] counts = jdbcTemplate.batchUpdate(INSERT_TRANSACTION, rows, properties.batchSize(), (ps, row) -> {
            ps.setString(1, row.transactionId());
            ps.setObject(2, row.transactedAt());     // LocalDateTime -> SQL TIMESTAMP
            ps.setString(3, row.details());
            ps.setString(4, row.utr());              // null -> SQL NULL
            ps.setString(5, row.type().name());
            ps.setString(6, row.instrument());
            ps.setBigDecimal(7, row.amount());
            ps.setLong(8, importId);
        });
        return Arrays.stream(counts)
                .flatMapToInt(Arrays::stream)          // int[][] -> one IntStream
                .filter(count -> count > 0)
                .sum();
    }

    private void insertErrors(long importId, List<ParsedRow.Invalid> invalidRows) {
        jdbcTemplate.batchUpdate(INSERT_ERROR, invalidRows, properties.batchSize(), (ps, invalid) -> {
            ps.setLong(1, importId);
            ps.setInt(2, invalid.lineNumber());
            ps.setString(3, CsvLine.format(invalid.cells()));
            ps.setString(4, invalid.reason());
        });
    }
}
