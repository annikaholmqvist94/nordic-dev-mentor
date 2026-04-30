package se.devmentor.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import se.devmentor.domain.Personality;

/**
 * Inkommande chatt-request från klient.
 *
 * sessionId är valfri om den saknas genererar servern en ny UUID och
 * returnerar den i ChatResponse så klienten kan fortsätta konversationen.
 */
@Schema(description = "Chatt-förfrågan från klient")
public record ChatRequest(

        @Schema(description = "Vilken personlighet som ska svara",
                example = "senior-architect", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "personality is required")
        Personality personality,

        @Schema(description = "Användarens fråga",
                example = "Should I use PostgreSQL or MongoDB for a CRM?",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "message is required")
        @Size(min = 1, max = 4000, message = "message must be 1-4000 characters")
        String message,

        @Schema(description = "Existerande session-id för att fortsätta konversation. " +
                "Lämna tom för att starta en ny session.",
                example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sessionId
) {
}
