package se.devmentor.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.devmentor.application.ChatService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerDeleteTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @Test
    void delete_returns_no_content_and_clears_session() throws Exception {
        String sessionId = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(delete("/api/v1/chat/{sessionId}", sessionId))
                .andExpect(status().isNoContent());

        verify(chatService).deleteSession(sessionId);
    }
}
