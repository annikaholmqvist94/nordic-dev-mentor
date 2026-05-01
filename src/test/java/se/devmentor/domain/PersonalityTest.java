package se.devmentor.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalityTest {

    @Test
    void junior_helper_uses_pedagogical_temperature() {
        assertThat(Personality.JUNIOR_HELPER.temperature()).isEqualTo(0.5);
    }

    @Test
    void senior_architect_uses_medium_temperature() {
        assertThat(Personality.SENIOR_ARCHITECT.temperature()).isEqualTo(0.7);
    }

    @Test
    void code_reviewer_uses_strictest_temperature() {
        assertThat(Personality.CODE_REVIEWER.temperature()).isEqualTo(0.4);
    }

    @Test
    void rubber_duck_uses_balanced_temperature_for_persona_plus_joke_variation() {
        assertThat(Personality.RUBBER_DUCK.temperature()).isEqualTo(0.7);
    }
}
