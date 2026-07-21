package com.cinema.hyperCinema.controller.admin;

import com.cinema.hyperCinema.dto.admin.hall.response.HallDetailView;
import com.cinema.hyperCinema.dto.admin.hall.response.HallManagementContext;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatGenerateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatUpdateRequest;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatManagementContext;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatMapView;
import com.cinema.hyperCinema.exception.seat.SeatValidationException;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.hall.HallService;
import com.cinema.hyperCinema.service.seat.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/halls/{hallId}/seats")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'BRANCH_MANAGER', 'BRANCHMANAGER')")
@RequiredArgsConstructor
public class SeatManagementController {

    private final SeatService seatService;
    private final HallService hallService;

    @GetMapping
    public String seatMap(@PathVariable Integer hallId,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        SeatMapView seats = seatService.getSeatMap(hallId, principal.getUser());
        addContext(model, principal, seats);
        model.addAttribute("seats", seats);
        model.addAttribute("seatUpdate", new SeatUpdateRequest());
        model.addAttribute("seatAdd", new SeatUpdateRequest());
        return "admin/seats/seat-map";
    }

    @GetMapping("/generate")
    public String showGenerateForm(@PathVariable Integer hallId,
                                   @AuthenticationPrincipal CustomUserDetails principal,
                                   Model model) {
        HallDetailView hall = hallService.findById(hallId, principal.getUser());
        addContext(model, principal);
        
        model.addAttribute("hall", hall);
        model.addAttribute("generateRequest", new SeatGenerateRequest());
        return "admin/halls/seats-generate";
    }

    @PostMapping("/generate")
    public String generateSeats(@PathVariable Integer hallId,
                                @Valid @ModelAttribute("generateRequest") SeatGenerateRequest request,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            HallDetailView hall = hallService.findById(hallId, principal.getUser());
            addContext(model, principal);
            model.addAttribute("hall", hall);
            return "admin/halls/seats-generate";
        }

        try {
            seatService.generateSeats(hallId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.generate.success");
            return "redirect:/admin/halls/" + hallId + "/seats";
        } catch (SeatValidationException ex) {
            bindingResult.reject(ex.getKey());
            HallDetailView hall = hallService.findById(hallId, principal.getUser());
            addContext(model, principal);
            model.addAttribute("hall", hall);
            return "admin/halls/seats-generate";
        }
    }

    @PostMapping({"/update/{seatId}", "/{seatId}"})
    public String updateSeat(@PathVariable Integer hallId,
                             @PathVariable Integer seatId,
                             @ModelAttribute("seatUpdate") SeatUpdateRequest request,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        try {
            seatService.updateSeat(seatId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.update.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/{seatId}/maintenance")
    public String updateSeatMaintenance(@PathVariable Integer hallId,
                                        @PathVariable Integer seatId,
                                        @RequestParam String status,
                                        @AuthenticationPrincipal CustomUserDetails principal,
                                        RedirectAttributes redirectAttributes) {
        try {
            seatService.updateSeatMaintenance(seatId, status, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.update.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/add")
    public String addSeat(@PathVariable Integer hallId,
                          @Valid @ModelAttribute("seatAdd") SeatUpdateRequest request,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorKey", "seat.add.validation_error");
            return "redirect:/admin/halls/" + hallId + "/seats";
        }

        try {
            seatService.addSingleSeat(hallId, request, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.add.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping({"/delete/{seatId}", "/{seatId}/delete"})
    public String deleteSeat(@PathVariable Integer hallId,
                             @PathVariable Integer seatId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        try {
            seatService.deleteSeat(seatId, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.delete.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/clear")
    public String clearSeats(@PathVariable Integer hallId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        try {
            seatService.clearAllSeats(hallId, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.clear.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/layout/rows")
    public String addRow(@PathVariable Integer hallId,
                         @RequestParam String type,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes redirectAttributes) {
        try {
            seatService.addRow(hallId, type, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.add.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/layout/columns")
    public String addColumn(@PathVariable Integer hallId,
                            @RequestParam String type,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttributes) {
        try {
            seatService.addColumn(hallId, type, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.add.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/layout/column-aisles")
    public String insertColumnAisle(@PathVariable Integer hallId,
                                    @RequestParam Integer afterColumn,
                                    @AuthenticationPrincipal CustomUserDetails principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            seatService.insertColumnAisle(hallId, afterColumn, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.update.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    @PostMapping("/layout/row-aisles")
    public String insertRowAisle(@PathVariable Integer hallId,
                                 @RequestParam String afterRow,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            seatService.insertRowAisle(hallId, afterRow, principal.getUser());
            redirectAttributes.addFlashAttribute("successKey", "seat.update.success");
        } catch (SeatValidationException ex) {
            redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        }
        return "redirect:/admin/halls/" + hallId + "/seats";
    }

    private void addContext(Model model, CustomUserDetails principal) {
        HallManagementContext context = hallService.managementContext(principal.getUser());
        model.addAttribute("hallContext", context);
    }

    private void addContext(Model model, CustomUserDetails principal, SeatMapView seats) {
        HallManagementContext context = hallService.managementContext(principal.getUser());
        model.addAttribute("hallContext", context);
        model.addAttribute("seatContext", SeatManagementContext.builder()
                .admin(context.isAdmin())
                .sidebar(context.getSidebar())
                .hallId(seats.getHallId())
                .hallName(seats.getHallName())
                .branchName(seats.getBranchName())
                .build());
    }
}
