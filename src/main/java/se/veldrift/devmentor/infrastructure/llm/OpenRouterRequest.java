package se.veldrift.devmentor.infrastructure.llm;

import se.veldrift.devmentor.domain.Message;

import java.util.List;

/**
 * Request body for OpenRouter's /chat/completions endpoint.
 *
 * Package-private on purpose this is OpenRouter's wire format and must not
 * leak out of infrastructure/llm/. The domain works in {@link Message}; this
 * record is the adapter on the way out.
 */
record OpenRouterRequest(String model, List<RequestMessage> messages) {

    /** OpenRouter expects role as lowercase: "system", "user", "assistant". */
    record RequestMessage(String role, String content) {

        static RequestMessage from(Message message) {
            return new RequestMessage(
                    message.role().name().toLowerCase(),
                    message.content());
        }
    }

    static OpenRouterRequest of(String model, List<Message> messages) {
        return new OpenRouterRequest(
                model,
                messages.stream().map(RequestMessage::from).toList());
    }
}
