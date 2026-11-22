package com.walkthrough.api.common;

/**
 * Thrown when the client sent something we cannot accept -> HTTP 400.
 *
 * <p>It extends {@link RuntimeException}, an <i>unchecked</i> exception: callers are not forced to
 * write try/catch (like every JS error). Java also has <i>checked</i> exceptions (e.g. {@code IOException})
 * that the compiler forces you to handle; Spring code mostly uses unchecked ones.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
