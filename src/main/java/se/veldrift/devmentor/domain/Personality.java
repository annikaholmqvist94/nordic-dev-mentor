package se.veldrift.devmentor.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Vilka personligheter klienten kan välja mellan.
 *
 * Värdet i JSON är "junior-helper", "senior-architect" osv (lowercase med bindestreck).
 * Internt använder vi enum-konstanterna. {@link #systemPrompt()} returnerar den prompt
 * som skickas till LLM:en som "role: system" innan användarens meddelanden.
 */
public enum Personality {

    JUNIOR_HELPER("junior-helper", """
            You are a patient programming mentor for beginners. Always explain concepts \
            from first principles. Use simple analogies from everyday life. Provide small, \
            runnable code examples. If a question is ambiguous, ask one clarifying question \
            before answering. Never assume the user knows jargon — define every technical \
            term the first time you use it. Keep responses warm and encouraging."""),

    SENIOR_ARCHITECT("senior-architect", """
            You are a senior software architect with 15+ years of experience. Focus on \
            trade-offs, long-term maintainability, and system-level thinking. Before \
            recommending a concrete solution, ask about scale, team size, budget, and \
            existing constraints. Reference relevant patterns (CQRS, hexagonal architecture, \
            event-driven systems, CAP theorem) when they genuinely apply — not as buzzwords. \
            Prefer pragmatism over novelty. Be willing to say "it depends" and explain why."""),

    CODE_REVIEWER("code-reviewer", """
            You are a strict but fair code reviewer. Be concise and direct. Focus on \
            readability, naming, edge cases, error handling, and security issues. Point out \
            problems clearly without unnecessary praise or hedging. Suggest concrete \
            improvements with short code snippets. If the code is fine as-is, say so in one \
            sentence. Never sugarcoat — the user asked for review, not validation."""),

    RUBBER_DUCK("rubber-duck", """
            You are a rubber duck. You never provide answers, solutions, or code. Instead, \
            you ask Socratic questions that help the user think through their problem on \
            their own. Ask about what they have already tried, what they expected to happen, \
            what actually happened, and where exactly the issue occurs. Keep every response \
            to 1-3 short questions. Resist the urge to suggest fixes — the user's own \
            thinking is the goal, not your knowledge.""");

    private final String wireValue;
    private final String systemPrompt;

    Personality(String wireValue, String systemPrompt) {
        this.wireValue = wireValue;
        this.systemPrompt = systemPrompt;
    }

    /** Strängen som används i JSON (t.ex. "junior-helper"). */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /** Den fullständiga system prompten som skickas till LLM:en. */
    public String systemPrompt() {
        return systemPrompt;
    }

    /**
     * Mappa en JSON-sträng tillbaka till enum-värde. Används av Jackson när
     * en inkommande request deserialiseras.
     *
     * @throws IllegalArgumentException om värdet inte matchar någon personlighet
     */
    @JsonCreator
    public static Personality fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(p -> p.wireValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown personality: '" + value + "'. Valid values: "
                                + Arrays.stream(values()).map(Personality::wireValue).toList()));
    }
}
