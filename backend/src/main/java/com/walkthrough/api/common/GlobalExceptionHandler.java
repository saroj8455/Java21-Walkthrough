package com.walkthrough.api.common;

import com.walkthrough.api.transaction.importing.ImportFailedException;
import com.walkthrough.api.transaction.importing.TooManyImportsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Central error handling for all controllers - like an Express error middleware
 * {@code app.use((err, req, res, next) => ...)} or an Angular {@code HttpInterceptor} on the server side.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} gives correct status codes for Spring's own
 * exceptions (404, 400 validation, 405, 413 upload too large ...). Every error body follows RFC 7807 "Problem Details":
 * <pre>{ "type": "about:blank", "title": "Internal Server Error", "status": 500, "detail": "...", "instance": "/api/..." }</pre>
 *
 * <p>Spring picks the MOST SPECIFIC matching {@code @ExceptionHandler}; {@code Exception.class} is the fallback.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        return problem;
    }

    /** e.g. {@code ?sort=doesNotExist} on a paged endpoint. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleUnknownProperty(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Upload bulkhead is full. 429 + {@code Retry-After} header tells well-behaved clients when to try again.
     * Returning {@code ResponseEntity<ProblemDetail>} (instead of just ProblemDetail) lets us add headers.
     */
    @ExceptionHandler(TooManyImportsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyImports(TooManyImportsException ex) {
        long seconds = ex.getRetryAfter().toSeconds();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setProperty("retryAfterSeconds", seconds);  // extra JSON field, allowed by RFC 7807
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problem);
    }

    /** DB failure during an import. The transaction was rolled back; the failed attempt is in the history. */
    @ExceptionHandler(ImportFailedException.class)
    public ProblemDetail handleImportFailed(ImportFailedException ex) {
        log.error("Statement import {} failed", ex.getImportId(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Import failed and was rolled back - no transactions were saved. Please try again.");
        problem.setTitle("Import Failed");
        problem.setProperty("importId", ex.getImportId());
        return problem;
    }

    /** Last-resort handler: log the real error, return a generic message (never leak stack traces). */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later.");
    }
}
