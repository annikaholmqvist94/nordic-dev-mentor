package se.devmentor.domain;

import java.util.List;

/**
 * Port mot lagring av konversationshistorik.
 *
 * Just nu är implementationen in-memory.
 */
public interface ConversationStore {

    /**
     * Hämta hela historiken för en session. Returnerar tom lista om sessionen
     * inte finns ännu.
     */
    List<Message> getHistory(String sessionId);

    /**
     * Lägg till ett meddelande i sessionens historik. Skapar sessionen om den
     * inte finns. Implementationen ansvarar för att tillämpa eventuell
     * sliding window-logik.
     */
    void append(String sessionId, Message message);

    /** Rensa en session helt. */
    void clear(String sessionId);
}
