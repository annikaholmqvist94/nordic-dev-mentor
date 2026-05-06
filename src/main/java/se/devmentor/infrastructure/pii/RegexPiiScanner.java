package se.devmentor.infrastructure.pii;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.devmentor.config.PiiProperties;
import se.devmentor.domain.MaskingResult;
import se.devmentor.domain.PiiScanner;
import se.devmentor.domain.PiiType;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RegexPiiScanner implements PiiScanner {

    private static final Pattern PERSONNUMMER = Pattern.compile(
            "\\b(?:\\d{2})?\\d{6}[-+]\\d{4}\\b");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+46[\\s-]?|0)[1-9](?:[\\s-]?\\d){5,9}\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    private final PiiProperties properties;

    @Override
    public MaskingResult mask(String input) {
        if (!properties.enabled()) {
            return MaskingResult.none(input);
        }

        Set<PiiType> typesFound = EnumSet.noneOf(PiiType.class);
        String result = input;

        if (properties.types().personnummer()) {
            result = maskPersonnummer(result, typesFound);
        }
        if (properties.types().phone()) {
            result = maskPhone(result, typesFound);
        }
        if (properties.types().email()) {
            result = maskEmail(result, typesFound);
        }

        return new MaskingResult(result, Set.copyOf(typesFound));
    }

    private static String maskEmail(String input, Set<PiiType> typesFound) {
        Matcher m = EMAIL.matcher(input);
        StringBuilder sb = new StringBuilder();
        boolean replaced = false;
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(PiiType.EMAIL.maskToken()));
            replaced = true;
        }
        m.appendTail(sb);
        if (replaced) {
            typesFound.add(PiiType.EMAIL);
        }
        return sb.toString();
    }

    private static String maskPhone(String input, Set<PiiType> typesFound) {
        Matcher m = PHONE.matcher(input);
        StringBuilder sb = new StringBuilder();
        boolean replaced = false;
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(PiiType.PHONE.maskToken()));
            replaced = true;
        }
        m.appendTail(sb);
        if (replaced) {
            typesFound.add(PiiType.PHONE);
        }
        return sb.toString();
    }

    private static String maskPersonnummer(String input, Set<PiiType> typesFound) {
        Matcher m = PERSONNUMMER.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String candidate = m.group();
            String tenDigits = lastTenDigits(candidate);
            if (luhnValid(tenDigits)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(PiiType.PERSONNUMMER.maskToken()));
                typesFound.add(PiiType.PERSONNUMMER);
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(candidate));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String lastTenDigits(String personnummer) {
        String digitsOnly = personnummer.replaceAll("[^0-9]", "");
        return digitsOnly.substring(digitsOnly.length() - 10);
    }

    private static boolean luhnValid(String tenDigits) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int digit = tenDigits.charAt(i) - '0';
            int weight = (i % 2 == 0) ? 2 : 1;
            int product = digit * weight;
            sum += (product > 9) ? (product - 9) : product;
        }
        return sum % 10 == 0;
    }
}
