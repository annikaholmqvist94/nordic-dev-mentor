package se.devmentor.infrastructure.llm;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import se.devmentor.config.OpenRouterProperties;
import se.devmentor.domain.Message;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class OpenRouterClientWireMockTest {

    private WireMockServer wireMock;
    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build();

        OpenRouterProperties props = new OpenRouterProperties(
                "test-key",
                wireMock.baseUrl(),
                "test-model",
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                new OpenRouterProperties.Retry(1, Duration.ofMillis(1), Duration.ofMillis(2))
        );

        client = new OpenRouterClient(webClient, props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void sends_temperature_in_request_body() {
        wireMock.stubFor(post("/chat/completions").willReturn(okJson("""
                {"choices":[{"message":{"content":"hi back"}}]}
                """)));

        String reply = client.complete(List.of(Message.user("hi")), 0.7);

        assertThat(reply).isEqualTo("hi back");
        wireMock.verify(postRequestedFor(urlEqualTo("/chat/completions"))
                .withRequestBody(matchingJsonPath("$.temperature", equalTo("0.7")))
                .withRequestBody(matchingJsonPath("$.model", equalTo("test-model"))));
    }
}
