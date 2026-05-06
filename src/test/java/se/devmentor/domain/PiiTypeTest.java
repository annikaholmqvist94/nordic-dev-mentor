package se.devmentor.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiTypeTest {

    @Test
    void wire_value_is_lowercase_for_each_type() {
        assertThat(PiiType.EMAIL.wireValue()).isEqualTo("email");
        assertThat(PiiType.PHONE.wireValue()).isEqualTo("phone");
        assertThat(PiiType.PERSONNUMMER.wireValue()).isEqualTo("personnummer");
    }

    @Test
    void mask_token_is_uppercase_in_brackets() {
        assertThat(PiiType.EMAIL.maskToken()).isEqualTo("[EMAIL]");
        assertThat(PiiType.PHONE.maskToken()).isEqualTo("[PHONE]");
        assertThat(PiiType.PERSONNUMMER.maskToken()).isEqualTo("[PERSONNUMMER]");
    }

    @Test
    void serializes_to_lowercase_string_in_json() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.writeValueAsString(PiiType.EMAIL)).isEqualTo("\"email\"");
        assertThat(mapper.writeValueAsString(PiiType.PHONE)).isEqualTo("\"phone\"");
        assertThat(mapper.writeValueAsString(PiiType.PERSONNUMMER)).isEqualTo("\"personnummer\"");
    }
}
