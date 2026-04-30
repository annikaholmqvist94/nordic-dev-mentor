package se.devmentor.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Felsvar")
public record ApiError(

        @Schema(description = "HTTP-statuskod", example = "503")
        int status,

        @Schema(description = "Kort feltyp", example = "LLM_UNAVAILABLE")
        String error,

        @Schema(description = "Läsbart meddelande", example = "AI-tjänsten svarar inte just nu")
        String message,

        @Schema(description = "När felet inträffade")
        Instant timestamp
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now());
    }
}
