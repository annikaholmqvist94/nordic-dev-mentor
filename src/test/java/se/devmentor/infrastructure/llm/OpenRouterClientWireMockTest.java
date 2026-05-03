package se.devmentor.infrastructure.llm;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import se.devmentor.config.OpenRouterProperties;
import se.devmentor.domain.Message;
import se.devmentor.exception.LlmServiceException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenRouterClientWireMockTest {

    private WireMockServer wireMock;
    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        client = clientWithRetries(1);
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

    @Test
    void retries_on_503_and_eventually_succeeds() {
        OpenRouterClient retryClient = clientWithRetries(3);
        String scenario = "retry-503";

        wireMock.stubFor(post("/chat/completions")
                .inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("recovered")
                .willReturn(serviceUnavailable()));
        wireMock.stubFor(post("/chat/completions")
                .inScenario(scenario)
                .whenScenarioStateIs("recovered")
                .willReturn(okJson("""
                        {"choices":[{"message":{"content":"recovered"}}]}
                        """)));

        String reply = retryClient.complete(List.of(Message.user("hi")), 0.7);

        assertThat(reply).isEqualTo("recovered");
        wireMock.verify(2, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    void retries_on_429_and_eventually_succeeds() {
        OpenRouterClient retryClient = clientWithRetries(3);
        String scenario = "retry-429";

        wireMock.stubFor(post("/chat/completions")
                .inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("recovered")
                .willReturn(aResponse().withStatus(429)));
        wireMock.stubFor(post("/chat/completions")
                .inScenario(scenario)
                .whenScenarioStateIs("recovered")
                .willReturn(okJson("""
                        {"choices":[{"message":{"content":"after rate limit"}}]}
                        """)));

        String reply = retryClient.complete(List.of(Message.user("hi")), 0.7);

        assertThat(reply).isEqualTo("after rate limit");
        wireMock.verify(2, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    void does_not_retry_on_400() {
        OpenRouterClient retryClient = clientWithRetries(3);

        wireMock.stubFor(post("/chat/completions").willReturn(badRequest()));

        assertThatThrownBy(() -> retryClient.complete(List.of(Message.user("hi")), 0.7))
                .isInstanceOf(LlmServiceException.class);
        wireMock.verify(1, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    void reuses_same_idempotency_key_across_retries() {
        OpenRouterClient retryClient = clientWithRetries(3);
        String scenario = "idem-key";

        wireMock.stubFor(post("/chat/completions")
                .inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("recovered")
                .willReturn(serviceUnavailable()));
        wireMock.stubFor(post("/chat/completions")
                .inScenario(scenario)
                .whenScenarioStateIs("recovered")
                .willReturn(okJson("""
                        {"choices":[{"message":{"content":"ok"}}]}
                        """)));

        retryClient.complete(List.of(Message.user("hi")), 0.7);

        var requests = wireMock.findAll(postRequestedFor(urlEqualTo("/chat/completions")));
        assertThat(requests).hasSize(2);
        String firstKey = requests.get(0).getHeader("Idempotency-Key");
        String secondKey = requests.get(1).getHeader("Idempotency-Key");
        assertThat(firstKey).isNotNull().matches("[0-9a-f-]{36}");
        assertThat(firstKey).isEqualTo(secondKey);
    }

    private OpenRouterClient clientWithRetries(int maxAttempts) {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(factory)
                .build();

        OpenRouterProperties props = new OpenRouterProperties(
                "test-key",
                wireMock.baseUrl(),
                "test-model",
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                new OpenRouterProperties.Retry(maxAttempts, Duration.ofMillis(1), Duration.ofMillis(2))
        );

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .exponentialBackoff(1L, 2.0, 2L)
                .retryOn(HttpServerErrorException.class)
                .retryOn(HttpClientErrorException.TooManyRequests.class)
                .retryOn(ResourceAccessException.class)
                .build();

        return new OpenRouterClient(restClient, props, retryTemplate);
    }
}
