package se.devmentor.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PiiType {
    EMAIL("email", "[EMAIL]"),
    PHONE("phone", "[PHONE]"),
    PERSONNUMMER("personnummer", "[PERSONNUMMER]");

    private final String wireValue;
    private final String maskToken;

    PiiType(String wireValue, String maskToken) {
        this.wireValue = wireValue;
        this.maskToken = maskToken;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    public String maskToken() {
        return maskToken;
    }
}
