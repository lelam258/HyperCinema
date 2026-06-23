package com.cinema.hyperCinema.controller.admin;

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

import com.cinema.hyperCinema.dto.admin.seat.request.SeatBulkCreateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatCreateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatUpdateRequest;
import com.cinema.hyperCinema.dto.admin.seat.response.BulkCreateResult;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatListItem;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatManagementContext;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatMapView;
import com.cinema.hyperCinema.exception.seat.SeatNotFoundException;
import com.cinema.hyperCinema.exception.seat.SeatValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.seat.SeatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/halls/{hallId}/seats")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_MANAGER', 'BRANCHMANAGER')")
@RequiredArgsConstructor
public class SeatManagementController {

    private final SeatService seatService;

    // ── GET "" → Seat map view ──────────────────────────────────────────────────

    @GetMapping
    public String seatMap(@PathVariable Integer hallId,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {

        SeatMapView seatMap = seatService.getSeatMap(hallId, principal.getUser());
        addContext(model, hallId, principal);
        model.addAttribute("seats", seatMap);

        return "admin/seats/seat-map";
    }

    // ── GET "/new" → Create form ────────────────────────────────────────────────

    @GetMapping("/new")
    public String newForm(@PathVariable Integer hallId,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {

        addContext(model, hallId, principal);
        model.addAttribute("seat", new SeatCreateRequest());
        model.addAttribute("mode", "create");

        return "admin/seats/seat-form";
    }

    // ── POST "" → Create seat ───────────────────────────────────────────────────

    @PostMapping
    public String create(@PathVariable Integer hallId,
                         @Valid @ModelAttribute("seat") SeatCreateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            addContext(model, hallId, principal);
            model.addAttribute("mode", "create");
            return "admin/seats/seat-form";
        }

        try {
            seatService.create(hallId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.create.success");
            return "redirect:/admin/halls/" + hallId + "/seats";
        } catch (SeatValidationException ex) {
            bindingResult.reject(ex.getKey());
            addContext(model, hallId, principal);
            model.addAttribute("mode", "create");
            return "admin/seats/seat-form";
        }
    }

    // ── GET "/bulk" → Bulk creation form ────────────────────────────────────────

    @GetMapping("/bulk")
    public String bulkForm(@PathVariable Integer hallId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {

        addContext(model, hallId, principal);
        model.addAttribute("bulkRequest", new SeatBulkCreateRequest());

        return "admin/seats/seat-bulk-form";
    }

    // ── POST "/bulk" → Bulk create seats ────────────────────────────────────────

    @PostMapping("/bulk")
    public String bulkCreate(@PathVariable Integer hallId,
                             @Valid @ModelAttribute("bulkRequest") SeatBulkCreateRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        if (bindingResult.hasErrors()) {
            addContext(model, hallId, principal);
            return "admin/seats/seat-bulk-form";
        }

        try {
            BulkCreateResult result = seatService.bulkCreate(hallId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.bulk.create.success");
            redirectAttributes.addFlashAttribute("createdCount", result.getCreatedCount());
            return "redirect:/admin/halls/" + hallId + "/seats";
        } catch (SeatValidationException ex) {
            bindingResult.reject(ex.getKey());
            addContext(model, hallId, principal);
            return "admin/seats/seat-bulk-form";
        }
    }

    // ── GET "/{seatId}/edit" → Edit form ────────────────────────────────────────

    @PostMapping("/layout/rows")
    public String addRowInline(@PathVariable Integer hallId,
                               @RequestParam(value = "type", required = false) String type,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes) {
        try {
            BulkCreateResult result = seatService.addRow(hallId, type, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.layout.row.added");
            redirectAttributes.addFlashAttribute("createdCount", result.getCreatedCount());
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/layout/columns")
    public String addColumnInline(@PathVariable Integer hallId,
                                  @RequestParam(value = "type", required = false) String type,
                                  @AuthenticationPrincipal CustomUserDetails principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            BulkCreateResult result = seatService.addColumn(hallId, type, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.layout.column.added");
            redirectAttributes.addFlashAttribute("createdCount", result.getCreatedCount());
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/layout/column-aisles")
    public String insertColumnAisleInline(@PathVariable Integer hallId,
                                          @RequestParam("afterColumn") Integer afterColumn,
                                          @AuthenticationPrincipal CustomUserDetails principal,
                                          RedirectAttributes redirectAttributes) {
        try {
            seatService.insertColumnAisle(hallId, afterColumn, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.layout.column_aisle.added");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/layout/row-aisles")
    public String insertRowAisleInline(@PathVariable Integer hallId,
                                       @RequestParam("afterRow") String afterRow,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            seatService.insertRowAisle(hallId, afterRow, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.layout.row_aisle.added");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @GetMapping("/{seatId}/edit")
    public String editForm(@PathVariable Integer hallId,
                           @PathVariable Integer seatId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {

        SeatListItem current = seatService.findById(seatId, principal.getUser());

        SeatUpdateRequest request = new SeatUpdateRequest();
        request.setSeatRow(current.getSeatRow());
        request.setSeatNumber(current.getSeatNumber());
        request.setType(current.getType());
        request.setMaintenanceStatus(current.getMaintenanceStatus());

        addContext(model, hallId, principal);
        model.addAttribute("seat", request);
        model.addAttribute("seatId", seatId);
        model.addAttribute("mode", "edit");

        return "admin/seats/seat-form";
    }

    // ── POST "/{seatId}" → Update seat ──────────────────────────────────────────

    @PostMapping("/{seatId}")
    public String update(@PathVariable Integer hallId,
                         @PathVariable Integer seatId,
                         @Valid @ModelAttribute("seat") SeatUpdateRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        if (bindingResult.hasErrors()) {
            addContext(model, hallId, principal);
            model.addAttribute("seatId", seatId);
            model.addAttribute("mode", "edit");
            return "admin/seats/seat-form";
        }

        try {
            seatService.update(seatId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.update.success");
            return "redirect:/admin/halls/" + hallId + "/seats";
        } catch (SeatValidationException ex) {
            bindingResult.reject(ex.getKey());
            addContext(model, hallId, principal);
            model.addAttribute("seatId", seatId);
            model.addAttribute("mode", "edit");
            return "admin/seats/seat-form";
        }
    }

    // ── DELETE "/{seatId}" → Delete seat ────────────────────────────────────────

    @DeleteMapping("/{seatId}")
    public String delete(@PathVariable Integer hallId,
                         @PathVariable Integer seatId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {

        try {
            seatService.delete(seatId, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.delete.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        } catch (SeatNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorKey", "seat.not_found");
        }

        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    // ── POST "/{seatId}/maintenance" → Toggle maintenance ───────────────────────

    @PostMapping("/{seatId}/delete")
    public String deletePost(@PathVariable Integer hallId,
                             @PathVariable Integer seatId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        return delete(hallId, seatId, principal, redirectAttributes);
    }

    @PostMapping("/{seatId}/maintenance")
    public String toggleMaintenance(@PathVariable Integer hallId,
                                    @PathVariable Integer seatId,
                                    @RequestParam("status") String newStatus,
                                    @AuthenticationPrincipal CustomUserDetails principal,
                                    RedirectAttributes redirectAttributes) {

        try {
            seatService.toggleMaintenance(seatId, newStatus, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.maintenance.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        } catch (SeatNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorKey", "seat.not_found");
        }

        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private void addContext(Model model, Integer hallId, CustomUserDetails principal) {
        SeatManagementContext context = seatService.managementContext(hallId, principal.getUser());
        model.addAttribute("seatContext", context);
    }
}
