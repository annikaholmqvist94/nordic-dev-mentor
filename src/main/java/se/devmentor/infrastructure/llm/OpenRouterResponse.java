package se.devmentor.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body from OpenRouter's /chat/completions endpoint.
 * Package-private must not leak out of infrastructure/llm/.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenRouterResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ResponseMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseMessage(String content) {}
}
