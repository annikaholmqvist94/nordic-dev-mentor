package se.devmentor.infrastructure.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import se.devmentor.config.OpenRouterProperties;
import se.devmentor.domain.LlmClient;
import se.devmentor.domain.Message;
import se.devmentor.exception.LlmServiceException;

import java.util.List;
import java.util.UUID;

/**
 * OpenRouter implementation of {@link LlmClient}.
 *
 * Sends the conversation to OpenRouter's /chat/completions endpoint with
 * exponential-backoff retry on 429, 5xx, and network errors. Each logical
 * call gets a single Idempotency-Key header that is reused across retries
 * so OpenRouter dedupes server-side and we never get double-billed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterClient implements LlmClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final WebClient openRouterWebClient;
    private final OpenRouterProperties properties;

    @Override
    public String complete(List<Message> messages, double temperature) {
        OpenRouterRequest request = OpenRouterRequest.of(properties.model(), temperature, messages);
        String idempotencyKey = UUID.randomUUID().toString();

        log.debug("Calling OpenRouter model={} temperature={} messageCount={} idempotencyKey={}",
                properties.model(), temperature, messages.size(), idempotencyKey);

        OpenRouterResponse response;
        try {
            response = openRouterWebClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
                    .retryWhen(buildRetrySpec())
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("OpenRouter returned {} {}: {}",
                    ex.getStatusCode().value(),
                    ex.getStatusText(),
                    ex.getResponseBodyAsString());
            throw new LlmServiceException(
                    "OpenRouter returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (RuntimeException ex) {
            // Network failures, timeouts, JSON decoding errors all surfaced
            // as RuntimeException via .block().
            log.error("OpenRouter call failed", ex);
            throw new LlmServiceException("OpenRouter call failed: " + ex.getMessage(), ex);
        }

        String content = extractContent(response);
        if (content == null) {
            log.error("OpenRouter returned an empty or malformed response: {}", response);
            throw new LlmServiceException("OpenRouter returned an empty response");
        }
        return content;
    }

    /**
     * Exponential-backoff retry spec — retries on 429, 5xx, and network errors.
     * 4xx (other than 429) fails fast since retries won't help client errors.
     * On exhaustion, the original failure is re-thrown so the existing catch
     * blocks above produce the same LlmServiceException as before.
     */
    private Retry buildRetrySpec() {
        OpenRouterProperties.Retry config = properties.retry();
        long retries = Math.max(0, config.maxAttempts() - 1);
        return Retry.backoff(retries, config.initialBackoff())
                .maxBackoff(config.maxBackoff())
                .filter(OpenRouterClient::isRetryable)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        // Network errors, timeouts, connect failures — retry.
        return true;
    }

    private static String extractContent(OpenRouterResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()) {
            return null;
        }
        OpenRouterResponse.Choice choice = response.choices().getFirst();
        if (choice == null || choice.message() == null) {
            return null;
        }
        return choice.message().content();
    }
}
