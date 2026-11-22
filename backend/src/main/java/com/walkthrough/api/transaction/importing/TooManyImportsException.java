package com.walkthrough.api.transaction.importing;

import java.time.Duration;

/** All import slots are busy -> HTTP 429 with a Retry-After header. */
public class TooManyImportsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyImportsException(Duration retryAfter) {
        super("The server is busy processing other uploads. Please retry in a few seconds.");
        this.retryAfter = retryAfter.isZero() ? Duration.ofSeconds(1) : retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
