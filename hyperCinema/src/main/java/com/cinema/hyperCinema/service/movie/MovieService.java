package com.cinema.hyperCinema.service.movie;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cinema.hyperCinema.dto.admin.movie.request.MovieCreateRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieUpdateRequest;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.admin.movie.response.UpdateResult;
import com.cinema.hyperCinema.model.User;

public interface MovieService {

    MovieDetailView create(MovieCreateRequest req, User admin);

    UpdateResult update(Integer movieId, MovieUpdateRequest req, User admin);

    MovieDetailView findById(Integer movieId);

    Page<MovieListItem> search(MovieSearchCriteria criteria, Pageable pageable);

    void changeStatus(Integer movieId, String newStatus, User admin);

    void deleteHard(Integer movieId, User admin);

    void addGenre(Integer movieId, Integer genreId, User admin);

    void removeGenre(Integer movieId, Integer genreId, User admin);

    void assignBranch(Integer movieId, Integer branchId, User admin);

    void unassignBranch(Integer movieId, Integer branchId, User admin);
}

