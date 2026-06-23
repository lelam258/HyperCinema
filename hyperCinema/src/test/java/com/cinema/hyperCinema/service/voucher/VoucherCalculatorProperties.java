package com.cinema.hyperCinema.service.voucher;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based tests for {@link VoucherCalculator}.
 *
 * <p>These tests verify the universal correctness properties of the pure
 * discount-calculation component using jqwik.</p>
 */
// Feature: voucher-management, Property 14: Tính giảm giá đúng công thức và luôn được kẹp
class VoucherCalculatorProperties {

    /**
     * Immutable input bundle for the calculator: a discount type, a value that is
     * valid for that type, and a non-negative order value.
     */
    record CalculatorInput(String discountType, int discountValue, long orderValue) {
    }

    /**
     * Generates valid calculator inputs covering both discount types and boundary
     * cases (order value of 0, very large order values, percentage at 1..100,
     * fixed amounts up to a large bound).
     */
    @Provide
    Arbitrary<CalculatorInput> calculatorInput() {
        // orderValue >= 0, including 0 and very large values near Long range that
        // still keep discountValue * orderValue within long bounds.
        Arbitrary<Long> orderValue = Arbitraries.oneOf(
                Arbitraries.just(0L),
                Arbitraries.longs().between(1L, 100L),
                Arbitraries.longs().between(0L, 10_000_000_000L),
                Arbitraries.longs().between(0L, 90_000_000_000_000_000L)
        );

        // PERCENTAGE: discountValue valid range is 1..100.
        Arbitrary<CalculatorInput> percentage = Combinators
                .combine(Arbitraries.integers().between(1, 100), orderValue)
                .as((value, order) -> new CalculatorInput("PERCENTAGE", value, order));

        // FIXED_AMOUNT: discountValue is a positive integer.
        Arbitrary<CalculatorInput> fixedAmount = Combinators
                .combine(Arbitraries.integers().between(1, Integer.MAX_VALUE), orderValue)
                .as((value, order) -> new CalculatorInput("FIXED_AMOUNT", value, order));

        return Arbitraries.oneOf(percentage, fixedAmount);
    }

    @Property(tries = 100)
    void discountFollowsFormulaAndIsClamped(@ForAll("calculatorInput") CalculatorInput input) {
        long expectedRaw = switch (input.discountType()) {
            case "PERCENTAGE" -> Math.floorDiv((long) input.discountValue() * input.orderValue(), 100L);
            case "FIXED_AMOUNT" -> (long) input.discountValue();
            default -> throw new IllegalStateException("Unexpected type");
        };
        long expectedDiscount = Math.max(0L, Math.min(expectedRaw, input.orderValue()));

        long actualDiscount = VoucherCalculator.discountFor(
                input.discountType(), input.discountValue(), input.orderValue());

        // Discount equals the formula result clamped to [0, orderValue].
        assertEquals(expectedDiscount, actualDiscount);
        assertTrue(actualDiscount >= 0L && actualDiscount <= input.orderValue(),
                "discount must be within [0, orderValue]");
    }

    @Property(tries = 100)
    void finalPriceIsAlwaysWithinBounds(@ForAll("calculatorInput") CalculatorInput input) {
        long discount = VoucherCalculator.discountFor(
                input.discountType(), input.discountValue(), input.orderValue());
        long finalPrice = VoucherCalculator.finalPrice(
                input.discountType(), input.discountValue(), input.orderValue());

        // finalPrice = orderValue - discount, always within [0, orderValue].
        assertEquals(input.orderValue() - discount, finalPrice);
        assertTrue(finalPrice >= 0L && finalPrice <= input.orderValue(),
                "finalPrice must be within [0, orderValue]");
    }
}
