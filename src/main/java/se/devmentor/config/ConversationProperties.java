package se.devmentor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Inställningar för konversationshistorik.
 *
 * @param maxMessages hur många av de senaste meddelandena som behålls per session.
 *                    Sliding window: när vi sparar ett nytt meddelande och listan
 *                    redan är full, droppas det äldsta. Detta gäller bara user/assistant
 *                     system prompten är aldrig lagrad i store, den läggs på vid varje
 *                    anrop från Personality-enumen.
 */
@ConfigurationProperties(prefix = "devmentor.conversation")
public record ConversationProperties(
        int maxMessages
) {
    public ConversationProperties {
        if (maxMessages < 2) {
            throw new IllegalArgumentException("maxMessages must be at least 2");
        }
    }
}
