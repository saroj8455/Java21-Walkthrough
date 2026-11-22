package com.walkthrough.api.transaction.dto;

import com.walkthrough.api.transaction.BankTransaction;
import com.walkthrough.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** JSON view of one transaction. {@code instrument} is the masked account/card shown in the statement. */
public record BankTransactionResponse(
        long id,
        String transactionId,
        LocalDateTime transactedAt,
        String details,
        String utr,
        TransactionType type,
        String instrument,
        BigDecimal amount
) {
    public static BankTransactionResponse from(BankTransaction t) {
        return new BankTransactionResponse(t.getId(), t.getTransactionId(), t.getTransactedAt(), t.getDetails(),
                t.getUtr(), t.getType(), t.getInstrument(), t.getAmount());
    }
}
