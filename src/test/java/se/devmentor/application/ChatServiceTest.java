package se.devmentor.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import se.devmentor.domain.ConversationStore;
import se.devmentor.domain.LlmClient;
import se.devmentor.domain.Message;
import se.devmentor.domain.Message.Role;
import se.devmentor.domain.Personality;
import se.devmentor.exception.LlmServiceException;
import se.devmentor.web.dto.ChatRequest;
import se.devmentor.web.dto.ChatResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private static final String UUID_REGEX = "[0-9a-f-]{36}";

    private ConversationStore store;
    private LlmClient llm;
    private ChatService service;

    @BeforeEach
    void setUp() {
        store = mock(ConversationStore.class);
        llm = mock(LlmClient.class);
        service = new ChatService(store, llm);
    }

    @Test
    void generates_new_sessionId_when_request_has_none() {
        when(store.getHistory(anyString())).thenReturn(List.of());
        when(llm.complete(any(), anyDouble())).thenReturn("hi");

        ChatResponse response = service.handleChat(
                new ChatRequest(Personality.JUNIOR_HELPER, "hello", null));

        assertThat(response.sessionId()).matches(UUID_REGEX);
        assertThat(response.personality()).isEqualTo(Personality.JUNIOR_HELPER);
        assertThat(response.reply()).isEqualTo("hi");
    }

    @Test
    void calls_store_and_llm_in_correct_order() {
        String sessionId = "existing-session";
        when(store.getHistory(sessionId)).thenReturn(List.of());
        when(llm.complete(any(), anyDouble())).thenReturn("reply");

        service.handleChat(new ChatRequest(Personality.JUNIOR_HELPER, "q", sessionId));

        InOrder order = inOrder(store, llm);
        order.verify(store).getHistory(sessionId);
        order.verify(llm).complete(any(), anyDouble());
        order.verify(store).append(eq(sessionId), argThat(m -> m.role() == Role.USER));
        order.verify(store).append(eq(sessionId), argThat(m -> m.role() == Role.ASSISTANT));
    }

    @Test
    void sends_system_prompt_first_then_history_then_user_message() {
        String sessionId = "s1";
        when(store.getHistory(sessionId)).thenReturn(List.of(
                Message.user("tidigare fråga"),
                Message.assistant("tidigare svar")
        ));
        when(llm.complete(any(), anyDouble())).thenReturn("ny reply");

        service.handleChat(new ChatRequest(Personality.SENIOR_ARCHITECT, "ny fråga", sessionId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(llm).complete(captor.capture(), anyDouble());

        List<Message> sent = captor.getValue();
        assertThat(sent).hasSize(4);
        assertThat(sent.get(0).role()).isEqualTo(Role.SYSTEM);
        assertThat(sent.get(0).content()).isEqualTo(Personality.SENIOR_ARCHITECT.systemPrompt());
        assertThat(sent.get(1)).isEqualTo(Message.user("tidigare fråga"));
        assertThat(sent.get(2)).isEqualTo(Message.assistant("tidigare svar"));
        assertThat(sent.get(3)).isEqualTo(Message.user("ny fråga"));
    }

    @Test
    void passes_personality_temperature_to_llm() {
        when(store.getHistory(anyString())).thenReturn(List.of());
        when(llm.complete(any(), eq(0.4))).thenReturn("strict");

        service.handleChat(new ChatRequest(Personality.CODE_REVIEWER, "review this", "s1"));

        verify(llm).complete(any(), eq(0.4));
    }

    @Test
    void does_not_persist_user_message_when_llm_throws() {
        String sessionId = "s1";
        when(store.getHistory(sessionId)).thenReturn(List.of());
        when(llm.complete(any(), anyDouble()))
                .thenThrow(new LlmServiceException("upstream down"));

        assertThatThrownBy(() -> service.handleChat(
                new ChatRequest(Personality.JUNIOR_HELPER, "hi", sessionId)))
                .isInstanceOf(LlmServiceException.class);

        verify(store, never()).append(anyString(), any());
    }

    @Test
    void delete_clears_session_in_store() {
        service.deleteSession("abc");

        verify(store).clear("abc");
    }
}
