package se.devmentor.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingResultTest {

    @Test
    void none_returns_input_unchanged_with_empty_types() {
        MaskingResult result = MaskingResult.none("hello world");

        assertThat(result.masked()).isEqualTo("hello world");
        assertThat(result.types()).isEmpty();
    }

    @Test
    void none_with_empty_string_works() {
        MaskingResult result = MaskingResult.none("");

        assertThat(result.masked()).isEmpty();
        assertThat(result.types()).isEmpty();
    }
}
