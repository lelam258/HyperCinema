package com.cinema.hyperCinema.dto.ui.admin;

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
public class QuickActionView {

    private String label;

    private String href;

    private String icon;

    private boolean enabled;

    private String disabledReason;
}
