package com.walkthrough.api.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One transaction from an uploaded bank / UPI statement. Table {@code bank_transactions}.
 *
 * <p>Named {@code BankTransaction} (not {@code Transaction}) to avoid confusion with
 * <i>database</i> transactions ({@code @Transactional}), which this feature also uses heavily.
 *
 * <p><b>Read-mostly entity</b>: rows are INSERTED by {@code TransactionImportWriter} with plain JDBC
 * batches (much faster for bulk loads); JPA is used to READ them with paging/sorting/filtering.
 * That's why there are only getters here - no setters, nothing in the app changes a transaction.
 */
@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    /** LocalDateTime = date + time WITHOUT zone. The statement gives local (IST) times, so we keep them as-is. */
    @Column(name = "transacted_at", nullable = false)
    private LocalDateTime transactedAt;

    @Column(nullable = false)
    private String details;

    @Column(length = 64)
    private String utr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(length = 32)
    private String instrument;

    /**
     * Money is ALWAYS {@link BigDecimal}, never double: 0.1 + 0.2 != 0.3 in floating point
     * (same problem as JS numbers). BigDecimal does exact decimal arithmetic.
     */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Plain FK value; we don't need to navigate to the import entity from here. */
    @Column(name = "import_id")
    private Long importId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected BankTransaction() {
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getTransactedAt() {
        return transactedAt;
    }

    public String getDetails() {
        return details;
    }

    public String getUtr() {
        return utr;
    }

    public TransactionType getType() {
        return type;
    }

    public String getInstrument() {
        return instrument;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getImportId() {
        return importId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
