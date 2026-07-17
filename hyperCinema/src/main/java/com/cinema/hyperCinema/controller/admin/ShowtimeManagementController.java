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

import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeCreateRequest;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeSearchCriteria;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeUpdateRequest;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeDetailView;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeListItem;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeManagementContext;
import com.cinema.hyperCinema.exception.showtime.ShowtimeValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.showtime.ShowtimeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/showtimes")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_MANAGER', 'BRANCHMANAGER')")
@RequiredArgsConstructor
public class ShowtimeManagementController {

    private final ShowtimeService showtimeService;

    @GetMapping
    public String list(@ModelAttribute("criteria") ShowtimeSearchCriteria criteria,
                       @AuthenticationPrincipal CustomUserDetails principal,
                       Model model) {

        criteria.normalize();
        Sort.Direction direction = Sort.Direction.fromString(criteria.getDirection());
        Pageable pageable = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(direction, criteria.toSortProperty()));

        Page<ShowtimeListItem> page = showtimeService.search(criteria, pageable, principal.getUser());
        addContext(model, principal);
        model.addAttribute("page", page);
        model.addAttribute("criteria", criteria);
        model.addAttribute("pageSizes", ShowtimeSearchCriteria.ALLOWED_PAGE_SIZES);

        return "admin/showtimes/showtime-list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        addContext(model, principal);
        model.addAttribute("showtime", new ShowtimeCreateRequest());
        model.addAttribute("mode", "create");
        return "admin/showtimes/showtime-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("showtime") ShowtimeCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            addContext(model, principal);
            model.addAttribute("mode", "create");
            return "admin/showtimes/showtime-form";
        }

        try {
            ShowtimeDetailView created = showtimeService.create(request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "showtime.create.success");
            return "redirect:/admin/showtimes/" + created.getShowtimeId();
        } catch (ShowtimeValidationException ex) {
            bindingResult.reject(ex.getKey());
            addContext(model, principal);
            model.addAttribute("mode", "create");
            return "admin/showtimes/showtime-form";
        }
    }

    @GetMapping("/{showtimeId}")
    public String detail(@PathVariable Integer showtimeId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {

        ShowtimeDetailView showtime = showtimeService.findById(showtimeId, principal.getUser());
        addContext(model, principal);
        model.addAttribute("showtime", showtime);

        return "admin/showtimes/showtime-detail";
    }

    @GetMapping("/{showtimeId}/edit")
    public String editForm(@PathVariable Integer showtimeId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {

        ShowtimeDetailView current = showtimeService.findById(showtimeId, principal.getUser());
        ShowtimeUpdateRequest request = new ShowtimeUpdateRequest();
        request.setMovieId(current.getMovieId());
        request.setBranchId(current.getBranchId());
        request.setHallId(current.getHallId());
        request.setStartTime(current.getStartTime());
        request.setEndTime(current.getEndTime());

        addContext(model, principal);
        model.addAttribute("showtime", request);
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("currentShowtime", current);
        model.addAttribute("mode", "edit");

        return "admin/showtimes/showtime-form";
    }

    @PostMapping("/{showtimeId}")
    public String update(@PathVariable Integer showtimeId,
                         @Valid @ModelAttribute("showtime") ShowtimeUpdateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            addContext(model, principal);
            model.addAttribute("showtimeId", showtimeId);
            model.addAttribute("mode", "edit");
            return "admin/showtimes/showtime-form";
        }

        try {
            showtimeService.update(showtimeId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "showtime.update.success");
            return "redirect:/admin/showtimes/" + showtimeId;
        } catch (ShowtimeValidationException ex) {
            bindingResult.reject(ex.getKey());
            addContext(model, principal);
            model.addAttribute("showtimeId", showtimeId);
            model.addAttribute("mode", "edit");
            return "admin/showtimes/showtime-form";
        }
    }

    @PostMapping("/{showtimeId}/cancel")
    public String cancel(@PathVariable Integer showtimeId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        return delete(showtimeId, principal, redirectAttributes);
    }

    @DeleteMapping("/{showtimeId}")
    public String delete(@PathVariable Integer showtimeId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        try {
            showtimeService.delete(showtimeId, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "showtime.delete.success");
            return "redirect:/admin/showtimes";
        } catch (ShowtimeValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
            return "redirect:/admin/showtimes/" + showtimeId;
        }
    }

    private void addContext(Model model, CustomUserDetails principal) {
        ShowtimeManagementContext context = showtimeService.managementContext(principal.getUser());
        model.addAttribute("showtimeContext", context);
    }
}
