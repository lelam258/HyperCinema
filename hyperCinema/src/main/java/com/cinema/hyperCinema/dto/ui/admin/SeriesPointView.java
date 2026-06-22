package com.cinema.hyperCinema.dto.ui.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesPointView {

    private String label;

    private long value;

    private String displayValue;
}
