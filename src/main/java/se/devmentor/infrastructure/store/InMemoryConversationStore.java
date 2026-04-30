package se.devmentor.infrastructure.store;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.devmentor.config.ConversationProperties;
import se.devmentor.domain.ConversationStore;
import se.devmentor.domain.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Enkel in-memory lagring av konversationshistorik.
 *
 * Använder ConcurrentHashMap för att hantera samtidiga anrop över olika sessioner.
 * Inom samma session synkroniseras list-operationerna manuellt eftersom append måste
 * vara atomic.
 *
 * Sliding window: behåller bara de senaste N meddelandena enligt
 * {@link ConversationProperties#maxMessages()}.
 *
 * Begränsningar :
 *   - Försvinner vid omstart
 *   - Skalar inte över flera instanser
 *   - Ingen TTL sessioner ackumuleras tills processen dör
 */
@Component
@RequiredArgsConstructor
public class InMemoryConversationStore implements ConversationStore {

    private final ConcurrentMap<String, List<Message>> sessions = new ConcurrentHashMap<>();
    private final ConversationProperties properties;

    @Override
    public List<Message> getHistory(String sessionId) {
        List<Message> messages = sessions.get(sessionId);
        if (messages == null) {
            return List.of();
        }
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }

    @Override
    public void append(String sessionId, Message message) {
        List<Message> messages = sessions.computeIfAbsent(sessionId,
                k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (messages) {
            messages.add(message);
            // Sliding window — droppa äldsta tills vi är inom gränsen
            while (messages.size() > properties.maxMessages()) {
                messages.removeFirst();
            }
        }
    }

    @Override
    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }
}
