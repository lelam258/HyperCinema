package com.cinema.hyperCinema.dto.ui.workspace;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceActionView {

    private String label;

    private String href;

    private String icon;

    private boolean enabled;

    private String disabledReason;
}
