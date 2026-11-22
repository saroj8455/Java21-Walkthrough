package com.walkthrough.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Application entry point - the Java equivalent of Angular's {@code main.ts} / Node's {@code server.js}.
 *
 * <p>{@code @SpringBootApplication} is a shortcut for three annotations:
 * <ul>
 *   <li>{@code @Configuration}       - this class may declare beans (like Angular providers).</li>
 *   <li>{@code @EnableAutoConfiguration} - Spring inspects the classpath (pom.xml dependencies) and
 *       configures things for you, e.g. sees the Postgres driver + JPA, so it creates a DataSource.</li>
 *   <li>{@code @ComponentScan}       - finds every {@code @Component/@Service/@Repository/@RestController}
 *       in this package and sub-packages and registers them in the DI container.</li>
 * </ul>
 * That's why every class lives under {@code com.walkthrough.api.*}: anything outside is not scanned.
 *
 * <p>{@code @ConfigurationPropertiesScan} finds {@code @ConfigurationProperties} records
 * (e.g. {@code ImportProperties}) and binds application.yml values into them.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class WalkthroughApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalkthroughApiApplication.class, args);
    }
}
