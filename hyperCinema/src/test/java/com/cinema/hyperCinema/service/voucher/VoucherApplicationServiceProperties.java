package com.cinema.hyperCinema.service.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherPreview;
import com.cinema.hyperCinema.model.Booking;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.PromotionUsage;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.PromotionUsageRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for {@link VoucherApplicationServiceImpl} covering the
 * booking-time application behaviour: the applicability decision (Property 12)
 * and the successful-apply increment (Property 13).
 *
 * <p>Per the design's Testing Strategy, the persistence layer is substituted with
 * Mockito-backed in-memory fakes so the service logic is exercised without I/O.
 * The fake {@link PromotionRepository#findByCodeIgnoreCase(String)} returns the
 * single seeded voucher (or empty for the "not found" scenario), mirroring the
 * documented lookup semantics, and the {@link PromotionUsageRepository#save} call
 * is captured to assert the usage record that {@code apply} creates.</p>
 */
class VoucherApplicationServiceProperties {

    private static final String STORED_CODE = "voucher";
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 12, 0);

    // ----------------------------------------------------------------------
    // Generators (@Provide)
    // ----------------------------------------------------------------------

    /**
     * One applicability scenario: the voucher to seed (or {@code null} for the
     * not-found case), the order value / branch / now to evaluate against, and the
     * expected outcome. Each generated scenario either satisfies every applicability
     * condition (valid) or violates exactly one (so the resulting {@code errorKey} is
     * deterministic given the documented check order).
     */
    record OrderScenario(Promotion voucher, long orderValue, Integer bookingBranchId,
            boolean expectedValid, String expectedErrorKey) {
    }

    /**
     * Generates a mix of fully-valid order scenarios and scenarios violating exactly
     * one applicability condition (serves Property 12). Violation index:
     * <ul>
     *   <li>0 - voucher does not exist</li>
     *   <li>1 - status INACTIVE</li>
     *   <li>2 - status EXPIRED</li>
     *   <li>3 - now after endDate (out of period)</li>
     *   <li>4 - now before startDate (out of period)</li>
     *   <li>5 - usedCount has reached maxUses (exhausted)</li>
     *   <li>6 - orderValue below minOrderValue</li>
     *   <li>7 - branch-specific voucher, booking branch differs</li>
     *   <li>8+ - fully valid</li>
     * </ul>
     */
    @Provide
    Arbitrary<OrderScenario> orderScenario() {
        Arbitrary<Integer> seed = Arbitraries.integers().between(0, 1_000_000);
        Arbitrary<Boolean> percentageFlag = Arbitraries.of(true, false);
        Arbitrary<Boolean> branchSpecificFlag = Arbitraries.of(true, false);
        // 0..7 violate exactly one condition; 8..12 are valid (weighted towards valid).
        Arbitrary<Integer> violation = Arbitraries.integers().between(0, 12);

        return Combinators.combine(seed, percentageFlag, branchSpecificFlag, violation)
                .as((s, percentage, branchSpecific, v) -> {
                    // ---- Base values that satisfy every applicability condition. ----
                    int minOrder = Math.floorMod(s, 1000);
                    long orderValue = minOrder + Math.floorMod(s, 5000); // >= minOrder
                    int maxUses = Math.floorMod(s, 50) + 2;              // >= 2
                    int usedCount = Math.floorMod(s, maxUses);          // < maxUses
                    Integer voucherBranchId = Math.floorMod(s, 9) + 1;
                    Integer bookingBranchId = branchSpecific ? voucherBranchId : Math.floorMod(s, 9) + 1;

                    Promotion p = buildVoucher(s, percentage, branchSpecific, voucherBranchId);
                    p.setMinOrderValue(minOrder);
                    p.setMaxUses(maxUses);
                    p.setUsedCount(usedCount);
                    p.setStatus("ACTIVE");
                    p.setStartDate(NOW.minusDays(1));
                    p.setEndDate(NOW.plusDays(1));

                    switch (v) {
                        case 0:
                            return new OrderScenario(null, orderValue, bookingBranchId,
                                    false, "voucher.apply.not_found");
                        case 1:
                            p.setStatus("INACTIVE");
                            return new OrderScenario(p, orderValue, bookingBranchId,
                                    false, "voucher.apply.inactive");
                        case 2:
                            p.setStatus("EXPIRED");
                            return new OrderScenario(p, orderValue, bookingBranchId,
                                    false, "voucher.apply.inactive");
                        case 3:
                            p.setStartDate(NOW.minusDays(5));
                            p.setEndDate(NOW.minusDays(1));   // now after endDate
                            return new OrderScenario(p, orderValue, bookingBranchId,
                                    false, "voucher.apply.out_of_period");
                        case 4:
                            p.setStartDate(NOW.plusDays(1));  // now before startDate
                            p.setEndDate(NOW.plusDays(5));
                            return new OrderScenario(p, orderValue, bookingBranchId,
                                    false, "voucher.apply.out_of_period");
                        case 5:
                            p.setUsedCount(p.getMaxUses()); // reached limit
                            return new OrderScenario(p, orderValue, bookingBranchId,
                                    false, "voucher.apply.exhausted");
                        case 6:
                            // Order strictly below the minimum (min >= 1 guaranteed below).
                            p.setMinOrderValue(minOrder + 1);
                            return new OrderScenario(p, (long) minOrder, bookingBranchId,
                                    false, "voucher.apply.below_min_order");
                        case 7:
                            // Force branch-specific with a mismatching booking branch.
                            p.setBranchSpecific(true);
                            p.setBranch(branch(voucherBranchId));
                            return new OrderScenario(p, orderValue, voucherBranchId + 100,
                                    false, "voucher.apply.branch_mismatch");
                        default:
                            return new OrderScenario(p, orderValue, bookingBranchId, true, null);
                    }
                });
    }

    /** Generates a valid, applicable voucher for the apply-increment property. */
    @Provide
    Arbitrary<Promotion> applicableVoucher() {
        Arbitrary<Integer> seed = Arbitraries.integers().between(0, 1_000_000);
        Arbitrary<Boolean> percentageFlag = Arbitraries.of(true, false);
        return Combinators.combine(seed, percentageFlag).as((s, percentage) -> {
            Promotion p = buildVoucher(s, percentage, false, null);
            p.setMinOrderValue(0);
            p.setMaxUses(Math.floorMod(s, 50) + 2);
            p.setUsedCount(Math.floorMod(s, p.getMaxUses() == null ? 1 : p.getMaxUses()));
            p.setStatus("ACTIVE");
            p.setStartDate(NOW.minusDays(1));
            p.setEndDate(NOW.plusDays(1));
            return p;
        });
    }

    // ======================================================================
    // Task 8.2 — voucher applicability property
    // ======================================================================

    // Feature: voucher-management, Property 12: Điều kiện áp dụng voucher khi đặt vé
    @Property(tries = 100)
    void validateAndPreviewIsValidIffAllConditionsHold(@ForAll("orderScenario") OrderScenario scenario) {
        Ctx ctx = newCtx();
        Integer bookingBranchId = scenario.bookingBranchId();
        when(ctx.promotionRepo.findByCodeIgnoreCase(anyString()))
                .thenReturn(Optional.ofNullable(scenario.voucher()));

        VoucherPreview preview = ctx.service.validateAndPreview(STORED_CODE, scenario.orderValue(),
                bookingBranchId, NOW);

        assertThat(preview.isValid()).isEqualTo(scenario.expectedValid());
        if (scenario.expectedValid()) {
            // Valid result carries no error and the discount computed by VoucherCalculator.
            assertThat(preview.getErrorKey()).isNull();
            Promotion v = scenario.voucher();
            long expectedDiscount = VoucherCalculator.discountFor(
                    v.getDiscountType(), v.getDiscountValue(), scenario.orderValue());
            assertThat(preview.getDiscountAmount()).isEqualTo(expectedDiscount);
            assertThat(preview.getFinalPrice()).isEqualTo(scenario.orderValue() - expectedDiscount);
        } else {
            assertThat(preview.getErrorKey()).isEqualTo(scenario.expectedErrorKey());
        }
    }

    // ======================================================================
    // Task 8.3 — apply increment property
    // ======================================================================

    // Feature: voucher-management, Property 13: Áp dụng thành công tăng usedCount đúng một
    @Property(tries = 100)
    void applyIncrementsUsedCountByOneAndRecordsUsage(@ForAll("applicableVoucher") Promotion voucher) {
        Ctx ctx = newCtx();
        int previousUsedCount = voucher.getUsedCount();
        when(ctx.promotionRepo.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(voucher));
        when(ctx.usageRepo.save(any(PromotionUsage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ctx.promotionRepo.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = new Booking();
        booking.setBookingId(42);
        User user = new User();
        user.setUserId(7);
        booking.setUser(user);

        ctx.service.apply(STORED_CODE, booking, NOW);

        // usedCount increased by exactly one.
        assertThat(voucher.getUsedCount()).isEqualTo(previousUsedCount + 1);

        // A new PromotionUsage record was created and linked to this voucher and booking.
        ArgumentCaptor<PromotionUsage> captor = ArgumentCaptor.forClass(PromotionUsage.class);
        verify(ctx.usageRepo).save(captor.capture());
        PromotionUsage usage = captor.getValue();
        assertThat(usage.getPromotion()).isSameAs(voucher);
        assertThat(usage.getBooking()).isSameAs(booking);
        assertThat(usage.getUser()).isSameAs(user);
        assertThat(usage.getUsedAt()).isEqualTo(NOW);

        // The updated voucher is persisted.
        verify(ctx.promotionRepo).save(voucher);
    }

    // ----------------------------------------------------------------------
    // Test fixtures & in-memory fake wiring
    // ----------------------------------------------------------------------

    /** Bundles the service under test with its mocked collaborators. */
    record Ctx(VoucherApplicationServiceImpl service,
            PromotionRepository promotionRepo,
            PromotionUsageRepository usageRepo) {
    }

    private Ctx newCtx() {
        PromotionRepository promotionRepo = mock(PromotionRepository.class);
        PromotionUsageRepository usageRepo = mock(PromotionUsageRepository.class);
        VoucherApplicationServiceImpl service = new VoucherApplicationServiceImpl(promotionRepo, usageRepo);
        return new Ctx(service, promotionRepo, usageRepo);
    }

    /** Builds a voucher with diverse but valid discount config for the given seed. */
    private static Promotion buildVoucher(int seed, boolean percentage, boolean branchSpecific,
            Integer branchId) {
        Promotion p = new Promotion();
        p.setPromotionId(Math.floorMod(seed, 10_000) + 1);
        p.setTitle("voucher-" + seed);
        p.setCode(STORED_CODE);
        if (percentage) {
            p.setDiscountType("PERCENTAGE");
            p.setDiscountValue(Math.floorMod(seed, 100) + 1);   // 1..100
        } else {
            p.setDiscountType("FIXED_AMOUNT");
            p.setDiscountValue(Math.floorMod(seed, 100_000) + 1); // > 0
        }
        p.setBranchSpecific(branchSpecific);
        p.setBranch(branchSpecific ? branch(branchId) : null);
        p.setCreatedAt(NOW);
        return p;
    }

    private static Branch branch(Integer branchId) {
        Branch b = new Branch();
        b.setBranchId(branchId);
        b.setName("Branch-" + branchId);
        return b;
    }
}
