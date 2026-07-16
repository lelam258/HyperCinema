package com.cinema.hyperCinema.controller.customer;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.ui.workspace.CustomerDashboardView;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.movie.MovieService;
import com.cinema.hyperCinema.service.ui.WorkspaceUiDataService;

@Controller
@RequestMapping("/my")
public class CustomerDashboardController {

    private final WorkspaceUiDataService workspaceUiDataService;
    private final MovieService movieService;

    public CustomerDashboardController(WorkspaceUiDataService workspaceUiDataService,
                                       MovieService movieService) {
        this.workspaceUiDataService = workspaceUiDataService;
        this.movieService = movieService;
    }

    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        CustomerDashboardView dashboard = workspaceUiDataService.getCustomerDashboard(userDetails.getUser());
        List<MovieListItem> nowShowingMovies = findCustomerMovies();
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("customerName", dashboard.getCustomerName());
        model.addAttribute("email", dashboard.getEmail());
        model.addAttribute("phone", dashboard.getPhone());
        model.addAttribute("rewardPoints", dashboard.getRewardPoints());
        model.addAttribute("membershipTier", dashboard.getMembershipTier());
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
