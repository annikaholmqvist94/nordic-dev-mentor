package se.devmentor.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.devmentor.exception.LlmServiceException;
import se.devmentor.web.dto.ApiError;

import java.util.stream.Collectors;

/**
 * Centraliserad felhantering för alla controllers.
 *
 * Mappar exceptions till snygga JSON-svar istället för att läcka ut
 * stack traces eller default-Spring-felsidor.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Bean Validation-fel (t.ex. @NotBlank på message saknas) → 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", details);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "VALIDATION_ERROR", details));
    }

    /** Ogiltigt enum-värde, deserialiseringsfel etc. → 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "BAD_REQUEST", ex.getMessage()));
    }

    /** LLM-tjänsten är otillgänglig → 503. */
    @ExceptionHandler(LlmServiceException.class)
    public ResponseEntity<ApiError> handleLlmFailure(LlmServiceException ex) {
        log.error("LLM call failed", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of(503, "LLM_UNAVAILABLE",
                        "AI-tjänsten är otillgänglig just nu. Försök igen om en stund."));
    }

    /** Catch-all för oväntade fel → 500. Logga full stack trace, läck inte ut den. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INTERNAL_ERROR", "Något gick fel internt."));
    }
}
