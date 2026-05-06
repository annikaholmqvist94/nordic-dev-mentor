package se.devmentor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devmentor.pii")
public record PiiProperties(boolean enabled, Types types) {

    public record Types(boolean email, boolean phone, boolean personnummer) {}
}
