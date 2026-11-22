package com.walkthrough.api.transaction;

import java.util.Locale;
import java.util.Optional;

/** Money out (DEBIT) or money in (CREDIT). */
public enum TransactionType {
    DEBIT,
    CREDIT;

    /**
     * Parses the statement value ("Debit" / "Credit", any case, surrounding spaces ignored).
     *
     * <p>Returns {@link Optional} instead of {@code null}: the type says "there may be no value",
     * and the caller must decide what to do ({@code orElse}, {@code orElseThrow}, {@code isPresent} ...).
     * Think of it as a typed {@code T | undefined} that you cannot forget to check.
     */
    public static Optional<TransactionType> fromStatementValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "debit", "dr" -> Optional.of(DEBIT);    // one case, several labels
            case "credit", "cr" -> Optional.of(CREDIT);
            default -> Optional.empty();
        };
    }
}
