package se.devmentor.exception;

/**
 * Kastas när anropet till LLM-leverantören misslyckas på ett sätt som
 * inte är klientens fel t.ex. natverksfel, 5xx, eller uppnådd retry-gräns.
 */
public class LlmServiceException extends RuntimeException {

    public LlmServiceException(String message) {
        super(message);
    }

    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
