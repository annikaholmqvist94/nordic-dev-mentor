package se.devmentor.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import se.devmentor.domain.Personality;

@Schema(description = "Svar från LLM via Nordic Dev Mentor")
public record ChatResponse(

        @Schema(description = "Sessionens id — använd i nästa request för att fortsätta " +
                "samma konversation", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "Den personlighet som svarade", example = "senior-architect")
        Personality personality,

        @Schema(description = "Assistentens svarstext")
        String reply
) {
}
