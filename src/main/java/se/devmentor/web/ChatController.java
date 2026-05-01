package se.devmentor.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.devmentor.application.ChatService;
import se.devmentor.web.dto.ChatRequest;
import se.devmentor.web.dto.ChatResponse;

@RestController
@RequestMapping(value = "/api/v1/chat", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chatta med en AI-personlighet")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Skicka ett meddelande till en AI-personlighet",
            description = "Skickar användarens meddelande till vald personlighet. " +
                    "Om sessionId saknas startas en ny konversation. " +
                    "Om sessionId anges fortsätter vi den existerande historiken."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Svar från AI:n"),
            @ApiResponse(responseCode = "400", description = "Ogiltig request"),
            @ApiResponse(responseCode = "503", description = "LLM-tjänsten otillgänglig")
    })
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.handleChat(request);
    }

    @DeleteMapping("/{sessionId}")
    @Operation(
            summary = "Radera en konversation",
            description = "Rensar in-memory historiken för angiven session-id. " +
                    "Returnerar 204 även om sessionen inte fanns (idempotent)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessionen är borta")
    })
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
