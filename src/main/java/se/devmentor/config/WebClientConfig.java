package se.devmentor.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * WebClient-bönan som används för att anropa OpenRouter.
 *
 * Vi sätter generösa timeouts: connect lågt (5s) eftersom man oftast vet snabbt om
 * anslutningen kan etableras, men response-timeout högre (60s) eftersom LLM-svar
 * kan ta tid vid långa generations.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient openRouterWebClient(OpenRouterProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.connectTimeout().toMillis())
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(properties.responseTimeout().toMillis(),
                                TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // OpenRouter-specifika headers
                .defaultHeader("HTTP-Referer", "https://github.com/your-org/dev-mentor")
                .defaultHeader("X-Title", "Nordic Dev Mentor")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
