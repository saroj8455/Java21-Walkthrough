package com.walkthrough.api.transaction;

import java.math.BigDecimal;

/** Query projection: transactions grouped by their "details" text (e.g. "Paid to SHOP"). */
public record CounterpartyTotal(String details, Long count, BigDecimal total) {
}
