package se.devmentor.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.devmentor.application.ChatService;
import se.devmentor.domain.Personality;
import se.devmentor.domain.PiiType;
import se.devmentor.web.dto.ChatResponse;

import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerPiiTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @Test
    void post_chat_returns_200_with_masked_fields_populated() throws Exception {
        when(chatService.handleChat(any())).thenReturn(
                new ChatResponse("sess", Personality.JUNIOR_HELPER,
                        "ok", Set.of(PiiType.PERSONNUMMER)));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personality":"junior-helper","message":"19900101-2344"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedFields", hasItem("personnummer")));
    }
}
