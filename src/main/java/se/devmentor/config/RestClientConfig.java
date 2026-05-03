package se.devmentor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * RestClient + RetryTemplate beans for OpenRouter.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient openRouterRestClient(OpenRouterProperties properties) {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(properties.responseTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", "https://github.com/annikaholmqvist94/nordic-dev-mentor")
                .defaultHeader("X-Title", "Nordic Dev Mentor")
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RetryTemplate openRouterRetryTemplate(OpenRouterProperties properties) {
        OpenRouterProperties.Retry config = properties.retry();
        return RetryTemplate.builder()
                .maxAttempts(config.maxAttempts())
                .exponentialBackoff(
                        config.initialBackoff().toMillis(),
                        2.0,
                        config.maxBackoff().toMillis())
                .retryOn(HttpServerErrorException.class)
                .retryOn(HttpClientErrorException.TooManyRequests.class)
                .retryOn(ResourceAccessException.class)
                .build();
    }
}
