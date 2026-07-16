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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cinema.hyperCinema.dto.admin.hall.request.HallCreateRequest;
import com.cinema.hyperCinema.dto.admin.hall.request.HallSearchCriteria;
import com.cinema.hyperCinema.dto.admin.hall.request.HallUpdateRequest;
import com.cinema.hyperCinema.dto.admin.hall.response.HallDetailView;
import com.cinema.hyperCinema.dto.admin.hall.response.HallListItem;
import com.cinema.hyperCinema.dto.admin.hall.response.HallManagementContext;
import com.cinema.hyperCinema.exception.hall.HallValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.hall.HallService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/halls")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_MANAGER', 'BRANCHMANAGER')")
@RequiredArgsConstructor
public class HallManagementController {

    private final HallService hallService;

    @GetMapping
    public String list(@ModelAttribute("criteria") HallSearchCriteria criteria,
                       @AuthenticationPrincipal CustomUserDetails principal,
                       Model model) {

        criteria.normalize();
        Sort.Direction direction = Sort.Direction.fromString(criteria.getDirection());
        Pageable pageable = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(direction, criteria.getSort()));

        Page<HallListItem> page = hallService.search(criteria, pageable, principal.getUser());
        addContext(model, principal);
        model.addAttribute("page", page);
        model.addAttribute("criteria", criteria);

        return "admin/halls/hall-list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        addContext(model, principal);
        model.addAttribute("hall", new HallCreateRequest());
        model.addAttribute("mode", "create");
        return "admin/halls/hall-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("hall") HallCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            addContext(model, principal);
            model.addAttribute("mode", "create");
            return "admin/halls/hall-form";
        }

        try {
            HallDetailView created = hallService.create(request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "hall.create.success");
            return "redirect:/admin/halls/" + created.getHallId();
        } catch (HallValidationException ex) {
            bindingResult.reject(ex.getKey());
            addContext(model, principal);
            model.addAttribute("mode", "create");
            return "admin/halls/hall-form";
        }
    }

    @GetMapping("/{hallId}")
    public String detail(@PathVariable Integer hallId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {

        HallDetailView hall = hallService.findById(hallId, principal.getUser());
        addContext(model, principal);
        model.addAttribute("hall", hall);

        return "admin/halls/hall-detail";
    }

    @GetMapping("/{hallId}/edit")
    public String editForm(@PathVariable Integer hallId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {

        HallDetailView current = hallService.findById(hallId, principal.getUser());
        HallUpdateRequest request = new HallUpdateRequest();
        request.setName(current.getName());
        request.setBranchId(current.getBranchId());
        request.setHallType(current.getHallType());
        request.setTicketPrice(current.getTicketPrice());
        current.getSeatTypePrices().forEach(price -> {
            if ("STANDARD".equals(price.getSeatType())) {
                request.setStandardPrice(price.getPrice());
            } else if ("VIP".equals(price.getSeatType())) {
                request.setVipPrice(price.getPrice());
            } else if ("COUPLE".equals(price.getSeatType())) {
                request.setCouplePrice(price.getPrice());
            } else if ("DISABLED".equals(price.getSeatType())) {
                request.setDisabledPrice(price.getPrice());
            }
        });
        request.setCapacity(current.getCapacity());
        request.setStatus(current.getStatus());

        addContext(model, principal);
        model.addAttribute("hall", request);
        model.addAttribute("hallId", hallId);
        model.addAttribute("currentHall", current);
        model.addAttribute("mode", "edit");

        return "admin/halls/hall-form";
    }

    @PostMapping("/{hallId}")
    public String update(@PathVariable Integer hallId,
                         @Valid @ModelAttribute("hall") HallUpdateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            addContext(model, principal);
            model.addAttribute("hallId", hallId);
            model.addAttribute("mode", "edit");
            return "admin/halls/hall-form";
        }

        try {
            hallService.update(hallId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "hall.update.success");
            return "redirect:/admin/halls/" + hallId;
        } catch (HallValidationException ex) {
            bindingResult.reject(ex.getKey());
            addContext(model, principal);
            model.addAttribute("hallId", hallId);
            model.addAttribute("mode", "edit");
            return "admin/halls/hall-form";
        }
    }

    @DeleteMapping("/{hallId}")
    public String delete(@PathVariable Integer hallId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        try {
            hallService.delete(hallId, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "hall.delete.success");
            return "redirect:/admin/halls";
        } catch (HallValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/halls/" + hallId;
        }
    }

    private void addContext(Model model, CustomUserDetails principal) {
        HallManagementContext context = hallService.managementContext(principal.getUser());
        model.addAttribute("hallContext", context);
    }
}
