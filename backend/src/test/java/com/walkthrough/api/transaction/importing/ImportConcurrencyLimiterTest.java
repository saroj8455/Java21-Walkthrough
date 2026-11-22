package com.walkthrough.api.transaction.importing;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportConcurrencyLimiterTest {

    private final ImportConcurrencyLimiter limiter =
            new ImportConcurrencyLimiter(new ImportProperties(1, Duration.ofMillis(100), 500, 1000));

    @Test
    void rejectsWhenAllPermitsAreBusyAndRecoversAfterwards() throws Exception {
        CountDownLatch workStarted = new CountDownLatch(1);
        CountDownLatch releaseWork = new CountDownLatch(1);

        // Java 21 virtual thread: a cheap thread, perfect for a short background task in a test.
        Thread busyImport = Thread.ofVirtual().start(() -> limiter.withPermit(() -> {
            workStarted.countDown();
            await(releaseWork);
            return "done";
        }));

        assertThat(workStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(limiter.availablePermits()).isZero();

        assertThatThrownBy(() -> limiter.withPermit(() -> "second upload"))
                .isInstanceOf(TooManyImportsException.class);

        releaseWork.countDown();
        busyImport.join();

        assertThat(limiter.availablePermits()).isEqualTo(1);
        assertThat(limiter.withPermit(() -> "after release")).isEqualTo("after release");
    }

    @Test
    void releasesPermitWhenWorkThrows() {
        assertThatThrownBy(() -> limiter.withPermit(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(limiter.availablePermits()).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
