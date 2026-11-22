package com.walkthrough.api.transaction;

import com.walkthrough.api.common.BadRequestException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The filter shared by the table, the KPIs and the counterparty chips - so they always describe
 * the SAME set of transactions.
 *
 * <p>Spring MVC binds query parameters straight into this record through its constructor
 * ({@code ?type=DEBIT&q=udemy&from=2022-04-01&to=2023-03-31&minAmount=500&maxAmount=5000}).
 * Every component is optional: a missing parameter is {@code null} = "no condition".
 * In Express you would read {@code req.query} and parse each value by hand.
 *
 * @param from inclusive start date (yyyy-MM-dd)
 * @param to   inclusive end date (yyyy-MM-dd) - the whole day is included
 */
public record TransactionFilter(
        TransactionType type,
        String q,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {

    /** No filter at all (used by callers that want all-time numbers). */
    public static TransactionFilter none() {
        return new TransactionFilter(null, null, null, null, null, null);
    }

    /** Rejects contradictory filters with a 400 instead of silently returning nothing. */
    public void validate() {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' (%s) must not be after 'to' (%s)".formatted(from, to));
        }
        if (isNegative(minAmount) || isNegative(maxAmount)) {
            throw new BadRequestException("Amounts must not be negative");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("'minAmount' must not be greater than 'maxAmount'");
        }
    }

    /** Combines the individual specifications; null pieces are ignored by {@code allOf}. */
    public Specification<BankTransaction> toSpecification() {
        return Specification.allOf(
                TransactionSpecifications.hasType(type),
                TransactionSpecifications.matchesText(q),
                TransactionSpecifications.transactedBetween(from, to),
                TransactionSpecifications.amountBetween(minAmount, maxAmount));
    }

    private static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }
}
