package com.walkthrough.api.transaction;

import java.math.BigDecimal;

/** Query projection: count, sum and largest amount for one {@link TransactionType}. */
public record TransactionTypeTotal(TransactionType type, Long count, BigDecimal total, BigDecimal largest) {
}
