package com.cinema.hyperCinema.dto.admin.showtime.response;

import com.cinema.hyperCinema.dto.admin.hall.response.SeatTypePriceView;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeListItem {

    private Integer showtimeId;

    private Integer movieId;

    private String movieTitle;

    private Integer branchId;

    private String branchName;

    private Integer hallId;

    private String hallName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer price;

    private String priceRange;

    private List<SeatTypePriceView> seatTypePrices;

    private long bookingCount;

    private long ticketCount;

    private long reservationCount;

    private boolean past;

    private boolean canDelete;

    private boolean canEditSchedule;
}
