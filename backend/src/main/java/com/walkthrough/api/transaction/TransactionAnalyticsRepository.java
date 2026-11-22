package com.walkthrough.api.transaction;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * <b>Custom repository fragment</b>: aggregate queries that accept a dynamic {@link Specification}.
 *
 * <p>Spring Data can generate {@code findAll(spec, pageable)}, but not "SUM/COUNT/GROUP BY with a dynamic WHERE".
 * So we declare the methods here and implement them by hand in {@link TransactionAnalyticsRepositoryImpl}
 * (the {@code Impl} suffix is how Spring finds the implementation). {@link BankTransactionRepository} extends
 * this interface, so callers still use ONE repository.
 */
public interface TransactionAnalyticsRepository {

    List<TransactionTypeTotal> totalsByType(Specification<BankTransaction> filter);

    TransactionDateRange dateRange(Specification<BankTransaction> filter);

    /** Most frequent counterparties first (then by total amount). */
    List<CounterpartyTotal> topCounterparties(Specification<BankTransaction> filter, int limit);
}
