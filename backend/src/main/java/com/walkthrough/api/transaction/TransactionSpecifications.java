package com.walkthrough.api.transaction;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reusable, composable WHERE-clause pieces for {@link BankTransaction}.
 *
 * <p>A {@link Specification} is a lambda {@code (root, query, criteriaBuilder) -> Predicate}:
 * <ul>
 *   <li>{@code root}  - the entity being queried ({@code root.get("type")} = column t.type)</li>
 *   <li>{@code cb}    - factory for conditions: equal, like, and, or ...</li>
 * </ul>
 * Returning {@code null} means "no condition" - that's how optional filters stay optional.
 * They are combined in {@link TransactionFilter#toSpecification()}.
 *
 * <p>{@code final} class + private constructor = a utility class of static methods (no instances).
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<BankTransaction> hasType(TransactionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    /** Case-insensitive "contains" across details, transaction id and UTR. */
    public static Specification<BankTransaction> matchesText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            // Escape LIKE wildcards typed by the user so "50%" searches for the literal text.
            String escaped = text.strip().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("details")), pattern, '\\'),
                    cb.like(cb.lower(root.get("transactionId")), pattern, '\\'),
                    cb.like(cb.lower(root.get("utr")), pattern, '\\'));
        };
    }

    /**
     * Inclusive date range on a TIMESTAMP column: {@code transacted_at >= from 00:00} and
     * {@code transacted_at < (to + 1 day) 00:00}. Using "< next day" avoids missing 23:59:59.5 times.
     */
    public static Specification<BankTransaction> transactedBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (from != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("transactedAt"), from.atStartOfDay()));
            }
            if (to != null) {
                conditions.add(cb.lessThan(root.<LocalDateTime>get("transactedAt"), to.plusDays(1).atStartOfDay()));
            }
            return conditions.isEmpty() ? null : cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    /** Inclusive amount range; either bound may be null. */
    public static Specification<BankTransaction> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (min != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("amount"), min));
            }
            if (max != null) {
                conditions.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("amount"), max));
            }
            return conditions.isEmpty() ? null : cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
