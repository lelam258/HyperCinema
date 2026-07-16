package com.cinema.hyperCinema.controller.admin;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.branch.request.AssignStaffRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.AssignManagerRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchCreateRequest;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchDetailView;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchListItem;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchSearchCriteria;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchStatusChangeRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchUpdateRequest;
import com.cinema.hyperCinema.dto.admin.branch.response.UpdateResult;
import com.cinema.hyperCinema.exception.branch.BranchValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.branch.BranchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/branches")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BranchController {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("Active", "Inactive", "Maintenance");

    private static final String WARNING_KEY_STATUS_IGNORED =
            "branch.search.status_ignored";

    private static final String ROLE_ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final BranchService branchService;

    @GetMapping
    public String list(@ModelAttribute("criteria") BranchSearchCriteria criteria,
                       Model model) {

        criteria.normalize();

        String rawStatus = criteria.getStatus();
        if (rawStatus != null && !rawStatus.isBlank()
                && !ALLOWED_STATUSES.contains(rawStatus)) {
            criteria.setStatus(null);
            model.addAttribute("warningKey", WARNING_KEY_STATUS_IGNORED);
        }

        Sort.Direction direction = Sort.Direction.fromString(criteria.getDirection());
        Pageable pageable = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(direction, criteria.getSort()));

        Page<BranchListItem> page = branchService.search(criteria, pageable);
        model.addAttribute("page", page);
        model.addAttribute("criteria", criteria);

        return "admin/branches/branch-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("branch", new BranchCreateRequest());
        model.addAttribute("mode", "create");
        return "admin/branches/branch-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("branch") BranchCreateRequest branch,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "admin/branches/branch-form";
        }

        try {
            BranchDetailView created =
                    branchService.create(branch, principal.getUser());

            redirectAttributes.addFlashAttribute(
                    "successKey", "branch.create.success");
            return "redirect:/admin/branches/" + created.getBranchId();
        } catch (BranchValidationException ex) {

            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "create");
            return "admin/branches/branch-form";
        }
    }

    @GetMapping("/{branchId}")
    @PreAuthorize("hasRole('ADMIN') "
            + "or (hasRole('MANAGER') "
            + "and @branchAccessGuard.canRead(authentication, #branchId))")
    public String detail(@PathVariable Integer branchId,
                         Authentication authentication,
                         Model model) {

        BranchDetailView branch = branchService.findById(branchId);

        boolean isAdmin = hasAuthority(authentication, ROLE_ADMIN_AUTHORITY);
        boolean readOnly = !isAdmin;

        model.addAttribute("branch", branch);
        model.addAttribute("readOnly", readOnly);
        if (isAdmin) {
            model.addAttribute("staffCandidates", branchService.listUnassignedStaff());
            model.addAttribute("managerCandidates", branch.getManagers());
            model.addAttribute("assignStaffRequest", new AssignStaffRequest());
        }

        return "admin/branches/branch-detail";
    }

    @GetMapping("/{branchId}/edit")
    public String editForm(@PathVariable Integer branchId, Model model) {

        BranchDetailView current = branchService.findById(branchId);

        BranchUpdateRequest request = new BranchUpdateRequest();
        request.setName(current.getName());
        request.setAddress(current.getAddress());
        request.setCity(current.getCity());
        request.setPhone(current.getPhone());
        request.setOpeningTime(current.getOpeningTime());
        request.setClosingTime(current.getClosingTime());

        model.addAttribute("branch", request);
        model.addAttribute("branchId", branchId);
        model.addAttribute("mode", "edit");

        return "admin/branches/branch-form";
    }

    @PostMapping("/{branchId}")
    public String update(@PathVariable Integer branchId,
                         @Valid @ModelAttribute("branch") BranchUpdateRequest branch,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("branchId", branchId);
            return "admin/branches/branch-form";
        }

        try {
            UpdateResult result =
                    branchService.update(branchId, branch, principal.getUser());

            if (!result.isHasChanges()) {
                model.addAttribute("mode", "edit");
                model.addAttribute("branchId", branchId);
                model.addAttribute("infoKey", "branch.update.no_change");
                return "admin/branches/branch-form";
            }

            redirectAttributes.addFlashAttribute(
                    "successKey", "branch.update.success");
            return "redirect:/admin/branches/" + branchId;
        } catch (BranchValidationException ex) {

            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "edit");
            model.addAttribute("branchId", branchId);
            return "admin/branches/branch-form";
        }
    }

    @PostMapping("/{branchId}/status")
    public String changeStatus(@PathVariable Integer branchId,
                               @Valid @ModelAttribute("statusRequest") BranchStatusChangeRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorKey", "branch.status.invalid");
            return "redirect:/admin/branches/" + branchId;
        }

        try {
            branchService.changeStatus(branchId, request.getStatus(), principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "branch.status.changed");
        } catch (BranchValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/branches/" + branchId;
    }

    @GetMapping("/{branchId}/managers/assign")
    public String assignManagerForm(@PathVariable Integer branchId, Model model) {

        BranchDetailView branch = branchService.findById(branchId);

        model.addAttribute("branch", branch);
        model.addAttribute("candidates", branchService.listUnassignedManagers());

        model.addAttribute("assignRequest", new AssignManagerRequest());

        return "admin/branches/assign-manager";
    }

    @PostMapping("/{branchId}/managers")
    public String assignManager(@PathVariable Integer branchId,
                                @Valid @ModelAttribute("assignRequest") AssignManagerRequest request,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorKey", "branch.assign_manager.role_invalid");
            return "redirect:/admin/branches/" + branchId + "/managers/assign";
        }

        try {
            branchService.assignManager(branchId, request.getUserId(), principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "branch.manager.assigned");
            return "redirect:/admin/branches/" + branchId;
        } catch (BranchValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/branches/" + branchId + "/managers/assign";
        }
    }

    @GetMapping("/{branchId}/staff/assign")
    public String assignStaffForm(@PathVariable Integer branchId, Model model) {

        BranchDetailView branch = branchService.findById(branchId);

        model.addAttribute("branch", branch);
        model.addAttribute("staffCandidates", branchService.listUnassignedStaff());
        model.addAttribute("managerCandidates", branch.getManagers());
        model.addAttribute("assignStaffRequest", new AssignStaffRequest());

        return "admin/branches/assign-staff";
    }

    @PostMapping("/{branchId}/managers/{userId}/unassign")
    public String unassignManager(@PathVariable Integer branchId,
                                  @PathVariable Integer userId,
                                  @AuthenticationPrincipal CustomUserDetails principal,
                                  RedirectAttributes redirectAttributes) {

        try {
            branchService.unassignManager(branchId, userId, principal.getUser());

            redirectAttributes.addFlashAttribute("successKey", "branch.manager.unassigned");
        } catch (BranchValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/branches/" + branchId;
    }

    @PostMapping("/{branchId}/staff")
    public String assignStaff(@PathVariable Integer branchId,
                              @Valid @ModelAttribute("assignStaffRequest") AssignStaffRequest request,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorKey", "branch.assign_staff.role_invalid");
            return "redirect:/admin/branches/" + branchId + "/staff/assign";
        }

        try {
            branchService.assignStaff(branchId, request.getUserId(),
                    request.getManagerId(), principal.getUser());

            redirectAttributes.addFlashAttribute(
                    "successKey", "branch.staff.assigned");
            return "redirect:/admin/branches/" + branchId;
        } catch (BranchValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/branches/" + branchId + "/staff/assign";
        }
    }

    @PostMapping("/{branchId}/staff/{userId}/unassign")
    public String unassignStaff(@PathVariable Integer branchId,
                                @PathVariable Integer userId,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                RedirectAttributes redirectAttributes) {

        try {
            branchService.unassignStaff(branchId, userId, principal.getUser());

            redirectAttributes.addFlashAttribute(
                    "successKey", "branch.staff.unassigned");
        } catch (BranchValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/branches/" + branchId;
    }

    @DeleteMapping("/{branchId}")
    public String deleteHard(@PathVariable Integer branchId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {

        try {
            branchService.deleteHard(branchId, principal.getUser());

            redirectAttributes.addFlashAttribute(
                    "successKey", "branch.delete.success");
            return "redirect:/admin/branches";
        } catch (BranchValidationException ex) {

            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/branches/" + branchId;
        }
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
