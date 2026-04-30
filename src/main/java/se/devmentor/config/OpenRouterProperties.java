package se.devmentor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Konfiguration för OpenRouter-integration.
 */
@ConfigurationProperties(prefix = "devmentor.openrouter")
public record OpenRouterProperties(
        String apiKey,
        String baseUrl,
        String model,
        Duration connectTimeout,
        Duration responseTimeout,
        Retry retry
) {

    public record Retry(
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff
    ) {}
}
