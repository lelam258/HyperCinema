package com.cinema.hyperCinema.service.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherCreateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherSearchCriteria;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherUpdateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherDetailView;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherListItem;
import com.cinema.hyperCinema.exception.voucher.VoucherAccessDeniedException;
import com.cinema.hyperCinema.exception.voucher.VoucherValidationException;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.VoucherStatus;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.UserRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for {@link VoucherServiceImpl} covering the search/listing
 * behaviour (Properties 1-4) and the create/validation/uniqueness behaviour
 * (Properties 5-7).
 *
 * <p>Per the design's Testing Strategy, the persistence layer is substituted with
 * Mockito-backed in-memory fakes so the service logic can be exercised without I/O.
 * The fake {@link PromotionRepository#findAll(Specification, Pageable)} mirrors the
 * documented query semantics (keyword on code/title, status equality) and, crucially,
 * honours the {@link Pageable}/{@link Sort} that the service constructs — so the
 * service's own sort/paging wiring is genuinely exercised (e.g. Property 1). The
 * JPA {@code PromotionSpecifications} themselves are covered by repository-slice tests.</p>
 */
class VoucherServiceProperties {

    private static final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 0, 0);

    // ----------------------------------------------------------------------
    // Generators (@Provide)
    // ----------------------------------------------------------------------

    /** A lightweight seed describing one voucher row for list-oriented properties. */
    record PromotionSeed(String code, String title, String status, int createdOffset) {
    }

    /**
     * Generates a non-empty list of voucher seeds with diverse code/title/status and
     * varied creation offsets (serves Properties 1, 2, 3, 4).
     */
    @Provide
    Arbitrary<List<PromotionSeed>> voucherList() {
        Arbitrary<String> codes = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(8);
        Arbitrary<String> titles = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(12);
        Arbitrary<String> statuses = Arbitraries.of("ACTIVE", "INACTIVE", "EXPIRED");
        Arbitrary<Integer> offsets = Arbitraries.integers().between(0, 100_000);
        Arbitrary<PromotionSeed> seed = Combinators.combine(codes, titles, statuses, offsets).as(PromotionSeed::new);
        return seed.list().ofMinSize(1).ofMaxSize(15);
    }

    /** Short keyword (lower-case) used to probe case-insensitive keyword search. */
    @Provide
    Arbitrary<String> keyword() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(2);
    }

    /** One of the three valid voucher statuses (serves Property 4). */
    @Provide
    Arbitrary<String> statusFilter() {
        return Arbitraries.of("ACTIVE", "INACTIVE", "EXPIRED");
    }

    /**
     * Generates a fully valid {@link VoucherCreateRequest}, covering both discount
     * types and both system-wide and branch-specific variants (serves Property 5/6).
     */
    @Provide
    Arbitrary<VoucherCreateRequest> validCreateRequest() {
        Arbitrary<String> title = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(30);
        Arbitrary<String> code = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20);
        Arbitrary<Integer> seed = Arbitraries.integers().between(0, 1_000_000);
        Arbitrary<Boolean> branchFlag = Arbitraries.of(true, false);
        Arbitrary<Boolean> percentageFlag = Arbitraries.of(true, false);
        return Combinators.combine(title, code, seed, branchFlag, percentageFlag)
                .as((t, c, s, branch, percentage) -> {
                    VoucherCreateRequest r = new VoucherCreateRequest();
                    r.setTitle(t);
                    r.setCode(c);
                    if (percentage) {
                        r.setDiscountType("PERCENTAGE");
                        r.setDiscountValue(Math.floorMod(s, 100) + 1);   // 1..100
                    } else {
                        r.setDiscountType("FIXED_AMOUNT");
                        r.setDiscountValue(Math.floorMod(s, 100_000) + 1); // >0
                    }
                    r.setMinOrderValue(Math.floorMod(s, 1000));
                    r.setMaxUses(Math.floorMod(s, 50) + 1);
                    LocalDateTime start = BASE.plusMinutes(Math.floorMod(s, 10_000));
                    r.setStartDate(start);
                    r.setEndDate(start.plusDays(1 + Math.floorMod(s, 30)));
                    r.setBranchSpecific(branch);
                    r.setBranchId(branch ? Math.floorMod(s, 9) + 1 : null);
                    return r;
                });
    }

    /**
     * Generates a {@link VoucherCreateRequest} that violates exactly one business rule
     * (serves Property 7). Every other field is valid so the intended violation is the
     * sole reason for rejection.
     */
    @Provide
    Arbitrary<VoucherCreateRequest> invalidCreateRequest() {
        Arbitrary<String> title = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(30);
        Arbitrary<String> code = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20);
        Arbitrary<Integer> seed = Arbitraries.integers().between(0, 1_000_000);
        Arbitrary<Integer> violation = Arbitraries.integers().between(0, 7);
        return Combinators.combine(title, code, seed, violation)
                .as((t, c, s, v) -> {
                    // Start from a fully valid (FIXED_AMOUNT, system-wide) request.
                    VoucherCreateRequest r = new VoucherCreateRequest();
                    r.setTitle(t);
                    r.setCode(c);
                    r.setDiscountType("FIXED_AMOUNT");
                    r.setDiscountValue(Math.floorMod(s, 100_000) + 1);
                    r.setMinOrderValue(Math.floorMod(s, 1000));
                    r.setMaxUses(Math.floorMod(s, 50) + 1);
                    LocalDateTime start = BASE.plusMinutes(Math.floorMod(s, 10_000));
                    r.setStartDate(start);
                    r.setEndDate(start.plusDays(1 + Math.floorMod(s, 30)));
                    r.setBranchSpecific(false);
                    r.setBranchId(null);

                    switch (v) {
                        case 0 -> r.setCode("");                       // empty code
                        case 1 -> r.setCode("x".repeat(51));           // code > 50 chars
                        case 2 -> r.setDiscountValue(0);               // discountValue <= 0
                        case 3 -> {                                    // PERCENTAGE > 100
                            r.setDiscountType("PERCENTAGE");
                            r.setDiscountValue(101 + Math.floorMod(s, 100));
                        }
                        case 4 -> r.setEndDate(r.getStartDate());      // endDate <= startDate
                        case 5 -> r.setMaxUses(0);                     // maxUses < 1
                        case 6 -> r.setMinOrderValue(-1);              // minOrderValue < 0
                        default -> r.setDiscountType("WEIRD_TYPE");    // unknown discount type
                    }
                    return r;
                });
    }

    // ======================================================================
    // Task 6.7 — search / listing properties
    // ======================================================================

    // Feature: voucher-management, Property 1: Danh sách sắp xếp theo ngày tạo giảm dần
    @Property(tries = 100)
    void listIsSortedByCreatedAtDescending(@ForAll("voucherList") List<PromotionSeed> seeds) {
        Ctx ctx = newCtx();
        List<Promotion> store = buildPromotions(seeds);
        VoucherSearchCriteria criteria = new VoucherSearchCriteria();
        criteria.setSize(1000);
        stubSearch(ctx, store, criteria);

        Page<VoucherListItem> result = ctx.service.search(criteria, null, ctx.actor);
        List<VoucherListItem> content = result.getContent();

        // Same multiset of vouchers as the source (no loss, no duplication).
        assertThat(content.stream().map(VoucherListItem::getVoucherId).sorted().toList())
                .isEqualTo(store.stream().map(Promotion::getPromotionId).sorted().toList());

        // Ordered by createdAt descending (non-increasing).
        List<LocalDateTime> createdAts = content.stream()
                .map(item -> store.stream()
                        .filter(p -> p.getPromotionId().equals(item.getVoucherId()))
                        .findFirst().orElseThrow().getCreatedAt())
                .toList();
        for (int i = 1; i < createdAts.size(); i++) {
            assertThat(createdAts.get(i - 1)).isAfterOrEqualTo(createdAts.get(i));
        }
    }

    // Feature: voucher-management, Property 2: Mapping danh sách giữ đầy đủ thông tin
    @Property(tries = 100)
    void listMappingPreservesAllFields(@ForAll("voucherList") List<PromotionSeed> seeds) {
        Ctx ctx = newCtx();
        List<Promotion> store = buildPromotions(seeds);
        VoucherSearchCriteria criteria = new VoucherSearchCriteria();
        criteria.setSize(1000);
        stubSearch(ctx, store, criteria);

        Page<VoucherListItem> result = ctx.service.search(criteria, null, ctx.actor);

        for (VoucherListItem item : result.getContent()) {
            Promotion source = store.stream()
                    .filter(p -> p.getPromotionId().equals(item.getVoucherId()))
                    .findFirst().orElseThrow();
            assertThat(item.getCode()).isEqualTo(source.getCode());
            assertThat(item.getTitle()).isEqualTo(source.getTitle());
            assertThat(item.getDiscountType()).isEqualTo(source.getDiscountType());
            assertThat(item.getDiscountValue()).isEqualTo(source.getDiscountValue());
            assertThat(item.getStartDate()).isEqualTo(source.getStartDate());
            assertThat(item.getEndDate()).isEqualTo(source.getEndDate());
            assertThat(item.getUsedCount()).isEqualTo(source.getUsedCount());
            assertThat(item.getMaxUses()).isEqualTo(source.getMaxUses());
            assertThat(item.getStatus()).isEqualTo(source.getStatus());
        }
    }

    // Feature: voucher-management, Property 3: Tìm kiếm theo từ khóa trả đúng và đủ
    @Property(tries = 100)
    void keywordSearchReturnsExactMatches(@ForAll("voucherList") List<PromotionSeed> seeds,
            @ForAll("keyword") String keyword) {
        Ctx ctx = newCtx();
        List<Promotion> store = buildPromotions(seeds);
        VoucherSearchCriteria criteria = new VoucherSearchCriteria();
        criteria.setSize(1000);
        criteria.setKeyword(keyword.toUpperCase());   // exercise case-insensitivity
        stubSearch(ctx, store, criteria);

        Page<VoucherListItem> result = ctx.service.search(criteria, null, ctx.actor);

        List<Integer> expected = store.stream()
                .filter(p -> matchesKeyword(p, criteria.getKeyword()))
                .map(Promotion::getPromotionId)
                .sorted()
                .toList();
        assertThat(result.getContent().stream().map(VoucherListItem::getVoucherId).sorted().toList())
                .isEqualTo(expected);
    }

    // Feature: voucher-management, Property 4: Lọc theo trạng thái trả đúng và đủ
    @Property(tries = 100)
    void statusFilterReturnsExactMatches(@ForAll("voucherList") List<PromotionSeed> seeds,
            @ForAll("statusFilter") String status) {
        Ctx ctx = newCtx();
        List<Promotion> store = buildPromotions(seeds);
        VoucherSearchCriteria criteria = new VoucherSearchCriteria();
        criteria.setSize(1000);
        criteria.setStatus(status.toLowerCase());      // exercise normalization
        stubSearch(ctx, store, criteria);

        Page<VoucherListItem> result = ctx.service.search(criteria, null, ctx.actor);

        List<Integer> expected = store.stream()
                .filter(p -> matchesStatus(p, criteria.getStatus()))
                .map(Promotion::getPromotionId)
                .sorted()
                .toList();
        assertThat(result.getContent().stream().map(VoucherListItem::getVoucherId).sorted().toList())
                .isEqualTo(expected);
    }

    // ======================================================================
    // Task 6.8 — create / validation / uniqueness properties
    // ======================================================================

    // Feature: voucher-management, Property 5: Tạo hợp lệ lưu đúng dữ liệu và giá trị mặc định
    @Property(tries = 100)
    void validCreatePersistsDataWithDefaults(@ForAll("validCreateRequest") VoucherCreateRequest request) {
        Ctx ctx = newCtx();
        when(ctx.promotionRepo.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        when(ctx.branchRepo.findById(anyInt())).thenAnswer(inv -> {
            Branch b = new Branch();
            b.setBranchId(inv.getArgument(0));
            b.setName("Branch-" + inv.getArgument(0));
            return Optional.of(b);
        });
        when(ctx.promotionRepo.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            p.setPromotionId(999);
            return p;
        });

        VoucherDetailView view = ctx.service.create(request, ctx.actor);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(ctx.promotionRepo).save(captor.capture());
        Promotion saved = captor.getValue();

        // Defaults.
        assertThat(saved.getStatus()).isEqualTo(VoucherStatus.ACTIVE.name());
        assertThat(saved.getUsedCount()).isEqualTo(0);

        // Remaining fields match the request.
        assertThat(saved.getTitle()).isEqualTo(request.getTitle());
        assertThat(saved.getCode()).isEqualTo(request.getCode().trim());
        assertThat(saved.getDiscountType()).isEqualTo(request.getDiscountType());
        assertThat(saved.getDiscountValue()).isEqualTo(request.getDiscountValue());
        assertThat(saved.getMinOrderValue()).isEqualTo(request.getMinOrderValue());
        assertThat(saved.getMaxUses()).isEqualTo(request.getMaxUses());
        assertThat(saved.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(saved.getEndDate()).isEqualTo(request.getEndDate());

        // Branch attachment matches branchSpecific flag.
        if (request.isBranchSpecific()) {
            assertThat(saved.getBranch()).isNotNull();
            assertThat(saved.getBranch().getBranchId()).isEqualTo(request.getBranchId());
            assertThat(view.getBranchId()).isEqualTo(request.getBranchId());
        } else {
            assertThat(saved.getBranch()).isNull();
        }
    }

    // Feature: voucher-management, Property 6: Tính duy nhất của Code khi tạo và cập nhật
    @Property(tries = 100)
    void duplicateCodeIsRejectedOnCreateAndUpdate(@ForAll("validCreateRequest") VoucherCreateRequest request) {
        // --- Create with an already-existing code → rejected, nothing saved.
        Ctx createCtx = newCtx();
        when(createCtx.promotionRepo.existsByCodeIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> createCtx.service.create(request, createCtx.actor))
                .isInstanceOf(VoucherValidationException.class);
        verify(createCtx.promotionRepo, never()).save(any(Promotion.class));

        // --- Update another voucher to a code owned by a different voucher → rejected, unchanged.
        Ctx updateCtx = newCtx();
        Promotion existing = existingVoucher(5, "originalcode");
        String originalCode = existing.getCode();
        String originalTitle = existing.getTitle();
        String originalStatus = existing.getStatus();
        when(updateCtx.promotionRepo.findById(5)).thenReturn(Optional.of(existing));
        when(updateCtx.promotionRepo.existsByCodeIgnoreCaseAndPromotionIdNot(anyString(), eq(5)))
                .thenReturn(true);

        VoucherUpdateRequest updateRequest = toUpdate(request, null);

        assertThatThrownBy(() -> updateCtx.service.update(5, updateRequest, updateCtx.actor))
                .isInstanceOf(VoucherValidationException.class);
        verify(updateCtx.promotionRepo, never()).save(any(Promotion.class));
        // Existing data unchanged.
        assertThat(existing.getCode()).isEqualTo(originalCode);
        assertThat(existing.getTitle()).isEqualTo(originalTitle);
        assertThat(existing.getStatus()).isEqualTo(originalStatus);
    }

    // Feature: voucher-management, Property 7: Xác thực đầu vào từ chối giá trị không hợp lệ
    @Property(tries = 100)
    void invalidInputIsRejectedOnCreateAndUpdate(@ForAll("invalidCreateRequest") VoucherCreateRequest request) {
        // --- Create rejects invalid input; nothing is saved.
        Ctx createCtx = newCtx();
        when(createCtx.promotionRepo.existsByCodeIgnoreCase(anyString())).thenReturn(false);

        assertThatThrownBy(() -> createCtx.service.create(request, createCtx.actor))
                .isInstanceOf(VoucherValidationException.class);
        verify(createCtx.promotionRepo, never()).save(any(Promotion.class));

        // --- Update rejects the same invalid input; existing voucher unchanged.
        Ctx updateCtx = newCtx();
        Promotion existing = existingVoucher(7, "keepcode");
        String originalCode = existing.getCode();
        Integer originalValue = existing.getDiscountValue();
        when(updateCtx.promotionRepo.findById(7)).thenReturn(Optional.of(existing));

        VoucherUpdateRequest updateRequest = toUpdate(request, null);

        assertThatThrownBy(() -> updateCtx.service.update(7, updateRequest, updateCtx.actor))
                .isInstanceOf(VoucherValidationException.class);
        verify(updateCtx.promotionRepo, never()).save(any(Promotion.class));
        assertThat(existing.getCode()).isEqualTo(originalCode);
        assertThat(existing.getDiscountValue()).isEqualTo(originalValue);
    }

    // ----------------------------------------------------------------------
    // Test fixtures & in-memory fake wiring
    // ----------------------------------------------------------------------

    /** Bundles the service under test with its mocked collaborators and an Admin actor. */
    record Ctx(VoucherServiceImpl service,
            PromotionRepository promotionRepo,
            BranchRepository branchRepo,
            BookingRepository bookingRepo,
            UserRepository userRepo,
            User actor) {
    }

    /** Builds a fresh service with mocked repositories and an Administrator actor. */
    private Ctx newCtx() {
        PromotionRepository promotionRepo = mock(PromotionRepository.class);
        BranchRepository branchRepo = mock(BranchRepository.class);
        BookingRepository bookingRepo = mock(BookingRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        Role adminRole = new Role();
        adminRole.setRoleId(1);
        adminRole.setName("Admin");

        User actor = new User();
        actor.setUserId(1);
        actor.setRole(adminRole);

        when(userRepo.findById(1)).thenReturn(Optional.of(actor));

        VoucherServiceImpl service = new VoucherServiceImpl(promotionRepo, branchRepo, bookingRepo, userRepo);
        return new Ctx(service, promotionRepo, branchRepo, bookingRepo, userRepo, actor);
    }

    /**
     * Wires the fake {@code findAll(Specification, Pageable)} to filter the store by the
     * (already-normalized) criteria and to honour the {@link Pageable}/{@link Sort} the
     * service builds.
     */
    private void stubSearch(Ctx ctx, List<Promotion> store, VoucherSearchCriteria criteria) {
        when(ctx.promotionRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> pageFor(store, criteria, inv.getArgument(1)));
    }

    private Page<Promotion> pageFor(List<Promotion> all, VoucherSearchCriteria criteria, Pageable pageable) {
        List<Promotion> filtered = new ArrayList<>();
        for (Promotion p : all) {
            if (matchesKeyword(p, criteria.getKeyword()) && matchesStatus(p, criteria.getStatus())) {
                filtered.add(p);
            }
        }
        Comparator<Promotion> comparator = comparatorFrom(pageable.getSort());
        if (comparator != null) {
            filtered.sort(comparator);
        }
        int total = filtered.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Promotion> content = start >= total ? List.of() : new ArrayList<>(filtered.subList(start, end));
        return new PageImpl<>(content, pageable, total);
    }

    private static Comparator<Promotion> comparatorFrom(Sort sort) {
        Comparator<Promotion> result = null;
        for (Sort.Order order : sort) {
            Comparator<Promotion> next = comparatorForProperty(order.getProperty());
            if (order.isDescending()) {
                next = next.reversed();
            }
            result = (result == null) ? next : result.thenComparing(next);
        }
        return result;
    }

    private static Comparator<Promotion> comparatorForProperty(String property) {
        return switch (property) {
            case "code" -> Comparator.comparing(Promotion::getCode);
            case "title" -> Comparator.comparing(Promotion::getTitle);
            case "startDate" -> Comparator.comparing(Promotion::getStartDate);
            case "endDate" -> Comparator.comparing(Promotion::getEndDate);
            case "status" -> Comparator.comparing(Promotion::getStatus);
            default -> Comparator.comparing(Promotion::getCreatedAt);
        };
    }

    private static boolean matchesKeyword(Promotion p, String keyword) {
        if (keyword == null) {
            return true;
        }
        String k = keyword.toLowerCase();
        return p.getCode().toLowerCase().contains(k) || p.getTitle().toLowerCase().contains(k);
    }

    private static boolean matchesStatus(Promotion p, String status) {
        if (status == null) {
            return true;
        }
        return status.equals(p.getStatus());
    }

    private List<Promotion> buildPromotions(List<PromotionSeed> seeds) {
        List<Promotion> list = new ArrayList<>();
        int i = 0;
        for (PromotionSeed s : seeds) {
            i++;
            Promotion p = new Promotion();
            p.setPromotionId(i);
            p.setTitle(s.title());
            p.setCode(s.code());
            p.setStatus(s.status());
            p.setDiscountType(i % 2 == 0 ? "PERCENTAGE" : "FIXED_AMOUNT");
            p.setDiscountValue(i % 2 == 0 ? (i % 100) + 1 : i * 10 + 1);
            p.setMinOrderValue(i * 5);
            p.setMaxUses(i + 10);
            p.setUsedCount(i);
            p.setStartDate(BASE);
            p.setEndDate(BASE.plusDays(10));
            p.setBranchSpecific(false);
            p.setBranch(null);
            p.setCreatedAt(BASE.plusMinutes(s.createdOffset()));
            list.add(p);
        }
        return list;
    }

    /** Builds an existing, valid voucher entity for update-path properties. */
    private Promotion existingVoucher(int id, String code) {
        Promotion p = new Promotion();
        p.setPromotionId(id);
        p.setTitle("existing-title");
        p.setCode(code);
        p.setDiscountType("FIXED_AMOUNT");
        p.setDiscountValue(500);
        p.setMinOrderValue(0);
        p.setMaxUses(10);
        p.setUsedCount(2);
        p.setStartDate(BASE);
        p.setEndDate(BASE.plusDays(5));
        p.setBranchSpecific(false);
        p.setBranch(null);
        p.setStatus(VoucherStatus.ACTIVE.name());
        p.setCreatedAt(BASE);
        return p;
    }

    private VoucherUpdateRequest toUpdate(VoucherCreateRequest r, String status) {
        VoucherUpdateRequest u = new VoucherUpdateRequest();
        u.setTitle(r.getTitle());
        u.setCode(r.getCode());
        u.setDiscountType(r.getDiscountType());
        u.setDiscountValue(r.getDiscountValue());
        u.setMinOrderValue(r.getMinOrderValue());
        u.setMaxUses(r.getMaxUses());
        u.setStartDate(r.getStartDate());
        u.setEndDate(r.getEndDate());
        u.setBranchSpecific(r.isBranchSpecific());
        u.setBranchId(r.getBranchId());
        u.setStatus(status);
        return u;
    }

    // ======================================================================
    // Task 6.9 — update & status properties
    // ======================================================================

    /** One of the two manually-settable statuses (ACTIVE / INACTIVE). */
    @Provide
    Arbitrary<String> activeInactive() {
        return Arbitraries.of(VoucherStatus.ACTIVE.name(), VoucherStatus.INACTIVE.name());
    }

    // Feature: voucher-management, Property 8: Cập nhật hợp lệ lưu thay đổi
    @Property(tries = 100)
    void validUpdatePersistsChanges(@ForAll("validCreateRequest") VoucherCreateRequest request,
            @ForAll("activeInactive") String status) {
        Ctx ctx = newCtx();
        Promotion existing = existingVoucher(8, "oldcode");
        when(ctx.promotionRepo.findById(8)).thenReturn(Optional.of(existing));
        when(ctx.promotionRepo.existsByCodeIgnoreCaseAndPromotionIdNot(anyString(), eq(8)))
                .thenReturn(false);
        when(ctx.branchRepo.findById(anyInt())).thenAnswer(inv -> {
            Branch b = new Branch();
            b.setBranchId(inv.getArgument(0));
            b.setName("Branch-" + inv.getArgument(0));
            return Optional.of(b);
        });
        when(ctx.promotionRepo.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        VoucherUpdateRequest updateRequest = toUpdate(request, status);
        ctx.service.update(8, updateRequest, ctx.actor);

        // The service mutates the loaded entity in place and persists it; re-reading the
        // entity reflects the requested values.
        assertThat(existing.getTitle()).isEqualTo(request.getTitle());
        assertThat(existing.getCode()).isEqualTo(request.getCode().trim());
        assertThat(existing.getDiscountType()).isEqualTo(request.getDiscountType());
        assertThat(existing.getDiscountValue()).isEqualTo(request.getDiscountValue());
        assertThat(existing.getMinOrderValue()).isEqualTo(request.getMinOrderValue());
        assertThat(existing.getMaxUses()).isEqualTo(request.getMaxUses());
        assertThat(existing.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(existing.getEndDate()).isEqualTo(request.getEndDate());
        assertThat(existing.getStatus()).isEqualTo(status);

        if (request.isBranchSpecific()) {
            assertThat(existing.getBranch()).isNotNull();
            assertThat(existing.getBranch().getBranchId()).isEqualTo(request.getBranchId());
        } else {
            assertThat(existing.getBranch()).isNull();
        }

        verify(ctx.promotionRepo).save(any(Promotion.class));
    }

    // Feature: voucher-management, Property 9: Đổi trạng thái ACTIVE↔INACTIVE round-trip
    @Property(tries = 100)
    void statusChangeRoundTripAndRejectsExpired(@ForAll("activeInactive") String target) {
        Ctx ctx = newCtx();
        Promotion voucher = existingVoucher(9, "rtcode");
        voucher.setStatus(target);
        when(ctx.promotionRepo.findById(9)).thenReturn(Optional.of(voucher));
        when(ctx.promotionRepo.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        String other = VoucherStatus.ACTIVE.name().equals(target)
                ? VoucherStatus.INACTIVE.name()
                : VoucherStatus.ACTIVE.name();

        // Round-trip: target → other → target restores the last-set status.
        ctx.service.changeStatus(9, other, ctx.actor);
        assertThat(voucher.getStatus()).isEqualTo(other);
        ctx.service.changeStatus(9, target, ctx.actor);
        assertThat(voucher.getStatus()).isEqualTo(target);

        // Manual status change must reject EXPIRED and leave the status untouched.
        assertThatThrownBy(() -> ctx.service.changeStatus(9, VoucherStatus.EXPIRED.name(), ctx.actor))
                .isInstanceOf(VoucherValidationException.class);
        assertThat(voucher.getStatus()).isEqualTo(target);
    }

    // ======================================================================
    // Task 6.10 — delete & expiry properties
    // ======================================================================

    /** Arbitrary voucher ids used by the delete-path properties. */
    @Provide
    Arbitrary<Integer> voucherIds() {
        return Arbitraries.integers().between(1, 10_000);
    }

    /** A seed describing one voucher row for the auto-expiry property. */
    record ExpirySeed(String status, int endOffsetMinutes) {
    }

    /**
     * Generates a non-empty list of vouchers with diverse statuses and end dates both
     * before and after the reference instant (serves Property 15).
     */
    @Provide
    Arbitrary<List<ExpirySeed>> expiryList() {
        Arbitrary<String> statuses = Arbitraries.of("ACTIVE", "INACTIVE", "EXPIRED");
        Arbitrary<Integer> offsets = Arbitraries.integers().between(-100_000, 100_000);
        Arbitrary<ExpirySeed> seed = Combinators.combine(statuses, offsets).as(ExpirySeed::new);
        return seed.list().ofMinSize(1).ofMaxSize(20);
    }

    // Feature: voucher-management, Property 10: Xóa thành công khi không có Active_Booking_Reference
    @Property(tries = 100)
    void deleteSucceedsWhenNoActiveBooking(@ForAll("voucherIds") int id) {
        Ctx ctx = newCtx();
        Promotion voucher = existingVoucher(id, "delok");
        when(ctx.promotionRepo.findById(id)).thenReturn(Optional.of(voucher));
        when(ctx.bookingRepo.existsByPromotion_PromotionIdAndStatusNot(id, "CANCELLED"))
                .thenReturn(false);

        ctx.service.delete(id, ctx.actor);

        // The voucher is removed from the repository.
        verify(ctx.promotionRepo).delete(voucher);
    }

    // Feature: voucher-management, Property 11: Xóa bị từ chối khi tồn tại Active_Booking_Reference
    @Property(tries = 100)
    void deleteRejectedWhenActiveBookingExists(@ForAll("voucherIds") int id) {
        Ctx ctx = newCtx();
        Promotion voucher = existingVoucher(id, "delno");
        when(ctx.promotionRepo.findById(id)).thenReturn(Optional.of(voucher));
        when(ctx.bookingRepo.existsByPromotion_PromotionIdAndStatusNot(id, "CANCELLED"))
                .thenReturn(true);

        assertThatThrownBy(() -> ctx.service.delete(id, ctx.actor))
                .isInstanceOf(VoucherValidationException.class);

        // The voucher remains in the repository.
        verify(ctx.promotionRepo, never()).delete(any(Promotion.class));
    }

    // Feature: voucher-management, Property 15: Tự động hết hạn voucher quá ngày
    @Property(tries = 100)
    void expireOverdueVouchersOnlyExpiresOverdueActive(@ForAll("expiryList") List<ExpirySeed> seeds) {
        Ctx ctx = newCtx();
        LocalDateTime now = BASE.plusDays(50);

        List<Promotion> store = new ArrayList<>();
        List<String> originalStatuses = new ArrayList<>();
        int i = 0;
        for (ExpirySeed s : seeds) {
            i++;
            Promotion p = new Promotion();
            p.setPromotionId(i);
            p.setTitle("title-" + i);
            p.setCode("code-" + i);
            p.setDiscountType("FIXED_AMOUNT");
            p.setDiscountValue(100);
            p.setMinOrderValue(0);
            p.setMaxUses(10);
            p.setUsedCount(0);
            p.setStartDate(BASE.minusDays(1));
            p.setEndDate(now.plusMinutes(s.endOffsetMinutes()));
            p.setBranchSpecific(false);
            p.setBranch(null);
            p.setStatus(s.status());
            p.setCreatedAt(BASE);
            store.add(p);
            originalStatuses.add(s.status());
        }

        // Fake the repository query exactly as documented: ACTIVE vouchers whose endDate < now.
        when(ctx.promotionRepo.findByStatusAndEndDateBefore(anyString(), any(LocalDateTime.class)))
                .thenAnswer(inv -> {
                    String status = inv.getArgument(0);
                    LocalDateTime cutoff = inv.getArgument(1);
                    List<Promotion> matches = new ArrayList<>();
                    for (Promotion p : store) {
                        if (status.equals(p.getStatus()) && p.getEndDate().isBefore(cutoff)) {
                            matches.add(p);
                        }
                    }
                    return matches;
                });

        long expectedChanged = 0;
        for (Promotion p : store) {
            if (VoucherStatus.ACTIVE.name().equals(p.getStatus()) && p.getEndDate().isBefore(now)) {
                expectedChanged++;
            }
        }

        int changed = ctx.service.expireOverdueVouchers(now);

        assertThat(changed).isEqualTo((int) expectedChanged);
        for (int idx = 0; idx < store.size(); idx++) {
            Promotion p = store.get(idx);
            boolean wasOverdueActive = VoucherStatus.ACTIVE.name().equals(originalStatuses.get(idx))
                    && p.getEndDate().isBefore(now);
            if (wasOverdueActive) {
                assertThat(p.getStatus()).isEqualTo(VoucherStatus.EXPIRED.name());
            } else {
                assertThat(p.getStatus()).isEqualTo(originalStatuses.get(idx));
            }
        }
    }

    // ======================================================================
    // Task 6.11 — authorization scope property
    // ======================================================================

    /** Bundles an actor, a target voucher and whether management should be permitted. */
    record AuthScenario(User actor, Promotion voucher, boolean expectedAllowed) {
    }

    /**
     * Generates an (actor, voucher) pair across Admin/Manager roles and same/different
     * branch combinations, along with the expected authorization outcome (serves
     * Property 16).
     */
    @Provide
    Arbitrary<AuthScenario> actorAndVoucher() {
        Arbitrary<String> roleName = Arbitraries.of("Admin", "Manager", "BranchManager");
        Arbitrary<Integer> actorBranch = Arbitraries.of(1, 2);
        Arbitrary<Integer> voucherBranch = Arbitraries.of(1, 2, -1); // -1 → system-wide (null)
        return Combinators.combine(roleName, actorBranch, voucherBranch)
                .as((rn, ab, vb) -> {
                    Integer voucherBranchId = vb == -1 ? null : vb;

                    Role role = new Role();
                    role.setRoleId(2);
                    role.setName(rn);

                    Branch actorBranchEntity = new Branch();
                    actorBranchEntity.setBranchId(ab);
                    actorBranchEntity.setName("Branch-" + ab);

                    User actor = new User();
                    actor.setUserId(100);
                    actor.setRole(role);
                    actor.setBranch(actorBranchEntity);

                    Promotion voucher = existingVoucher(50, "authcode");
                    if (voucherBranchId == null) {
                        voucher.setBranchSpecific(false);
                        voucher.setBranch(null);
                    } else {
                        voucher.setBranchSpecific(true);
                        Branch vb2 = new Branch();
                        vb2.setBranchId(voucherBranchId);
                        vb2.setName("Branch-" + voucherBranchId);
                        voucher.setBranch(vb2);
                    }

                    boolean isAdmin = "Admin".equals(rn);
                    boolean allowed = isAdmin
                            || (voucherBranchId != null && ab.equals(voucherBranchId));
                    return new AuthScenario(actor, voucher, allowed);
                });
    }

    // Feature: voucher-management, Property 16: Thực thi phạm vi phân quyền
    @Property(tries = 100)
    void authorizationScopeIsEnforced(@ForAll("actorAndVoucher") AuthScenario scenario) {
        PromotionRepository promotionRepo = mock(PromotionRepository.class);
        BranchRepository branchRepo = mock(BranchRepository.class);
        BookingRepository bookingRepo = mock(BookingRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        when(userRepo.findById(scenario.actor().getUserId()))
                .thenReturn(Optional.of(scenario.actor()));
        when(promotionRepo.findById(scenario.voucher().getPromotionId()))
                .thenReturn(Optional.of(scenario.voucher()));

        VoucherServiceImpl service =
                new VoucherServiceImpl(promotionRepo, branchRepo, bookingRepo, userRepo);

        if (scenario.expectedAllowed()) {
            VoucherDetailView view =
                    service.findById(scenario.voucher().getPromotionId(), scenario.actor());
            assertThat(view.getVoucherId()).isEqualTo(scenario.voucher().getPromotionId());
        } else {
            assertThatThrownBy(
                    () -> service.findById(scenario.voucher().getPromotionId(), scenario.actor()))
                    .isInstanceOf(VoucherAccessDeniedException.class);
        }
    }
}
