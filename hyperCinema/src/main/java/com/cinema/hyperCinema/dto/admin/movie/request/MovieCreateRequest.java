package com.cinema.hyperCinema.dto.admin.movie.request;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieCreateRequest {

    @NotBlank
    @Size(min = 1, max = 255, message = "Tiêu đề phải có độ dài từ 1 đến 255 ký tự")
    private String title;

    @NotNull
    @Min(1)
    @Max(600)
    private Integer duration;

    @NotBlank
    @Size(min = 1, max = 5000)
    private String description;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;

    @Pattern(
            regexp = "^(ComingSoon|NowShowing|Ended)$",
            message = "{movie.status.invalid}"
    )
    private String status;

    @NotNull
    private Integer languageId;

    @Pattern(
            regexp = "^(|https?://[^\\s]{1,500})$",
            message = "{movie.poster_url.invalid}"
    )
    private String posterUrl;

    @Pattern(
            regexp = "^(|https?://[^\\s]{1,500})$",
            message = "{movie.trailer_url.invalid}"
    )
    private String trailerUrl;

    private Set<Integer> genreIds = new HashSet<>();
}
