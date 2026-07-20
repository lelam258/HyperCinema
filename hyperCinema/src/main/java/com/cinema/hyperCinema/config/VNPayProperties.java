package com.cinema.hyperCinema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "vnpay")
public record VNPayProperties(
        String tmnCode,
        String hashSecret,
        String payUrl,
        String returnUrl,
        String version,
        String command,
        String orderType
) {

    public VNPayProperties {
        tmnCode = normalize(tmnCode);
        hashSecret = normalize(hashSecret);
        payUrl = normalize(payUrl);
        returnUrl = normalize(returnUrl);
        version = normalize(version);
        command = normalize(command);
        orderType = normalize(orderType);
    }

    public boolean isConfigured() {
        return missingRequiredKeys().isEmpty();
    }

    public List<String> missingRequiredKeys() {
        List<String> missing = new ArrayList<>();
        addIfBlank(missing, "vnpay.tmn-code", tmnCode);
        addIfBlank(missing, "vnpay.hash-secret", hashSecret);
        addIfBlank(missing, "vnpay.pay-url", payUrl);
        addIfBlank(missing, "vnpay.return-url", returnUrl);
        addIfBlank(missing, "vnpay.version", version);
        addIfBlank(missing, "vnpay.command", command);
        addIfBlank(missing, "vnpay.order-type", orderType);
        return missing;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static void addIfBlank(List<String> missing, String key, String value) {
        if (value.isBlank()) {
            missing.add(key);
        }
    }
}
