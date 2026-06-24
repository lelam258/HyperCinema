package com.cinema.hyperCinema.dto.ui.booking;

import lombok.*;

import java.time.LocalDateTime;

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
