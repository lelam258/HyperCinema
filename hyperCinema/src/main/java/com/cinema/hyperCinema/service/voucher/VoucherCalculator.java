package com.cinema.hyperCinema.service.voucher;

/**
 * Pure component for voucher discount calculations.
 *
 * <p>All methods are pure (no I/O, no shared state) to make the discount logic
 * easy to verify with property-based testing. See design.md section 7
 * "Pure Component: VoucherCalculator".</p>
 */
public final class VoucherCalculator {

    private VoucherCalculator() {
        // Utility class - prevent instantiation.
    }

    /**
     * Computes the discount amount for an order.
     *
     * <ul>
     *     <li>{@code PERCENTAGE}: {@code floor(discountValue * orderValue / 100)}</li>
     *     <li>{@code FIXED_AMOUNT}: {@code discountValue}</li>
     * </ul>
     *
     * The result is always clamped to the range {@code [0, orderValue]} so it is
     * never negative and never exceeds the order value.
     *
     * @param discountType  the discount type ("PERCENTAGE" or "FIXED_AMOUNT")
     * @param discountValue the configured discount value
     * @param orderValue    the order total before discount
     * @return the discount amount, clamped to {@code [0, orderValue]}
     * @throws IllegalArgumentException if the discount type is unknown
     */
    public static long discountFor(String discountType, int discountValue, long orderValue) {
        long raw = switch (discountType) {
            case "PERCENTAGE"   -> Math.floorDiv((long) discountValue * orderValue, 100L);
            case "FIXED_AMOUNT" -> (long) discountValue;
            default -> throw new IllegalArgumentException("Unknown discount type");
        };
        return Math.max(0L, Math.min(raw, orderValue));
    }

    /**
     * Computes the order total after applying the discount, clamped to 0 when the
     * discount is greater than or equal to the order value.
     *
     * @param discountType  the discount type ("PERCENTAGE" or "FIXED_AMOUNT")
     * @param discountValue the configured discount value
     * @param orderValue    the order total before discount
     * @return the final price, guaranteed to be in {@code [0, orderValue]}
     */
    public static long finalPrice(String discountType, int discountValue, long orderValue) {
        return orderValue - discountFor(discountType, discountValue, orderValue);
    }
}
