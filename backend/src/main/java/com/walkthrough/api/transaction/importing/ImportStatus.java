package com.walkthrough.api.transaction.importing;

/** Lifecycle of one upload. PROCESSING is never visible to others: it changes before the transaction commits. */
public enum ImportStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
