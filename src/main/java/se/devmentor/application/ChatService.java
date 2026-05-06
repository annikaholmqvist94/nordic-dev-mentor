package se.devmentor.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.devmentor.domain.ConversationStore;
import se.devmentor.domain.LlmClient;
import se.devmentor.domain.MaskingResult;
import se.devmentor.domain.Message;
import se.devmentor.domain.PiiScanner;
import se.devmentor.web.dto.ChatRequest;
import se.devmentor.web.dto.ChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orkestrerar ett chatt-anrop:
 *   1. Avgör sessionId (befintligt eller nytt)
 *   2. Hämtar historik
 *   3. Maskar PII i user-input
 *   4. Bygger meddelandelistan: system prompt + historik + maskerad user message
 *   5. Anropar LLM:en
 *   6. Sparar både user message och svar i historiken
 *   7. Returnerar svar (med maskedFields) till klienten
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationStore conversationStore;
    private final LlmClient llmClient;
    private final PiiScanner piiScanner;

    public ChatResponse handleChat(ChatRequest request) {
        String sessionId = resolveSessionId(request.sessionId());
        log.debug("Handling chat for session={} personality={}", sessionId, request.personality());

        List<Message> history = conversationStore.getHistory(sessionId);
        MaskingResult masked = piiScanner.mask(request.message());
        if (!masked.types().isEmpty()) {
            log.info("Masked PII in chat input: types={} session={}", masked.types(), sessionId);
        }

        // Bygg upp meddelandelistan vi skickar till LLM:en med den MASKERADE user-texten.
        List<Message> messagesForLlm = new ArrayList<>(history.size() + 2);
        messagesForLlm.add(Message.system(request.personality().systemPrompt()));
        messagesForLlm.addAll(history);
        Message userMessage = Message.user(masked.masked());
        messagesForLlm.add(userMessage);

        // Anropa LLM:en med personlighetens egen temperature. Om det failar kastas
        // LlmServiceException som GlobalExceptionHandler mappar till 503.
        String reply = llmClient.complete(messagesForLlm, request.personality().temperature());

        // Spara först nu vi vill inte ha kvar user-meddelandet i historiken om
        // anropet failar permanent.
        conversationStore.append(sessionId, userMessage);
        conversationStore.append(sessionId, Message.assistant(reply));

        return new ChatResponse(sessionId, request.personality(), reply, masked.types());
    }

    private String resolveSessionId(String incoming) {
        return Optional.ofNullable(incoming)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    public void deleteSession(String sessionId) {
        log.debug("Deleting session={}", sessionId);
        conversationStore.clear(sessionId);
    }
}
