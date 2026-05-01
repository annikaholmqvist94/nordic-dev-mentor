package se.devmentor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifierar att Spring-kontexten kan starta med all config.
 *
 * Sätter en dummy API-key så WebClientConfig inte failar på saknad nyckel.
 * Så fort du implementerar OpenRouterClient på riktigt kan du också skriva
 * riktiga integrationstester med WireMock i samma test-paket.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "devmentor.openrouter.api-key=test-key-not-real"
})
class DevMentorApplicationTests {

    @Test
    void contextLoads() {
        // Om Spring startar utan exceptions så har vi en korrekt grundstruktur.
    }
}
