package com.walkthrough.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Application-wide beans.
 *
 * <p>A {@code @Bean} method is like an Angular provider with {@code useFactory}:
 * whatever it returns is registered in the DI container and injected wherever that type is requested.
 */
@Configuration
public class AppConfig {

    /** The system clock in UTC. Tests replace it with {@code Clock.fixed(...)} for deterministic dates. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
