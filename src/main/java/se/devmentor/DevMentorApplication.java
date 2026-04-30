package se.devmentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Nordic Dev Mentor middleware mellan klient och LLM.
 *
 * Tjänsten exponerar POST /api/v1/chat och vidarebefordrar frågor till OpenRouter
 * med en personlighet (system prompt) och konversationshistorik per session.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DevMentorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevMentorApplication.class, args);
    }
}
