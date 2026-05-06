package se.devmentor.infrastructure.pii;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.devmentor.config.PiiProperties;
import se.devmentor.domain.MaskingResult;
import se.devmentor.domain.PiiType;

import static org.assertj.core.api.Assertions.assertThat;

class RegexPiiScannerTest {

    private RegexPiiScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new RegexPiiScanner(allEnabled());
    }

    @Nested
    class PersonnummerDetection {

        @Test
        void masks_valid_12_digit_personnummer() {
            MaskingResult result = scanner.mask("Mitt nr är 19900101-2344 idag");

            assertThat(result.masked()).isEqualTo("Mitt nr är [PERSONNUMMER] idag");
            assertThat(result.types()).contains(PiiType.PERSONNUMMER);
        }

        @Test
        void masks_valid_10_digit_personnummer() {
            MaskingResult result = scanner.mask("Pn: 900101-2344");

            assertThat(result.masked()).isEqualTo("Pn: [PERSONNUMMER]");
            assertThat(result.types()).contains(PiiType.PERSONNUMMER);
        }

        @Test
        void masks_personnummer_with_plus_separator() {
            MaskingResult result = scanner.mask("Pn: 200101+1234");

            assertThat(result.masked()).isEqualTo("Pn: [PERSONNUMMER]");
            assertThat(result.types()).contains(PiiType.PERSONNUMMER);
        }

        @Test
        void does_not_mask_when_luhn_fails() {
            MaskingResult result = scanner.mask("Pn: 19900101-1234");

            assertThat(result.masked()).isEqualTo("Pn: 19900101-1234");
            assertThat(result.types()).doesNotContain(PiiType.PERSONNUMMER);
        }

        @Test
        void does_not_mask_personnummer_without_separator() {
            MaskingResult result = scanner.mask("Numbers: 199001012344");

            assertThat(result.masked()).isEqualTo("Numbers: 199001012344");
            assertThat(result.types()).doesNotContain(PiiType.PERSONNUMMER);
        }
    }

    @Nested
    class PhoneDetection {

        @Test
        void masks_swedish_mobile_with_separators() {
            MaskingResult result = scanner.mask("Ring 070-123 45 67 imorgon");

            assertThat(result.masked()).isEqualTo("Ring [PHONE] imorgon");
            assertThat(result.types()).contains(PiiType.PHONE);
        }

        @Test
        void masks_swedish_mobile_compact() {
            MaskingResult result = scanner.mask("Ring 0701234567");

            assertThat(result.masked()).isEqualTo("Ring [PHONE]");
            assertThat(result.types()).contains(PiiType.PHONE);
        }

        @Test
        void masks_international_swedish_format() {
            MaskingResult result = scanner.mask("Call +46 70 123 45 67");

            assertThat(result.masked()).isEqualTo("Call [PHONE]");
            assertThat(result.types()).contains(PiiType.PHONE);
        }

        @Test
        void masks_swedish_landline() {
            MaskingResult result = scanner.mask("Tel 08-12 34 56");

            assertThat(result.masked()).isEqualTo("Tel [PHONE]");
            assertThat(result.types()).contains(PiiType.PHONE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Listen on port 8080",
                "Upgrade to version 1.2.3",
                "Order #2024010512345 has shipped",
                "ISO date 2026-05-06 is today"
        })
        void does_not_mask_phone_false_positives(String input) {
            MaskingResult result = scanner.mask(input);

            assertThat(result.types()).doesNotContain(PiiType.PHONE);
            assertThat(result.masked()).isEqualTo(input);
        }
    }

    @Nested
    class EmailDetection {

        @Test
        void masks_basic_email() {
            MaskingResult result = scanner.mask("Email: user@example.com");

            assertThat(result.masked()).isEqualTo("Email: [EMAIL]");
            assertThat(result.types()).contains(PiiType.EMAIL);
        }

        @Test
        void masks_email_with_dots_and_plus() {
            MaskingResult result = scanner.mask("Reply to first.last+tag@sub.domain.co.uk now");

            assertThat(result.masked()).isEqualTo("Reply to [EMAIL] now");
            assertThat(result.types()).contains(PiiType.EMAIL);
        }

        @Test
        void does_not_mask_string_without_at_sign() {
            MaskingResult result = scanner.mask("Visit example.com or @handle");

            assertThat(result.masked()).isEqualTo("Visit example.com or @handle");
            assertThat(result.types()).doesNotContain(PiiType.EMAIL);
        }
    }

    @Nested
    class Combined {

        @Test
        void masks_multiple_pii_types_in_single_input() {
            MaskingResult result = scanner.mask(
                    "Lisa, 19900101-2344, lisa@klarna.se vill prata.");

            assertThat(result.masked()).isEqualTo(
                    "Lisa, [PERSONNUMMER], [EMAIL] vill prata.");
            assertThat(result.types()).containsExactlyInAnyOrder(
                    PiiType.PERSONNUMMER, PiiType.EMAIL);
        }

        @Test
        void deduplicates_repeated_pii_in_types_set() {
            MaskingResult result = scanner.mask(
                    "Skicka till a@b.se eller a@b.se igen");

            assertThat(result.masked()).isEqualTo(
                    "Skicka till [EMAIL] eller [EMAIL] igen");
            assertThat(result.types()).hasSize(1).contains(PiiType.EMAIL);
        }
    }

    @Nested
    class Configuration {

        @Test
        void returns_input_unchanged_when_disabled() {
            RegexPiiScanner disabled = new RegexPiiScanner(
                    new PiiProperties(false, new PiiProperties.Types(true, true, true)));

            MaskingResult result = disabled.mask("user@example.com 19900101-2344");

            assertThat(result.masked()).isEqualTo("user@example.com 19900101-2344");
            assertThat(result.types()).isEmpty();
        }

        @Test
        void handles_empty_input_without_error() {
            MaskingResult result = scanner.mask("");

            assertThat(result.masked()).isEmpty();
            assertThat(result.types()).isEmpty();
        }

        @Test
        void respects_per_type_disable_flag() {
            RegexPiiScanner personnummerOff = new RegexPiiScanner(
                    new PiiProperties(true, new PiiProperties.Types(true, true, false)));

            MaskingResult result = personnummerOff.mask(
                    "19900101-2344 och a@b.se");

            assertThat(result.masked()).isEqualTo("19900101-2344 och [EMAIL]");
            assertThat(result.types()).contains(PiiType.EMAIL);
            assertThat(result.types()).doesNotContain(PiiType.PERSONNUMMER);
        }
    }

    private static PiiProperties allEnabled() {
        return new PiiProperties(true, new PiiProperties.Types(true, true, true));
    }
}
