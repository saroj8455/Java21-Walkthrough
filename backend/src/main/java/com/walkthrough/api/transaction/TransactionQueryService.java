package com.walkthrough.api.transaction;

import com.walkthrough.api.common.BadRequestException;
import com.walkthrough.api.common.PageResponse;
import com.walkthrough.api.transaction.dto.BankTransactionResponse;
import com.walkthrough.api.transaction.dto.CounterpartyResponse;
import com.walkthrough.api.transaction.dto.FinancialYearResponse;
import com.walkthrough.api.transaction.dto.TransactionStatsResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only queries over imported transactions. Every method takes the same {@link TransactionFilter},
 * so the table, the KPI cards and the counterparty chips always agree with each other.
 */
@Service
@Transactional(readOnly = true)   // on the class = applies to every public method
public class TransactionQueryService {

    /** Only these fields may be used in ?sort=... (avoids errors and sorting on unindexed columns). */
    private static final Set<String> SORTABLE = Set.of("transactedAt", "amount", "details", "type");
    private static final int MAX_COUNTERPARTIES = 20;

    private final BankTransactionRepository repository;

    public TransactionQueryService(BankTransactionRepository repository) {
        this.repository = repository;
    }

    public PageResponse<BankTransactionResponse> search(TransactionFilter filter, Pageable pageable) {
        filter.validate();
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '%s'. Allowed: %s"
                        .formatted(order.getProperty(), SORTABLE));
            }
        }
        return PageResponse.from(repository.findAll(filter.toSpecification(), pageable),
                BankTransactionResponse::from);
    }

    /** KPIs for exactly the rows the table shows (2 small aggregate queries, no rows loaded). */
    public TransactionStatsResponse stats(TransactionFilter filter) {
        filter.validate();
        Specification<BankTransaction> spec = filter.toSpecification();

        // Collectors.toMap(keyFn, valueFn): List<TransactionTypeTotal> -> Map<TransactionType, TransactionTypeTotal>
        Map<TransactionType, TransactionTypeTotal> byType = repository.totalsByType(spec).stream()
                .collect(Collectors.toMap(TransactionTypeTotal::type, Function.identity()));
        TransactionDateRange range = repository.dateRange(spec);

        Optional<TransactionTypeTotal> debit = Optional.ofNullable(byType.get(TransactionType.DEBIT));
        Optional<TransactionTypeTotal> credit = Optional.ofNullable(byType.get(TransactionType.CREDIT));

        long debitCount = debit.map(TransactionTypeTotal::count).orElse(0L);
        long creditCount = credit.map(TransactionTypeTotal::count).orElse(0L);
        BigDecimal totalDebit = debit.map(TransactionTypeTotal::total).orElse(BigDecimal.ZERO);
        BigDecimal totalCredit = credit.map(TransactionTypeTotal::total).orElse(BigDecimal.ZERO);

        return new TransactionStatsResponse(
                debitCount + creditCount,
                debitCount,
                creditCount,
                totalDebit,
                totalCredit,
                totalCredit.subtract(totalDebit),   // BigDecimal: methods instead of operators (+ - * /)
                average(totalDebit, debitCount),
                average(totalCredit, creditCount),
                debit.map(TransactionTypeTotal::largest).orElse(BigDecimal.ZERO),
                credit.map(TransactionTypeTotal::largest).orElse(BigDecimal.ZERO),
                range.first(),
                range.last());
    }

    public List<CounterpartyResponse> topCounterparties(TransactionFilter filter, int limit) {
        filter.validate();
        int safeLimit = Math.clamp(limit, 1, MAX_COUNTERPARTIES);   // Java 21 Math.clamp
        return repository.topCounterparties(filter.toSpecification(), safeLimit).stream()
                .map(CounterpartyResponse::from)
                .toList();
    }

    /**
     * Financial years that contain data, newest first.
     * groupingBy(classifier, mapFactory, downstream) = "GROUP BY fy, SUM(count)" in memory;
     * TreeMap keeps keys sorted, descendingMap() flips the order.
     */
    public List<FinancialYearResponse> financialYears() {
        TreeMap<Integer, Long> countByStartYear = repository.countByMonth().stream()
                .collect(Collectors.groupingBy(
                        m -> FinancialYearResponse.startYearOf(m.year(), m.month()),
                        TreeMap::new,
                        Collectors.summingLong(MonthCount::count)));

        return countByStartYear.descendingMap().entrySet().stream()
                .map(entry -> FinancialYearResponse.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** BigDecimal division needs an explicit scale + rounding mode (1/3 has no exact decimal form). */
    private static BigDecimal average(BigDecimal total, long count) {
        return count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
