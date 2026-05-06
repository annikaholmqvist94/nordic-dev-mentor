package se.devmentor.domain;

import java.util.Set;

public record MaskingResult(String masked, Set<PiiType> types) {

    public static MaskingResult none(String input) {
        return new MaskingResult(input, Set.of());
    }
}
