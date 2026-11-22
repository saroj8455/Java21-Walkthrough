package com.walkthrough.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Central error handling for all controllers - like an Express error middleware
 * {@code app.use((err, req, res, next) => ...)} or an Angular {@code HttpInterceptor} on the server side.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} gives correct status codes for Spring's own
 * exceptions (404, 400 validation, 405 ...). Every error body follows RFC 7807 "Problem Details":
 * <pre>{ "type": "about:blank", "title": "Internal Server Error", "status": 500, "detail": "...", "instance": "/api/..." }</pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Last-resort handler: log the real error, return a generic message (never leak stack traces). */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later.");
    }
}
