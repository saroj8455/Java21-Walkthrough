package com.walkthrough.api.transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregates built with the JPA <b>Criteria API</b> - the type-safe, programmatic way to build JPQL.
 * We need it here because the WHERE clause comes from a {@link Specification}, which produces
 * Criteria {@link Predicate}s. Each method generates ONE SQL query, for example:
 * <pre>
 *   SELECT type, count(id), sum(amount), max(amount) FROM bank_transactions
 *   WHERE type = ? AND lower(details) LIKE ? ... GROUP BY type
 * </pre>
 *
 * <p>{@link EntityManager} is the low-level JPA API (Spring Data repositories use it underneath).
 * The injected instance is a thread-safe proxy bound to the current transaction.
 */
class TransactionAnalyticsRepositoryImpl implements TransactionAnalyticsRepository {

    private final EntityManager entityManager;

    TransactionAnalyticsRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<TransactionTypeTotal> totalsByType(Specification<BankTransaction> filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TransactionTypeTotal> query = cb.createQuery(TransactionTypeTotal.class);
        Root<BankTransaction> root = query.from(BankTransaction.class);
        Path<BigDecimal> amount = root.get("amount");

        // cb.construct == JPQL "select new TransactionTypeTotal(...)"
        query.select(cb.construct(TransactionTypeTotal.class,
                        root.get("type"), cb.count(root), cb.sum(amount), cb.max(amount)))
                .where(where(filter, root, query, cb))
                .groupBy(root.get("type"));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public TransactionDateRange dateRange(Specification<BankTransaction> filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TransactionDateRange> query = cb.createQuery(TransactionDateRange.class);
        Root<BankTransaction> root = query.from(BankTransaction.class);
        Path<LocalDateTime> transactedAt = root.get("transactedAt");

        // least/greatest = MIN/MAX for non-numeric comparable values (dates)
        query.select(cb.construct(TransactionDateRange.class, cb.least(transactedAt), cb.greatest(transactedAt)))
                .where(where(filter, root, query, cb));
        return entityManager.createQuery(query).getSingleResult();   // aggregates without GROUP BY: always 1 row
    }

    @Override
    public List<CounterpartyTotal> topCounterparties(Specification<BankTransaction> filter, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CounterpartyTotal> query = cb.createQuery(CounterpartyTotal.class);
        Root<BankTransaction> root = query.from(BankTransaction.class);
        Expression<Long> count = cb.count(root);
        Expression<BigDecimal> total = cb.sum(root.<BigDecimal>get("amount"));

        query.select(cb.construct(CounterpartyTotal.class, root.get("details"), count, total))
                .where(where(filter, root, query, cb))
                .groupBy(root.get("details"))
                .orderBy(cb.desc(count), cb.desc(total));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();   // SQL LIMIT
    }

    /** Turns the specification into a predicate; "no filter" becomes an always-true conjunction. */
    private static Predicate where(Specification<BankTransaction> filter, Root<BankTransaction> root,
                                   CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = filter == null ? null : filter.toPredicate(root, query, cb);
        return predicate == null ? cb.conjunction() : predicate;
    }
}
