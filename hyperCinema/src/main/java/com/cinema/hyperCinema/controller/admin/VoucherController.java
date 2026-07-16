package com.cinema.hyperCinema.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherCreateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherSearchCriteria;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherUpdateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.response.UpdateResult;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherDetailView;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherListItem;
import com.cinema.hyperCinema.exception.voucher.VoucherValidationException;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.voucher.VoucherService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản trị voucher (Promotion) cho khu vực admin tại
 * {@code /admin/vouchers}: danh sách + tìm kiếm/lọc, tạo, xem chi tiết, sửa,
 * đổi trạng thái và xóa.
 *
 * <p>Tuân theo đúng mẫu của {@code MovieController}: trích xuất người dùng đã xác
 * thực qua {@code @AuthenticationPrincipal CustomUserDetails} và truyền
 * {@code principal.getUser()} làm actor cho service; lỗi xác thực Bean Validation
 * trả lại form view; {@link VoucherValidationException} được bắt và đẩy vào
 * {@link BindingResult} (form) hoặc flash {@code errorKey} (status/delete).
 *
 * <p>Phân quyền lớp ({@code @PreAuthorize("hasAnyRole('ADMIN','MANAGER','BRANCHMANAGER')")})
 * và phạm vi chi nhánh ở route chi tiết/sửa/xóa qua
 * {@code @voucherAccessGuard.canManage(...)}. Người dùng chưa xác thực được Spring
 * Security chuyển hướng tới {@code /login} (Requirement 7.4) — không xử lý thủ công.
 */
@Controller
@RequestMapping("/admin/vouchers")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_MANAGER', 'BRANCHMANAGER')")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final BranchRepository branchRepository;

    @GetMapping
    public String list(@ModelAttribute("criteria") VoucherSearchCriteria criteria,
                       @AuthenticationPrincipal CustomUserDetails principal,
                       Model model) {

        criteria.normalize();

        Sort.Direction direction = Sort.Direction.fromString(criteria.getDirection());
        Pageable pageable = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(direction, criteria.getSort()));

        Page<VoucherListItem> page =
                voucherService.search(criteria, pageable, principal.getUser());

        model.addAttribute("page", page);
        model.addAttribute("criteria", criteria);
        prepareFormDropdowns(model);

        return "admin/vouchers/voucher-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("voucher", new VoucherCreateRequest());
        model.addAttribute("mode", "create");
        prepareFormDropdowns(model);
        return "admin/vouchers/voucher-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("voucher") VoucherCreateRequest voucher,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            prepareFormDropdowns(model);
            return "admin/vouchers/voucher-form";
        }

        try {
            VoucherDetailView created = voucherService.create(voucher, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "voucher.create.success");
            return "redirect:/admin/vouchers/" + created.getVoucherId();
        } catch (VoucherValidationException ex) {

            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "create");
            prepareFormDropdowns(model);
            return "admin/vouchers/voucher-form";
        }
    }

    @GetMapping("/{voucherId}")
    @PreAuthorize("@voucherAccessGuard.canManage(authentication, #voucherId)")
    public String detail(@PathVariable Integer voucherId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {

        VoucherDetailView voucher = voucherService.findById(voucherId, principal.getUser());
        model.addAttribute("voucher", voucher);

        return "admin/vouchers/voucher-detail";
    }

    @GetMapping("/{voucherId}/edit")
    @PreAuthorize("@voucherAccessGuard.canManage(authentication, #voucherId)")
    public String editForm(@PathVariable Integer voucherId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {

        VoucherDetailView current = voucherService.findById(voucherId, principal.getUser());

        VoucherUpdateRequest request = new VoucherUpdateRequest();
        request.setTitle(current.getTitle());
        request.setCode(current.getCode());
        request.setDiscountType(current.getDiscountType());
        request.setDiscountValue(current.getDiscountValue());
        request.setMinOrderValue(current.getMinOrderValue());
        request.setMaxUses(current.getMaxUses());
        request.setStartDate(current.getStartDate());
        request.setEndDate(current.getEndDate());
        request.setBranchSpecific(current.isBranchSpecific());
        request.setBranchId(current.getBranchId());
        request.setStatus(current.getStatus());

        model.addAttribute("voucher", request);
        model.addAttribute("voucherId", voucherId);
        model.addAttribute("mode", "edit");
        prepareFormDropdowns(model);

        return "admin/vouchers/voucher-form";
    }

    @PostMapping("/{voucherId}")
    @PreAuthorize("@voucherAccessGuard.canManage(authentication, #voucherId)")
    public String update(@PathVariable Integer voucherId,
                         @Valid @ModelAttribute("voucher") VoucherUpdateRequest voucher,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("voucherId", voucherId);
            prepareFormDropdowns(model);
            return "admin/vouchers/voucher-form";
        }

        try {
            UpdateResult result = voucherService.update(voucherId, voucher, principal.getUser());

            if (!result.isHasChanges()) {
                model.addAttribute("mode", "edit");
                model.addAttribute("voucherId", voucherId);
                model.addAttribute("infoKey", "voucher.update.no_change");
                prepareFormDropdowns(model);
                return "admin/vouchers/voucher-form";
            }

            redirectAttributes.addFlashAttribute("successKey", "voucher.update.success");
            return "redirect:/admin/vouchers/" + voucherId;
        } catch (VoucherValidationException ex) {

            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "edit");
            model.addAttribute("voucherId", voucherId);
            prepareFormDropdowns(model);
            return "admin/vouchers/voucher-form";
        }
    }

    @PostMapping("/{voucherId}/status")
    @PreAuthorize("@voucherAccessGuard.canManage(authentication, #voucherId)")
    public String changeStatus(@PathVariable Integer voucherId,
                               @RequestParam("status") String status,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {

        try {
            voucherService.changeStatus(voucherId, status, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "voucher.status.changed");
        } catch (VoucherValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/vouchers/" + voucherId;
    }

    @DeleteMapping("/{voucherId}")
    @PreAuthorize("@voucherAccessGuard.canManage(authentication, #voucherId)")
    public String delete(@PathVariable Integer voucherId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        return deleteVoucher(voucherId, principal, redirectAttributes);
    }

    @PostMapping("/{voucherId}/delete")
    @PreAuthorize("@voucherAccessGuard.canManage(authentication, #voucherId)")
    public String deletePost(@PathVariable Integer voucherId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        return deleteVoucher(voucherId, principal, redirectAttributes);
    }

    private String deleteVoucher(Integer voucherId,
                                 CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            voucherService.delete(voucherId, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "voucher.delete.success");
            return "redirect:/admin/vouchers";
        } catch (VoucherValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/vouchers/" + voucherId;
        }
    }

    private void prepareFormDropdowns(Model model) {
        model.addAttribute("branches", branchRepository.findByStatusIgnoreCase("Active", Sort.by("name")));
    }
}
