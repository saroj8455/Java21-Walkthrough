package com.walkthrough.api.transaction;

import com.walkthrough.api.common.BadRequestException;
import com.walkthrough.api.transaction.dto.FinancialYearResponse;
import com.walkthrough.api.transaction.dto.TransactionStatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    @Mock
    private BankTransactionRepository repository;

    /** @InjectMocks builds the service and passes the @Mock fields into its constructor. */
    @InjectMocks
    private TransactionQueryService service;

    @Test
    void statsCombinesDebitAndCreditTotals() {
        when(repository.totalsByType(any())).thenReturn(List.of(
                new TransactionTypeTotal(TransactionType.DEBIT, 3L, new BigDecimal("100.00"), new BigDecimal("60.00")),
                new TransactionTypeTotal(TransactionType.CREDIT, 1L, new BigDecimal("250.00"), new BigDecimal("250.00"))));
        LocalDateTime first = LocalDateTime.of(2022, 4, 1, 10, 0);
        LocalDateTime last = LocalDateTime.of(2023, 3, 31, 18, 0);
        when(repository.dateRange(any())).thenReturn(new TransactionDateRange(first, last));

        TransactionStatsResponse stats = service.stats(TransactionFilter.none());

        assertThat(stats.totalCount()).isEqualTo(4);
        assertThat(stats.netAmount()).isEqualByComparingTo("150.00");
        assertThat(stats.averageDebit()).isEqualByComparingTo("33.33");   // 100 / 3, HALF_UP
        assertThat(stats.largestCredit()).isEqualByComparingTo("250.00");
        assertThat(stats.firstTransactionAt()).isEqualTo(first);
    }

    @Test
    void statsOfAnEmptySelectionAreZero() {
        when(repository.totalsByType(any())).thenReturn(List.of());
        when(repository.dateRange(any())).thenReturn(new TransactionDateRange(null, null));

        TransactionStatsResponse stats = service.stats(TransactionFilter.none());

        assertThat(stats.totalCount()).isZero();
        assertThat(stats.totalDebit()).isEqualByComparingTo("0");
        assertThat(stats.averageCredit()).isEqualByComparingTo("0");
        assertThat(stats.lastTransactionAt()).isNull();
    }

    @Test
    void groupsMonthsIntoFinancialYearsNewestFirst() {
        when(repository.countByMonth()).thenReturn(List.of(
                new MonthCount(2022, 4, 10L),    // FY 2022-23
                new MonthCount(2023, 3, 5L),     // FY 2022-23 (Jan-Mar belong to the previous April)
                new MonthCount(2023, 4, 7L)));   // FY 2023-24

        List<FinancialYearResponse> years = service.financialYears();

        assertThat(years).extracting(FinancialYearResponse::label).containsExactly("FY 2023-24", "FY 2022-23");
        assertThat(years.get(1).count()).isEqualTo(15);
        assertThat(years.get(1).from()).isEqualTo(LocalDate.of(2022, 4, 1));
        assertThat(years.get(1).to()).isEqualTo(LocalDate.of(2023, 3, 31));
    }

    @Test
    void rejectsContradictoryFiltersBeforeQuerying() {
        TransactionFilter badDates = new TransactionFilter(null, null,
                LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 1), null, null);
        TransactionFilter badAmounts = new TransactionFilter(null, null, null, null,
                new BigDecimal("500"), new BigDecimal("100"));

        assertThatThrownBy(() -> service.stats(badDates)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.topCounterparties(badAmounts, 5)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }
}
