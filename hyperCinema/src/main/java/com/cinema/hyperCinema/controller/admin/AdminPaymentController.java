package com.cinema.hyperCinema.controller.admin;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final BranchRepository branchRepository;

    @GetMapping
    public String listPayments(
            @RequestParam(name = "status", required = false, defaultValue = "All") String status,
            @RequestParam(name = "method", required = false, defaultValue = "All") String method,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "branchId", required = false) Integer branchId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        User user = userDetails.getUser();
        String roleName = user.getRole().getName();

        // Branch Managers can only filter for their own branch
        if ("BranchManager".equalsIgnoreCase(roleName) || "Branch_Manager".equalsIgnoreCase(roleName)) {
            branchId = user.getBranch() != null ? user.getBranch().getBranchId() : -1;
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "paymentId")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<Payment> paymentPage = paymentService.getPaymentHistory(user, status, method, startDate, endDate, pageable);

        // Fetch branches for filter dropdown (only relevant for Admin/Manager)
        List<Branch> branches = List.of();
        if ("Admin".equalsIgnoreCase(roleName) || "Manager".equalsIgnoreCase(roleName)) {
            branches = branchRepository.findByStatusIgnoreCaseOrderByNameAsc("Active");
        }

        model.addAttribute("payments", paymentPage);
        model.addAttribute("branches", branches);
        model.addAttribute("status", status);
        model.addAttribute("method", method);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("userRole", roleName);
        model.addAttribute("adminName", user.getFullName());
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));

        return "admin/payment/list";
    }

    @GetMapping("/{paymentId}")
    public String paymentDetail(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        User user = userDetails.getUser();
        Payment payment = paymentService.getPaymentById(paymentId, user);
        
        model.addAttribute("payment", payment);
        model.addAttribute("userRole", user.getRole().getName());
        model.addAttribute("adminName", user.getFullName());
        model.addAttribute("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        
        return "admin/payment/detail";
    }
}
