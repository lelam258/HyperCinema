package com.cinema.hyperCinema.controller;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.repository.GenreRepository;
import com.cinema.hyperCinema.repository.LanguageRepository;
import com.cinema.hyperCinema.service.movie.MovieService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieCustomerController {

    private final MovieService movieService;
    private final LanguageRepository languageRepository;
    private final GenreRepository genreRepository;

    @GetMapping
    public String list(@ModelAttribute("criteria") MovieSearchCriteria criteria, Model model) {
        criteria.normalize();

        // For public customer view, default to showing NowShowing movies if no status is specified
        if (criteria.getStatus() == null || criteria.getStatus().isBlank()) {
            criteria.setStatus("NowShowing");
        }

        Sort.Direction direction = Sort.Direction.fromString(criteria.getDirection());
        String sortField = mapSortField(criteria.getSort());
        Pageable pageable = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(direction, sortField));

        Page<MovieListItem> page = movieService.search(criteria, pageable);
        model.addAttribute("page", page);
        model.addAttribute("criteria", criteria);
        
        model.addAttribute("languages", languageRepository.findAll(Sort.by("name")));
        model.addAttribute("genres", genreRepository.findAllByOrderByNameAsc());

        return "customer/movies/list";
    }

    @GetMapping("/{movieId}")
    public String detail(@PathVariable Integer movieId, Model model) {
        MovieDetailView movie = movieService.findById(movieId);
        model.addAttribute("movie", movie);
        return "customer/movies/detail";
    }

    private String mapSortField(String sort) {
        return switch (sort) {
            case "release_date", "releaseDate" -> "releaseDate";
            case "created_at", "createdAt" -> "createdAt";
            default -> sort;
        };
    }
}
