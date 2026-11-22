package com.walkthrough.api.transaction.dto;

import java.time.LocalDate;
import java.time.Month;

/**
 * An Indian financial year (1 April - 31 March) that contains data, used for quick-filter chips.
 * Example: {@code FY 2022-23} = 2022-04-01 .. 2023-03-31.
 */
public record FinancialYearResponse(String label, LocalDate from, LocalDate to, long count) {

    public static FinancialYearResponse of(int startYear, long count) {
        return new FinancialYearResponse(
                "FY %d-%02d".formatted(startYear, (startYear + 1) % 100),
                LocalDate.of(startYear, Month.APRIL, 1),
                LocalDate.of(startYear + 1, Month.MARCH, 31),
                count);
    }

    /** Jan-Mar belong to the financial year that started the previous April. */
    public static int startYearOf(int year, int month) {
        return month >= Month.APRIL.getValue() ? year : year - 1;
    }
}
