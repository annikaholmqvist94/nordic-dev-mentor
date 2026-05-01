package se.devmentor.infrastructure.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import se.devmentor.config.OpenRouterProperties;
import se.devmentor.domain.LlmClient;
import se.devmentor.domain.Message;
import se.devmentor.exception.LlmServiceException;

import java.util.List;

/**
 * OpenRouter implementation of {@link LlmClient}.
 *
 * Sends the conversation to OpenRouter's /chat/completions endpoint and returns
 * the assistant's reply. Resilience (retry/backoff, idempotency) is added in
 *  TODO inside {@link #complete}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterClient implements LlmClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final WebClient openRouterWebClient;
    private final OpenRouterProperties properties;

    @Override
    public String complete(List<Message> messages, double temperature) {
        OpenRouterRequest request = OpenRouterRequest.of(properties.model(), temperature, messages);
        log.debug("Calling OpenRouter model={} temperature={} messageCount={}",
                properties.model(), temperature, messages.size());

        // TODO : wrap this call with Retry.backoff(...) retry only on
        //              5xx, 429 and network timeouts (NEVER on 4xx). Add an
        //              Idempotency-Key header generated ONCE per logical call
        //              so retries are deduped server-side and we don't get
        //              double-billed for the same prompt.
        OpenRouterResponse response;
        try {
            response = openRouterWebClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
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
