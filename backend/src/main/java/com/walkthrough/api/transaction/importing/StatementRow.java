package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A validated, typed statement line - ready to be inserted.
 *
 * <p>The <b>compact constructor</b> (no parameter list) runs before the fields are assigned,
 * so a {@code StatementRow} can never exist with a missing required value. Records can validate;
 * they are not just dumb bags of data.
 */
public record StatementRow(
        String transactionId,
        LocalDateTime transactedAt,
        String details,
        String utr,
        TransactionType type,
        String instrument,
        BigDecimal amount
) {
    public StatementRow {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(transactedAt, "transactedAt");
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
    }
}
