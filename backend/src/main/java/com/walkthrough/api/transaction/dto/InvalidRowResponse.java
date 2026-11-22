package com.walkthrough.api.transaction.dto;

/** One rejected line: where it is, why it failed, and its original text. */
public record InvalidRowResponse(int lineNumber, String reason, String rawRecord) {
}
