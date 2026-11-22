package com.walkthrough.api.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** KPIs for the transactions matching the current filter (all transactions when no filter is given). */
@Schema(description = "Totals for the filtered transactions")
public record TransactionStatsResponse(
        long totalCount,
        long debitCount,
        long creditCount,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        @Schema(description = "totalCredit - totalDebit") BigDecimal netAmount,
        BigDecimal averageDebit,
        BigDecimal averageCredit,
        BigDecimal largestDebit,
        BigDecimal largestCredit,
        LocalDateTime firstTransactionAt,
        LocalDateTime lastTransactionAt
) {
}
