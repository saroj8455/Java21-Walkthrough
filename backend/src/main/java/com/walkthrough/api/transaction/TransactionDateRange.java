package com.walkthrough.api.transaction;

import java.time.LocalDateTime;

/** Query projection: oldest and newest transaction (both null when the table is empty). */
public record TransactionDateRange(LocalDateTime first, LocalDateTime last) {
}
