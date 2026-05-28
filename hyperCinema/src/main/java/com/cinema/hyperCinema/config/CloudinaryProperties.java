package com.cinema.hyperCinema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret
) {

    public CloudinaryProperties {
        cloudName = normalize(cloudName);
        apiKey = normalize(apiKey);
        apiSecret = normalize(apiSecret);
    }

    boolean isConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
