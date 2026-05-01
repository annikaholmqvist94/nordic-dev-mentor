package se.devmentor.infrastructure.llm;

import org.junit.jupiter.api.Test;
import se.devmentor.domain.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRouterRequestTest {

    @Test
    void of_includes_temperature() {
        List<Message> messages = List.of(Message.user("hi"));

        OpenRouterRequest request = OpenRouterRequest.of("model-x", 0.7, messages);

        assertThat(request.temperature()).isEqualTo(0.7);
    }

    @Test
    void of_maps_message_roles_to_lowercase() {
        List<Message> messages = List.of(Message.system("be nice"), Message.user("hello"));

        OpenRouterRequest request = OpenRouterRequest.of("model-x", 0.5, messages);

        assertThat(request.messages())
                .extracting("role")
                .containsExactly("system", "user");
    }
}
