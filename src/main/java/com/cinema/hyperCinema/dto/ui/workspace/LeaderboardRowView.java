package com.cinema.hyperCinema.dto.ui.workspace;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardRowView {

    private String label;

    private long value;

    private String displayValue;
}
