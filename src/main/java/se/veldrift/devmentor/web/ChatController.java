package se.veldrift.devmentor.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.veldrift.devmentor.application.ChatService;
import se.veldrift.devmentor.web.dto.ChatRequest;
import se.veldrift.devmentor.web.dto.ChatResponse;

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
}
