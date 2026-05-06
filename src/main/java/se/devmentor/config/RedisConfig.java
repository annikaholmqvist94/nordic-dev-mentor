package se.devmentor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import se.devmentor.domain.Message;

/**
 * Aktiveras endast i Redis-läge. Konfigurerar en typad RedisTemplate
 * med StringRedisSerializer på keys och Jackson JSON på values så
 * lagrade Message-objekt blir läsbara via redis-cli.
 */
@Configuration
@ConditionalOnProperty(name = "devmentor.store.type", havingValue = "redis")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Message> messageRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, Message> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, Message.class));
        return template;
    }
}
