package com.cinema.hyperCinema.dto.admin.seat.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatUpdateRequest {

    @NotBlank(message = "Hàng ghế không được để trống")
    @Pattern(regexp = "^[A-Z]$", message = "Hàng ghế phải là một ký tự in hoa từ A-Z")
    private String seatRow;

    @NotNull(message = "Số ghế không được để trống")
    @Min(value = 1, message = "Số ghế phải lớn hơn 0")
    @Max(value = 99, message = "Số ghế không được vượt quá 99")
    private Integer seatNumber;

    @NotBlank(message = "Loại ghế không được để trống")
    private String type; // Standard, VIP, Double
}
