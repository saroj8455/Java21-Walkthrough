package com.walkthrough.api.transaction;

/** Query projection: number of transactions in one calendar month. */
public record MonthCount(Integer year, Integer month, Long count) {
}
