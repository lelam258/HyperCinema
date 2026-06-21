package com.cinema.hyperCinema.service.hall;

import com.cinema.hyperCinema.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cinema.hyperCinema.dto.admin.hall.request.HallCreateRequest;
import com.cinema.hyperCinema.dto.admin.hall.request.HallSearchCriteria;
import com.cinema.hyperCinema.dto.admin.hall.request.HallUpdateRequest;
import com.cinema.hyperCinema.dto.admin.hall.response.HallDetailView;
import com.cinema.hyperCinema.dto.admin.hall.response.HallListItem;
import com.cinema.hyperCinema.dto.admin.hall.response.HallManagementContext;

public interface HallService {

    Page<HallListItem> search(HallSearchCriteria criteria, Pageable pageable, User actor);

    HallDetailView findById(Integer hallId, User actor);

    HallDetailView create(HallCreateRequest request, User actor);

    HallDetailView update(Integer hallId, HallUpdateRequest request, User actor);

    void delete(Integer hallId, User actor);

    HallManagementContext managementContext(User actor);
}
