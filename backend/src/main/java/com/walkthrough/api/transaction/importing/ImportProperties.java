package com.walkthrough.api.transaction.importing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Type-safe binding of the {@code app.import.*} block in application.yml.
 *
 * <p>Spring converts YAML to Java types for you: {@code acquire-timeout: 5s} becomes a {@link Duration},
 * kebab-case keys map to camelCase components. Compare to reading {@code process.env.X} and parsing
 * strings by hand in Node. The compact constructor rejects nonsense values at STARTUP, not at runtime.
 */
@ConfigurationProperties(prefix = "app.import")
public record ImportProperties(
        @DefaultValue("3") int maxConcurrentImports,
        @DefaultValue("5s") Duration acquireTimeout,
        @DefaultValue("500") int batchSize,
        @DefaultValue("50000") int maxRows
) {
    public ImportProperties {
        if (maxConcurrentImports < 1) {
            throw new IllegalArgumentException("app.import.max-concurrent-imports must be >= 1");
        }
        if (batchSize < 1 || maxRows < 1) {
            throw new IllegalArgumentException("app.import.batch-size and max-rows must be >= 1");
        }
        if (acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("app.import.acquire-timeout must not be negative");
        }
    }
}
