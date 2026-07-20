package com.cinema.hyperCinema.controller.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanCreateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanSearchCriteria;
import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanUpdateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipCreateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipSearchCriteria;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipUpdateRequest;
import com.cinema.hyperCinema.dto.admin.membership.response.MembershipPlanDetailView;
import com.cinema.hyperCinema.dto.admin.membership.response.UserMembershipDetailView;
import com.cinema.hyperCinema.exception.membership.MembershipException;
import com.cinema.hyperCinema.exception.membership.MembershipValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.membership.MembershipService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/memberships")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping
    public String index() {
        return "redirect:/admin/memberships/plans";
    }

    @GetMapping("/plans")
    public String listPlans(@ModelAttribute("criteria") MembershipPlanSearchCriteria criteria,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            Model model) {
        criteria.normalize();
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(),
                Sort.by(Sort.Direction.fromString(criteria.getDirection()), criteria.getSort()));
        model.addAttribute("page", membershipService.searchPlans(criteria, pageable, principal.getUser()));
        model.addAttribute("criteria", criteria);
        return "admin/memberships/plan-list";
    }

    @GetMapping("/plans/new")
    public String newPlanForm(@RequestParam(required = false) Integer beforeId,
                              @RequestParam(required = false) Integer afterId,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        try {
            MembershipPlanCreateRequest request = new MembershipPlanCreateRequest();
            request.setLevel(membershipService.suggestedPlanLevel(beforeId, afterId, principal.getUser()));
            model.addAttribute("plan", request);
        } catch (MembershipException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/memberships/plans";
        }
        model.addAttribute("mode", "create");
        return "admin/memberships/plan-form";
    }

    @PostMapping("/plans")
    public String createPlan(@Valid @ModelAttribute("plan") MembershipPlanCreateRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "admin/memberships/plan-form";
        }
        try {
            MembershipPlanDetailView created = membershipService.createPlan(request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "membership.plan.create.success");
            return "redirect:/admin/memberships/plans/" + created.getPlanId();
        } catch (MembershipValidationException ex) {
            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "create");
            return "admin/memberships/plan-form";
        }
    }

    @GetMapping("/plans/{planId}")
    public String planDetail(@PathVariable Integer planId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        try {
            model.addAttribute("plan", membershipService.findPlan(planId, principal.getUser()));
            return "admin/memberships/plan-detail";
        } catch (MembershipException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/memberships/plans";
        }
    }

    @GetMapping("/plans/{planId}/edit")
    public String editPlanForm(@PathVariable Integer planId,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            MembershipPlanDetailView current = membershipService.findPlan(planId, principal.getUser());
            MembershipPlanUpdateRequest request = new MembershipPlanUpdateRequest();
            request.setName(current.getName());
            request.setDiscountPercent(current.getDiscountPercent());
            request.setPrice(current.getPrice());
            request.setLevel(current.getLevel());
            request.setStatus(current.getStatus());
            model.addAttribute("plan", request);
            model.addAttribute("planId", planId);
            model.addAttribute("mode", "edit");
            return "admin/memberships/plan-form";
        } catch (MembershipException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/memberships/plans";
        }
    }

    @PostMapping("/plans/{planId}")
    public String updatePlan(@PathVariable Integer planId,
                             @Valid @ModelAttribute("plan") MembershipPlanUpdateRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("planId", planId);
            return "admin/memberships/plan-form";
        }
        try {
            if (!membershipService.updatePlan(planId, request, principal.getUser()).isHasChanges()) {
                model.addAttribute("mode", "edit");
                model.addAttribute("planId", planId);
                model.addAttribute("infoKey", "membership.update.no_change");
                return "admin/memberships/plan-form";
            }
            redirectAttributes.addFlashAttribute("successKey", "membership.plan.update.success");
            return "redirect:/admin/memberships/plans/" + planId;
        } catch (MembershipValidationException ex) {
            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "edit");
            model.addAttribute("planId", planId);
            return "admin/memberships/plan-form";
        }
    }

    @PostMapping("/plans/{planId}/delete")
    public String deactivatePlan(@PathVariable Integer planId,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            membershipService.deactivatePlan(planId, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "membership.plan.delete.success");
        } catch (MembershipException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/memberships/plans";
    }

    @GetMapping("/users")
    public String listUserMemberships(@ModelAttribute("criteria") UserMembershipSearchCriteria criteria,
                                      @AuthenticationPrincipal CustomUserDetails principal,
                                      Model model) {
        criteria.normalize();
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(),
                Sort.by(Sort.Direction.fromString(criteria.getDirection()), criteria.getSort()));
        model.addAttribute("page", membershipService.searchUserMemberships(criteria, pageable, principal.getUser()));
        model.addAttribute("criteria", criteria);
        model.addAttribute("plans", membershipService.allPlanOptions());
        return "admin/memberships/user-list";
    }

    @GetMapping("/users/new")
    public String newUserMembershipForm(Model model) {
        model.addAttribute("membership", new UserMembershipCreateRequest());
        model.addAttribute("mode", "create");
        prepareUserMembershipForm(model);
        return "admin/memberships/user-form";
    }

    @PostMapping("/users")
    public String createUserMembership(@Valid @ModelAttribute("membership") UserMembershipCreateRequest request,
                                       BindingResult bindingResult,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            prepareUserMembershipForm(model);
            return "admin/memberships/user-form";
        }
        try {
            UserMembershipDetailView created = membershipService.createUserMembership(request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "membership.user.create.success");
            return "redirect:/admin/memberships/users/" + created.getMembershipId();
        } catch (MembershipValidationException ex) {
            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "create");
            prepareUserMembershipForm(model);
            return "admin/memberships/user-form";
        }
    }

    @GetMapping("/users/{membershipId}")
    public String userMembershipDetail(@PathVariable Integer membershipId,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        try {
            model.addAttribute("membership", membershipService.findUserMembership(membershipId, principal.getUser()));
            return "admin/memberships/user-detail";
        } catch (MembershipException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/memberships/users";
        }
    }

    @GetMapping("/users/{membershipId}/edit")
    public String editUserMembershipForm(@PathVariable Integer membershipId,
                                         @AuthenticationPrincipal CustomUserDetails principal,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        try {
            UserMembershipDetailView current = membershipService.findUserMembership(membershipId, principal.getUser());
            UserMembershipUpdateRequest request = new UserMembershipUpdateRequest();
            request.setPlanId(current.getPlanId());
            request.setStatus(current.getStatus());
            model.addAttribute("membership", request);
            model.addAttribute("membershipId", membershipId);
            model.addAttribute("current", current);
            model.addAttribute("mode", "edit");
            prepareUserMembershipForm(model);
            return "admin/memberships/user-form";
        } catch (MembershipException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/memberships/users";
        }
    }

    @PostMapping("/users/{membershipId}")
    public String updateUserMembership(@PathVariable Integer membershipId,
                                       @Valid @ModelAttribute("membership") UserMembershipUpdateRequest request,
                                       BindingResult bindingResult,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("membershipId", membershipId);
            prepareUserMembershipForm(model);
            return "admin/memberships/user-form";
        }
        try {
            if (!membershipService.updateUserMembership(membershipId, request, principal.getUser()).isHasChanges()) {
                model.addAttribute("mode", "edit");
                model.addAttribute("membershipId", membershipId);
                model.addAttribute("infoKey", "membership.update.no_change");
                prepareUserMembershipForm(model);
                return "admin/memberships/user-form";
            }
            redirectAttributes.addFlashAttribute("successKey", "membership.user.update.success");
            return "redirect:/admin/memberships/users/" + membershipId;
        } catch (MembershipValidationException ex) {
            bindingResult.reject(ex.getKey());
            model.addAttribute("mode", "edit");
            model.addAttribute("membershipId", membershipId);
            prepareUserMembershipForm(model);
            return "admin/memberships/user-form";
        }
    }

    @PostMapping("/users/{membershipId}/cancel")
    public String cancelUserMembership(@PathVariable Integer membershipId,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            membershipService.cancelUserMembership(membershipId, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "membership.user.cancel.success");
        } catch (MembershipException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/memberships/users";
    }

    private void prepareUserMembershipForm(Model model) {
        model.addAttribute("plans", membershipService.activePlanOptions());
        model.addAttribute("customers", membershipService.customerOptions(null));
    }
}
