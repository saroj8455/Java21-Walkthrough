package com.walkthrough.api.transaction.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <b>Bulkhead</b>: at most N imports run at the same time; the rest wait briefly, then get HTTP 429.
 *
 * <p>Why? Each import holds a DB connection and does heavy inserts. Without a limit, 50 people uploading
 * at once would grab every connection in the pool and the rest of the API (dashboard, lists) would stall.
 * With N = 3 and a pool of 10, at least 7 connections always remain for normal requests.
 *
 * <p>{@link Semaphore} = a counter of permits. {@code tryAcquire} takes one (waiting up to a timeout),
 * {@code release} gives it back. Node has no direct equivalent because JS is single-threaded; the closest
 * is a promise pool like {@code p-limit}. "fair = true" means waiting callers are served in arrival order.
 *
 * <p>Note: this limit is per application instance. With several API containers, each gets its own N
 * (a distributed limit would need Redis or the DB) - see docs/05.
 */
@Component
public class ImportConcurrencyLimiter {

    private static final Logger log = LoggerFactory.getLogger(ImportConcurrencyLimiter.class);

    private final Semaphore permits;
    private final ImportProperties properties;

    public ImportConcurrencyLimiter(ImportProperties properties) {
        this.properties = properties;
        this.permits = new Semaphore(properties.maxConcurrentImports(), true);
    }

    /**
     * Runs {@code work} while holding a permit.
     *
     * <p>{@code <T>} makes the method generic: it returns whatever the supplier returns.
     * {@link Supplier} is a functional interface - pass a lambda {@code () -> doSomething()}.
     *
     * @throws TooManyImportsException when no permit becomes free within the configured timeout
     */
    public <T> T withPermit(Supplier<T> work) {
        boolean acquired;
        try {
            acquired = permits.tryAcquire(properties.acquireTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();   // keep the interrupt flag; never swallow it silently
            throw new TooManyImportsException(properties.acquireTimeout());
        }
        if (!acquired) {
            log.warn("Import rejected: all {} import slots busy", properties.maxConcurrentImports());
            throw new TooManyImportsException(properties.acquireTimeout());
        }
        try {
            return work.get();
        } finally {
            permits.release();                    // ALWAYS give the permit back, even if work throws
        }
    }

    /** For monitoring/tests. */
    public int availablePermits() {
        return permits.availablePermits();
    }
}
