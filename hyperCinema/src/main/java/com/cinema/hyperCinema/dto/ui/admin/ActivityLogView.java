package com.cinema.hyperCinema.dto.ui.admin;

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
public class ActivityLogView {

    private Integer logId;

    private String action;

    private String description;

    private LocalDateTime createdAt;

    private String actorLabel;
}
