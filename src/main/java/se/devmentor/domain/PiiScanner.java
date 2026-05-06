package se.devmentor.domain;

public interface PiiScanner {

    MaskingResult mask(String input);
}
