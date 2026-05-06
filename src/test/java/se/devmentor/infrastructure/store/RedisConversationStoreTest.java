package se.devmentor.infrastructure.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import se.devmentor.config.ConversationProperties;
import se.devmentor.domain.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisConversationStoreTest {

    @Mock
    private RedisTemplate<String, Message> redisTemplate;

    @Mock
    private ListOperations<String, Message> listOps;

    private RedisConversationStore store;

    @BeforeEach
    void setUp() {
        store = new RedisConversationStore(redisTemplate, new ConversationProperties(10));
    }

    @Test
    void getHistory_returns_empty_when_redis_has_no_messages() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range("ndm:session:abc", 0, -1)).thenReturn(null);

        assertThat(store.getHistory("abc")).isEmpty();
    }

    @Test
    void getHistory_returns_messages_in_order() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        List<Message> stored = List.of(Message.user("hej"), Message.assistant("hej tillbaka"));
        when(listOps.range("ndm:session:abc", 0, -1)).thenReturn(stored);

        List<Message> result = store.getHistory("abc");

        assertThat(result).containsExactlyElementsOf(stored);
    }

    @Test
    void getHistory_returns_immutable_copy() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range("ndm:session:abc", 0, -1))
                .thenReturn(List.of(Message.user("hej")));

        List<Message> result = store.getHistory("abc");

        assertThat(result.getClass().getName()).contains("Immutable");
    }

    @Test
    void append_calls_rightPush_then_trim_with_sliding_window_indices() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        Message msg = Message.user("hej");

        store.append("abc", msg);

        verify(listOps).rightPush("ndm:session:abc", msg);
        verify(listOps).trim("ndm:session:abc", -10, -1);
    }

    @Test
    void clear_deletes_namespaced_key() {
        store.clear("abc");

        verify(redisTemplate).delete("ndm:session:abc");
    }
}
