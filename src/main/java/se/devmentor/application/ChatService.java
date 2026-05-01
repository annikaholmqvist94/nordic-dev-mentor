package se.devmentor.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.devmentor.domain.ConversationStore;
import se.devmentor.domain.LlmClient;
import se.devmentor.domain.Message;
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
 *   3. Bygger meddelandelistan: system prompt + historik + ny user message
 *   4. Anropar LLM:en
 *   5. Sparar både user message och svar i historiken
 *   6. Returnerar svar till klienten
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationStore conversationStore;
    private final LlmClient llmClient;

    public ChatResponse handleChat(ChatRequest request) {
        String sessionId = resolveSessionId(request.sessionId());
        log.debug("Handling chat for session={} personality={}", sessionId, request.personality());

        // Bygg upp meddelandelistan vi skickar till LLM:en.
        // System prompt först, sedan tidigare historik, sedan användarens nya meddelande.
        List<Message> history = conversationStore.getHistory(sessionId);
        List<Message> messagesForLlm = new ArrayList<>(history.size() + 2);
        messagesForLlm.add(Message.system(request.personality().systemPrompt()));
        messagesForLlm.addAll(history);
        Message userMessage = Message.user(request.message());
        messagesForLlm.add(userMessage);

        // Anropa LLM:en med personlighetens egen temperature. Om det failar kastas
        // LlmServiceException som GlobalExceptionHandler mappar till 503.
        String reply = llmClient.complete(messagesForLlm, request.personality().temperature());

        // Spara först nu vi vill inte ha kvar user-meddelandet i historiken om
        // anropet failar permanent.
        conversationStore.append(sessionId, userMessage);
        conversationStore.append(sessionId, Message.assistant(reply));

        return new ChatResponse(sessionId, request.personality(), reply);
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
