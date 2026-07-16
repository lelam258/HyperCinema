package com.cinema.hyperCinema.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackCreateRequest {

    @NotNull(message = "{feedback.rating.required}")
    @Min(value = 1, message = "{feedback.rating.min}")
    @Max(value = 5, message = "{feedback.rating.max}")
    private Integer rating;

    @NotBlank(message = "{feedback.comment.required}")
    private String comment;
}
