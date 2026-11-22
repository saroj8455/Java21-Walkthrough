package com.walkthrough.api.transaction.dto;

import com.walkthrough.api.transaction.CounterpartyTotal;

import java.math.BigDecimal;
import java.util.List;

/**
 * A frequent counterparty within the current filter.
 *
 * @param details full statement text, e.g. "Paid to SHOP" - send it back as {@code q} to filter by it
 * @param name    display name without the "Paid to" / "Received from" prefix
 */
public record CounterpartyResponse(String details, String name, long count, BigDecimal total) {

    private static final List<String> PREFIXES = List.of("Paid to ", "Received from ", "Paid - ", "Bill paid - ");

    public static CounterpartyResponse from(CounterpartyTotal total) {
        return new CounterpartyResponse(total.details(), displayName(total.details()), total.count(), total.total());
    }

    static String displayName(String details) {
        return PREFIXES.stream()
                .filter(details::startsWith)
                .findFirst()                                            // Optional<String>
                .map(prefix -> details.substring(prefix.length()).strip())
                .filter(name -> !name.isEmpty())
                .orElse(details);
    }
}
