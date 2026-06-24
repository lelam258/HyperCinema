package com.cinema.hyperCinema.service.showtime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeCreateRequest;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeSearchCriteria;
import com.cinema.hyperCinema.dto.admin.showtime.request.ShowtimeUpdateRequest;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeDetailView;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeListItem;
import com.cinema.hyperCinema.dto.admin.showtime.response.ShowtimeManagementContext;
import com.cinema.hyperCinema.model.User;

public interface ShowtimeService {

    Page<ShowtimeListItem> search(ShowtimeSearchCriteria criteria, Pageable pageable, User actor);

    ShowtimeDetailView findById(Integer showtimeId, User actor);

    ShowtimeDetailView create(ShowtimeCreateRequest request, User actor);

    ShowtimeDetailView update(Integer showtimeId, ShowtimeUpdateRequest request, User actor);

    void delete(Integer showtimeId, User actor);

    ShowtimeManagementContext managementContext(User actor);
}
