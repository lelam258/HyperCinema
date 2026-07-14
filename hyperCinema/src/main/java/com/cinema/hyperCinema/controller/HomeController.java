package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.service.movie.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MovieService movieService;

    @GetMapping("/")
    public String index(@RequestParam(required = false) String keyword, Authentication authentication, Model model) {
        List<MovieListItem> nowShowing = findMovies("NowShowing", keyword, 10);
        List<MovieListItem> comingSoon = findMovies("ComingSoon", keyword, 5);

        model.addAttribute("keyword", keyword);
        model.addAttribute("nowShowing", nowShowing);
        model.addAttribute("comingSoon", comingSoon);
        model.addAttribute("featuredMovie", nowShowing.isEmpty() ? null : nowShowing.get(0));
        model.addAttribute("loggedIn", isLoggedIn(authentication));
        model.addAttribute("dashboardUrl", resolveDashboardUrl(authentication));
        return "home";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ANONYMOUS"));
    }

    private String resolveDashboardUrl(Authentication authentication) {
        if (!isLoggedIn(authentication)) {
            return "/login";
        }
        return authentication.getAuthorities().stream()
                .map(authority -> switch (authority.getAuthority()) {
                    case "ROLE_ADMIN" -> "/admin/dashboard";
                    case "ROLE_MANAGER" -> "/manager/dashboard";
                    case "ROLE_BRANCH_MANAGER", "ROLE_BRANCHMANAGER" -> "/branch/dashboard";
                    case "ROLE_STAFF" -> "/staff/dashboard";
                    case "ROLE_CUSTOMER" -> "/my/dashboard";
                    default -> null;
                })
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse("/");
    }

    private List<MovieListItem> findMovies(String status, String keyword, int size) {
        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setStatus(status);
        criteria.setKeyword(keyword);
        criteria.normalize();

        return movieService.search(criteria, PageRequest.of(
                0,
                size,
                Sort.by(Sort.Direction.DESC, "releaseDate")))
                .getContent();
    }
}
