package com.cinema.hyperCinema.dto.ui.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricCardView {

    private String key;

    private String label;

    private long value;

    private String displayValue;

    private String helperText;

    private String icon;
}
