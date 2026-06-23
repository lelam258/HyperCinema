package com.cinema.hyperCinema.dto.ui.booking;

import java.time.LocalDateTime;

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
public class ShowtimeOptionView {

    private Integer showtimeId;

    private String movieTitle;

    private String branchName;

    private String hallName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer basePrice;

    private String displayPrice;

    private boolean available;
}
