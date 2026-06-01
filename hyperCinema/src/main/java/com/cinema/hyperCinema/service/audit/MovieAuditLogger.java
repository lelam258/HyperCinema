package com.cinema.hyperCinema.service.audit;

import com.cinema.hyperCinema.dto.admin.movie.response.FieldChange;
import com.cinema.hyperCinema.dto.admin.movie.response.GenreChangeSet;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.User;

import java.util.List;
import java.util.Set;

/**
 * Ghi audit trail cho các mutation của module Movie.
 *
 * <p>Mỗi method được hiện thực với {@code Propagation.REQUIRES_NEW} để cô lập
 * lỗi audit khỏi business transaction (REQ 12.6). Định dạng {@code details}
 * tuân theo bảng B.7.3 của design.</p>
 */
public interface MovieAuditLogger {

    /** REQ 12.1 — snapshot Phim mới tạo kèm tập genreId. */
    void logCreate(Movie movie, Set<Integer> genreIds, User admin);

    /** REQ 12.2 — diff scalar + thay đổi tập genre khi cập nhật. */
    void logUpdate(Movie oldMovie, Movie newMovie,
                   List<FieldChange> scalarChanges, GenreChangeSet genreChanges, User admin);

    /** REQ 12.3 — chuyển trạng thái. */
    void logStatusChange(Movie movie, String oldStatus, String newStatus, User admin);

    /** REQ 12.4 — phân phối Phim tới Branch. */
    void logAssignBranch(Integer movieId, Integer branchId, User admin);

    /** REQ 12.4 — gỡ phân phối Phim khỏi Branch. */
    void logUnassignBranch(Integer movieId, Integer branchId, User admin);
}
