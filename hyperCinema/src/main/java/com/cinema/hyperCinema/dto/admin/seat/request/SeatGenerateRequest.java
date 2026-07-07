package com.cinema.hyperCinema.dto.admin.seat.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatGenerateRequest {

    @NotBlank(message = "Hàng bắt đầu không được để trống")
    @Pattern(regexp = "^[A-Z]$", message = "Hàng bắt đầu phải là một ký tự in hoa từ A-Z")
    private String rowStart = "A";

    @NotBlank(message = "Hàng kết thúc không được để trống")
    @Pattern(regexp = "^[A-Z]$", message = "Hàng kết thúc phải là một ký tự in hoa từ A-Z")
    private String rowEnd = "H";

    @NotNull(message = "Số ghế trên mỗi hàng không được để trống")
    @Min(value = 1, message = "Mỗi hàng phải có ít nhất 1 ghế")
    @Max(value = 30, message = "Mỗi hàng không vượt quá 30 ghế")
    private Integer seatsPerRow = 10;

    private List<String> vipRows = new ArrayList<>();

    private List<String> doubleRows = new ArrayList<>();

    private List<String> aisleRows = new ArrayList<>();

    private String aisleColumns = "";
}

