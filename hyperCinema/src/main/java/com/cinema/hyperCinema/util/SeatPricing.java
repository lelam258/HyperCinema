package com.cinema.hyperCinema.util;

public final class SeatPricing {

    public enum SupportedSeatType {
        STANDARD, VIP, COUPLE, DISABLED
    }

    private static final int STANDARD_PRICE = 90_000;
    private static final int VIP_PRICE = 120_000;
    private static final int COUPLE_PRICE = 180_000;
    private static final int DISABLED_PRICE = 0;

    private SeatPricing() {
    }

    public static int priceFor(String seatType) {
        return defaultPriceFor(seatType);
    }

    public static int defaultPriceFor(String seatType) {
        return switch (normalizeType(seatType)) {
            case "VIP" -> VIP_PRICE;
            case "COUPLE" -> COUPLE_PRICE;
            case "DISABLED" -> DISABLED_PRICE;
            default -> STANDARD_PRICE;
        };
    }

    public static String normalizeType(String seatType) {
        if (seatType == null || seatType.isBlank()) {
            return "STANDARD";
        }
        String normalized = seatType.trim().replaceAll("[\\s-]+", "_").toUpperCase();
        if ("DOUBLE".equals(normalized)) {
            return "COUPLE";
        }
        if ("STANDARD".equals(normalized) || "VIP".equals(normalized)
                || "COUPLE".equals(normalized) || "DISABLED".equals(normalized)) {
            return normalized;
        }
        return "STANDARD";
    }

    public static SupportedSeatType supportedType(String seatType) {
        return SupportedSeatType.valueOf(normalizeType(seatType));
    }

    public static java.util.List<SupportedSeatType> supportedTypes() {
        return java.util.List.of(SupportedSeatType.STANDARD, SupportedSeatType.VIP,
                SupportedSeatType.COUPLE, SupportedSeatType.DISABLED);
    }

    public static String labelFor(String seatType) {
        return switch (normalizeType(seatType)) {
            case "VIP" -> "VIP";
            case "COUPLE" -> "Đôi";
            case "DISABLED" -> "Khuyết tật";
            default -> "Thường";
        };
    }
}
