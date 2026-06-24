package com.cinema.hyperCinema.dto.admin.movie.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieStatusChangeRequest {

    @NotBlank
    @Pattern(
            regexp = "^(ComingSoon|NowShowing|Ended)$",
            message = "{movie.status.invalid}"
    )
    private String status;
}
