package com.cinema.hyperCinema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
        return !tmnCode.isBlank()
                && !hashSecret.isBlank()
                && !payUrl.isBlank()
                && !returnUrl.isBlank()
                && !version.isBlank()
                && !command.isBlank()
                && !orderType.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
