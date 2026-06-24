package com.cinema.hyperCinema.controller.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cinema.hyperCinema.config.SecurityConfig;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherDetailView;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherListItem;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.security.CustomAuthenticationSuccessHandler;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.security.guard.VoucherAccessGuard;
import com.cinema.hyperCinema.service.voucher.VoucherService;

/**
 * Integration tests (MockMvc) for {@link VoucherController}, per the design's
 * Testing Strategy → Integration Tests section.
 *
 * <p><strong>Test style:</strong> a Spring MVC slice test ({@link WebMvcTest})
 * with the production {@link SecurityConfig} imported and the service/persistence
 * collaborators replaced by Mockito mocks ({@link MockitoBean}). A slice test is
 * used deliberately so the full {@code ApplicationContext} — and the unrelated
 * {@code DataInitializer.seedHalls()} bug that breaks full-context startup — is
 * never loaded. The real Thymeleaf templates are rendered so the assertions below
 * exercise the actual admin views.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>Unauthenticated request to a voucher management route → redirect to
 *       {@code /login} (Req 7.4).</li>
 *   <li>Delete flow renders a confirmation before the DELETE is sent — the detail
 *       view wires the DELETE via {@code _method=delete} behind a confirmation
 *       modal (Req 4.4).</li>
 *   <li>An EXPIRED voucher is shown with a distinguishing visual indicator in the
 *       list (Req 6.3).</li>
 * </ul>
 */
@WebMvcTest(VoucherController.class)
@Import(SecurityConfig.class)
class VoucherControllerIT {

    private static final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VoucherService voucherService;

    @MockitoBean
    private BranchRepository branchRepository;

    // Named to match the SpEL reference @voucherAccessGuard in @PreAuthorize.
    @MockitoBean(name = "voucherAccessGuard")
    private VoucherAccessGuard voucherAccessGuard;

    // Required by SecurityConfig's constructor.
    @MockitoBean
    private CustomAuthenticationSuccessHandler successHandler;

    // ----------------------------------------------------------------------
    // Req 7.4 — an unauthenticated request is redirected to the login page.
    // ----------------------------------------------------------------------

    @Test
    void list_whenUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/vouchers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ----------------------------------------------------------------------
    // Req 4.4 — the delete flow renders a confirmation before the DELETE is sent.
    // The detail view exposes the delete action through a confirmation modal and
    // wires the request as POST + hidden _method=delete (HiddenHttpMethodFilter).
    // ----------------------------------------------------------------------

    @Test
    void detail_rendersDeleteConfirmationBeforeDelete() throws Exception {
        VoucherDetailView detail = VoucherDetailView.builder()
                .voucherId(7)
                .title("Summer Sale")
                .code("SUMMER25")
                .discountType("PERCENTAGE")
                .discountValue(25)
                .minOrderValue(0)
                .maxUses(100)
                .usedCount(0)
                .startDate(BASE)
                .endDate(BASE.plusDays(30))
                .branchSpecific(false)
                .status("ACTIVE")
                .build();

        when(voucherAccessGuard.canManage(any(Authentication.class), any())).thenReturn(true);
        when(voucherService.findById(any(), any(User.class))).thenReturn(detail);

        mockMvc.perform(get("/admin/vouchers/7").with(user(adminDetails())))
                .andExpect(status().isOk())
                // A confirmation step exists (modal) before the destructive action.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận xóa Voucher")))
                // The DELETE is wired via the hidden _method override field.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_method\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"delete\"")));
    }

    // ----------------------------------------------------------------------
    // Req 6.3 — an EXPIRED voucher is shown with a distinguishing visual indicator.
    // ----------------------------------------------------------------------

    @Test
    void list_showsDistinguishingIndicatorForExpiredVoucher() throws Exception {
        VoucherListItem expired = VoucherListItem.builder()
                .voucherId(3)
                .code("OLD2024")
                .title("Hết hạn từ lâu")
                .discountType("PERCENTAGE")
                .discountValue(10)
                .startDate(BASE.minusDays(60))
                .endDate(BASE.minusDays(30))
                .usedCount(5)
                .maxUses(100)
                .status("EXPIRED")
                .expired(true)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VoucherListItem> page = new PageImpl<>(List.of(expired), pageable, 1);

        when(voucherService.search(any(), any(), any(User.class))).thenReturn(page);
        when(branchRepository.findAll(any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/vouchers").with(user(adminDetails())))
                .andExpect(status().isOk())
                // The EXPIRED status badge distinguishes the voucher visually.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hết hạn")))
                // Expired rows are dimmed (opacity-60) to set them apart from active ones.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("opacity-60")));
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    /** Builds a CustomUserDetails for an ADMIN actor so @AuthenticationPrincipal resolves. */
    private CustomUserDetails adminDetails() {
        Role adminRole = new Role();
        adminRole.setRoleId(1);
        adminRole.setName("Admin");

        User admin = new User();
        admin.setUserId(1);
        admin.setUsername("admin");
        admin.setPasswordHash("x");
        admin.setStatus("Active");
        admin.setRole(adminRole);

        return new CustomUserDetails(admin);
    }
}
