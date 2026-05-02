package se.devmentor.infrastructure.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import se.devmentor.config.OpenRouterProperties;
import se.devmentor.domain.LlmClient;
import se.devmentor.domain.Message;
import se.devmentor.exception.LlmServiceException;

import java.util.List;
import java.util.UUID;

/**
 * OpenRouter implementation of {@link LlmClient}.
 *
 * Synchronous HTTP via Spring's RestClient. Resilience comes from a
 * RetryTemplate bean (configured in RestClientConfig) which retries on
 * 429, 5xx, and network errors with exponential backoff. Each logical
 * call carries a single Idempotency-Key UUID reused across retries so
 * OpenRouter dedupes server-side and we never get double-billed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterClient implements LlmClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RestClient openRouterRestClient;
    private final OpenRouterProperties properties;
    private final RetryTemplate openRouterRetryTemplate;

    @Override
    public String complete(List<Message> messages, double temperature) {
        OpenRouterRequest request = OpenRouterRequest.of(properties.model(), temperature, messages);
        String idempotencyKey = UUID.randomUUID().toString();

        log.debug("Calling OpenRouter model={} temperature={} messageCount={} idempotencyKey={}",
                properties.model(), temperature, messages.size(), idempotencyKey);

        OpenRouterResponse response;
        try {
            response = openRouterRetryTemplate.execute(ctx ->
                    openRouterRestClient.post()
                            .uri(CHAT_COMPLETIONS_PATH)
                            .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                            .body(request)
                            .retrieve()
                            .body(OpenRouterResponse.class));
        } catch (RestClientResponseException ex) {
            log.error("OpenRouter returned {} {}: {}",
                    ex.getStatusCode().value(),
                    ex.getStatusText(),
                    ex.getResponseBodyAsString());
            throw new LlmServiceException(
                    "OpenRouter returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            log.error("OpenRouter network/timeout failure", ex);
            throw new LlmServiceException("OpenRouter call failed: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
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
