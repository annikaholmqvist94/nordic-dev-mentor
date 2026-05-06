package se.devmentor.infrastructure.store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import se.devmentor.config.ConversationProperties;
import se.devmentor.domain.ConversationStore;
import se.devmentor.domain.Message;

import java.util.List;

/**
 * Redis-baserad lagring. Aktiveras när devmentor.store.type=redis.
 */
@Component
@ConditionalOnProperty(name = "devmentor.store.type", havingValue = "redis")
@RequiredArgsConstructor
@Slf4j
public class RedisConversationStore implements ConversationStore {

    private static final String KEY_PREFIX = "ndm:session:";

    private final RedisTemplate<String, Message> messageRedisTemplate;
    private final ConversationProperties properties;

    @Override
    public List<Message> getHistory(String sessionId) {
        String key = key(sessionId);
        List<Message> messages = messageRedisTemplate.opsForList().range(key, 0, -1);
        return messages == null ? List.of() : List.copyOf(messages);
    }

    @Override
    public void append(String sessionId, Message message) {
        String key = key(sessionId);
        messageRedisTemplate.opsForList().rightPush(key, message);
        messageRedisTemplate.opsForList().trim(key, -properties.maxMessages(), -1);
    }

    @Override
    public void clear(String sessionId) {
        messageRedisTemplate.delete(key(sessionId));
    }

    private static String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
