package com.cinema.hyperCinema.service.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherCreateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherSearchCriteria;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherUpdateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherDetailView;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherListItem;
import com.cinema.hyperCinema.exception.voucher.VoucherNotFoundException;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.VoucherStatus;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.UserRepository;

/**
 * Unit tests for {@link VoucherServiceImpl} covering concrete examples and edge cases
 * (per the design's Testing Strategy → Unit Tests section), complementing the
 * universal properties exercised in {@code VoucherServiceProperties}.
 *
 * <p>The persistence layer is substituted with Mockito mocks, mirroring the mocking
 * approach used by {@code VoucherServiceProperties}. Each test wires only the
 * collaborators required for the path under test and asserts the documented behaviour.</p>
 *
 * <p>Covered edge cases:</p>
 * <ul>
 *   <li>Empty voucher list → {@code search} returns an empty page (Req 1.3).</li>
 *   <li>{@code update}/{@code delete} with a non-existent id → {@link VoucherNotFoundException}
 *       (Req 3.3, 4.3).</li>
 *   <li>Admin creates a system-wide voucher with {@code branchSpecific=false} →
 *       saved voucher has {@code branch == null} (Req 7.5).</li>
 * </ul>
 */
class VoucherServiceImplTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final Integer ADMIN_ID = 1;

    private PromotionRepository promotionRepository;
    private BranchRepository branchRepository;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private VoucherServiceImpl service;
    private User adminActor;

    @BeforeEach
    void setUp() {
        promotionRepository = org.mockito.Mockito.mock(PromotionRepository.class);
        branchRepository = org.mockito.Mockito.mock(BranchRepository.class);
        bookingRepository = org.mockito.Mockito.mock(BookingRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);

        Role adminRole = new Role();
        adminRole.setRoleId(1);
        adminRole.setName("Admin");

        adminActor = new User();
        adminActor.setUserId(ADMIN_ID);
        adminActor.setRole(adminRole);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminActor));

        service = new VoucherServiceImpl(promotionRepository, branchRepository, bookingRepository, userRepository);
    }

    // ----------------------------------------------------------------------
    // Req 1.3 — empty list surfaces an empty result (controller shows empty-state)
    // ----------------------------------------------------------------------

    @Test
    void search_whenNoVouchersExist_returnsEmptyPage() {
        when(promotionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(), Pageable.unpaged(), 0));

        Page<VoucherListItem> result = service.search(new VoucherSearchCriteria(), null, adminActor);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ----------------------------------------------------------------------
    // Req 3.3 — update with a non-existent id throws VoucherNotFoundException
    // ----------------------------------------------------------------------

    @Test
    void update_whenVoucherDoesNotExist_throwsVoucherNotFound() {
        Integer missingId = 4242;
        when(promotionRepository.findById(missingId)).thenReturn(Optional.empty());

        VoucherUpdateRequest request = validUpdateRequest();

        assertThatThrownBy(() -> service.update(missingId, request, adminActor))
                .isInstanceOf(VoucherNotFoundException.class);

        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    // ----------------------------------------------------------------------
    // Req 4.3 — delete with a non-existent id throws VoucherNotFoundException
    // ----------------------------------------------------------------------

    @Test
    void delete_whenVoucherDoesNotExist_throwsVoucherNotFound() {
        Integer missingId = 9999;
        when(promotionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(missingId, adminActor))
                .isInstanceOf(VoucherNotFoundException.class);

        verify(promotionRepository, never()).delete(any(Promotion.class));
    }

    // ----------------------------------------------------------------------
    // Req 7.5 — Admin creates a system-wide voucher (branchSpecific=false) → branch=null
    // ----------------------------------------------------------------------

    @Test
    void create_whenAdminCreatesSystemWideVoucher_savesWithNullBranch() {
        VoucherCreateRequest request = validCreateRequest();
        request.setBranchSpecific(false);
        request.setBranchId(null);

        when(promotionRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion saved = inv.getArgument(0);
            saved.setPromotionId(100);
            return saved;
        });

        VoucherDetailView view = service.create(request, adminActor);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        Promotion saved = captor.getValue();

        // System-wide voucher: no branch attached (Req 7.5).
        assertThat(saved.getBranch()).isNull();
        assertThat(saved.getBranchSpecific()).isFalse();

        // Default status + used count (Req 2.1).
        assertThat(saved.getStatus()).isEqualTo(VoucherStatus.ACTIVE.name());
        assertThat(saved.getUsedCount()).isZero();

        // Detail view reflects a system-wide voucher.
        assertThat(view.getBranchId()).isNull();
        assertThat(view.isBranchSpecific()).isFalse();

        // The branch repository is never consulted for a system-wide voucher.
        verify(branchRepository, never()).findById(any());
    }

    // ----------------------------------------------------------------------
    // Fixtures
    // ----------------------------------------------------------------------

    private VoucherCreateRequest validCreateRequest() {
        VoucherCreateRequest r = new VoucherCreateRequest();
        r.setTitle("Summer Sale");
        r.setCode("SUMMER25");
        r.setDiscountType("PERCENTAGE");
        r.setDiscountValue(25);
        r.setMinOrderValue(0);
        r.setMaxUses(100);
        r.setStartDate(BASE);
        r.setEndDate(BASE.plusDays(30));
        r.setBranchSpecific(false);
        r.setBranchId(null);
        return r;
    }

    private VoucherUpdateRequest validUpdateRequest() {
        VoucherUpdateRequest u = new VoucherUpdateRequest();
        u.setTitle("Updated Title");
        u.setCode("UPDATED10");
        u.setDiscountType("FIXED_AMOUNT");
        u.setDiscountValue(10000);
        u.setMinOrderValue(0);
        u.setMaxUses(50);
        u.setStartDate(BASE);
        u.setEndDate(BASE.plusDays(15));
        u.setBranchSpecific(false);
        u.setBranchId(null);
        u.setStatus(null);
        return u;
    }
}
