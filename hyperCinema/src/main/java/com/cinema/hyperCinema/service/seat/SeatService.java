package com.cinema.hyperCinema.service.seat;

import com.cinema.hyperCinema.dto.admin.seat.request.SeatBulkCreateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatCreateRequest;
import com.cinema.hyperCinema.dto.admin.seat.request.SeatUpdateRequest;
import com.cinema.hyperCinema.dto.admin.seat.response.BulkCreateResult;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatListItem;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatManagementContext;
import com.cinema.hyperCinema.dto.admin.seat.response.SeatMapView;
import com.cinema.hyperCinema.model.User;

public interface SeatService {

    /** Seat map cho phòng chiếu, sắp xếp theo row rồi number */
    SeatMapView getSeatMap(Integer hallId, User actor);

    /** Tạo một ghế */
    SeatListItem create(Integer hallId, SeatCreateRequest request, User actor);

    /** Tạo hàng loạt */
    BulkCreateResult bulkCreate(Integer hallId, SeatBulkCreateRequest request, User actor);

    BulkCreateResult addRow(Integer hallId, String type, User actor);

    BulkCreateResult addColumn(Integer hallId, String type, User actor);

    void insertColumnAisle(Integer hallId, Integer afterColumn, User actor);

    void insertRowAisle(Integer hallId, String afterRow, User actor);

    /** Chi tiết ghế (dùng cho form edit) */
    SeatListItem findById(Integer seatId, User actor);

    /** Cập nhật ghế */
    SeatListItem update(Integer seatId, SeatUpdateRequest request, User actor);

    /** Xóa ghế */
    void delete(Integer seatId, User actor);

    /** Toggle maintenance status */
    SeatListItem toggleMaintenance(Integer seatId, String newStatus, User actor);

    /** Management context (sidebar, hall info) */
    SeatManagementContext managementContext(Integer hallId, User actor);
}
