package com.cinema.hyperCinema.util;

public final class SeatPricing {

    private static final int STANDARD_PRICE = 90_000;
    private static final int VIP_PRICE = 120_000;
    private static final int COUPLE_PRICE = 180_000;
    private static final int DISABLED_PRICE = 0;

    private SeatPricing() {
    }

    public static int priceFor(String seatType) {
        return switch (normalize(seatType)) {
            case "VIP" -> VIP_PRICE;
            case "COUPLE" -> COUPLE_PRICE;
            case "DISABLED" -> DISABLED_PRICE;
            default -> STANDARD_PRICE;
        };
    }

    private static String normalize(String seatType) {
        if (seatType == null || seatType.isBlank()) {
            return "STANDARD";
        }
        return seatType.trim().replaceAll("[\\s-]+", "_").toUpperCase();
    }
}
