package com.cinema.hyperCinema.controller.customer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.ui.workspace.CustomerDashboardView;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.Payment;
import com.cinema.hyperCinema.model.Notification;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.movie.MovieService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;
import com.cinema.hyperCinema.service.PaymentService;
import com.cinema.hyperCinema.service.NotificationService;

@Controller
@RequestMapping("/my")
public class CustomerDashboardController {

    private final WorkspaceUiDataService workspaceUiDataService;
    private final MovieService movieService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public CustomerDashboardController(WorkspaceUiDataService workspaceUiDataService,
                                       MovieService movieService,
                                       PaymentService paymentService,
                                       NotificationService notificationService) {
        this.workspaceUiDataService = workspaceUiDataService;
        this.movieService = movieService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        CustomerDashboardView dashboard = workspaceUiDataService.getCustomerDashboard(userDetails.getUser());
        List<MovieListItem> nowShowingMovies = findCustomerMovies();
        addCustomerAttributes(model, dashboard);
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());
        model.addAttribute("movies", nowShowingMovies);
        model.addAttribute("nowShowingCount", nowShowingMovies.size());
        return "my/dashboard";
    }

    @GetMapping("/tai-khoan")
    public String account(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        CustomerDashboardView dashboard = workspaceUiDataService.getCustomerDashboard(userDetails.getUser());
        addCustomerAttributes(model, dashboard);
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());
        return "my/profile";
    }

    @GetMapping("/payments")
    public String paymentHistory(
            @RequestParam(name = "status", required = false, defaultValue = "All") String status,
            @RequestParam(name = "method", required = false, defaultValue = "All") String method,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        User user = userDetails.getUser();
        CustomerDashboardView dashboard = workspaceUiDataService.getCustomerDashboard(user);
        addCustomerAttributes(model, dashboard);
        
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentService.getPaymentHistory(user, status, method, null, null, pageable);

        model.addAttribute("payments", paymentPage);
        model.addAttribute("status", status);
        model.addAttribute("method", method);
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());

        return "my/payment/list";
    }

    @GetMapping("/payments/{paymentId}")
    public String paymentDetail(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        User user = userDetails.getUser();
        CustomerDashboardView dashboard = workspaceUiDataService.getCustomerDashboard(user);
        addCustomerAttributes(model, dashboard);
        
        Payment payment = paymentService.getPaymentById(paymentId, user);
        model.addAttribute("payment", payment);
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());

        return "my/payment/detail";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        CustomerDashboardView dashboard = workspaceUiDataService.getCustomerDashboard(user);
        addCustomerAttributes(model, dashboard);
        
        List<Notification> notifications = notificationService.getReceivedNotifications(user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("lastUpdated", dashboard.getLastUpdated());
        
        return "my/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> markNotificationAsRead(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User user = userDetails.getUser();
            Notification notification = notificationService.markAsRead(id, user);
            long unreadCount = notificationService.countUnreadNotifications(user);
            
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    private void addCustomerAttributes(Model model, CustomerDashboardView dashboard) {
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("customerName", dashboard.getCustomerName());
        model.addAttribute("email", dashboard.getEmail());
        model.addAttribute("phone", dashboard.getPhone());
        model.addAttribute("rewardPoints", dashboard.getRewardPoints());
        model.addAttribute("membershipTier", dashboard.getMembershipTier());
        model.addAttribute("membershipProgress", dashboard.getMembershipProgress());
    }

    private List<MovieListItem> findCustomerMovies() {
        PageRequest pageable = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt"));
        MovieSearchCriteria nowShowingCriteria = new MovieSearchCriteria();
        nowShowingCriteria.setStatus("NowShowing");
        List<MovieListItem> nowShowing = movieService.search(nowShowingCriteria, pageable).getContent();
        if (!nowShowing.isEmpty()) {
            return nowShowing;
        }

        return movieService.search(new MovieSearchCriteria(), pageable).getContent();
    }
}
