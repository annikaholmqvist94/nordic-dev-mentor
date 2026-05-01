package se.devmentor.domain;

import java.util.List;

/**
 * Port mot en LLM. Implementeras i infrastructure-lagret.
 *   - kan mocka det enkelt i tester
 *   - kan byta från OpenRouter till t.ex. Anthropic eller LM Studio utan att röra servicen
 *   - har en tydlig kontraktyta mellan affärslogik och extern integration
 */
public interface LlmClient {

    /**
     * Skicka en lista av meddelanden till LLM:en och få tillbaka assistentens svar.
     *
     * @param messages    hela kontexten — system prompt först, sedan user/assistant
     *                    i kronologisk ordning
     * @param temperature sampling temperature (0.0 = deterministic, ~1.0 = creative)
     * @return assistentens text-svar
     */
    String complete(List<Message> messages, double temperature);
}
