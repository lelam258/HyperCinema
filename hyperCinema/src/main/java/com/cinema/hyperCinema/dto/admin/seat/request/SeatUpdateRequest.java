package com.cinema.hyperCinema.dto.admin.seat.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatUpdateRequest {

    @NotBlank(message = "Hàng ghế không được để trống")
    @Size(max = 5, message = "Hàng ghế không được vượt quá 5 ký tự")
    private String seatRow;

    @NotNull(message = "Số ghế không được để trống")
    @Min(value = 1, message = "Số ghế phải lớn hơn hoặc bằng 1")
    private Integer seatNumber;

    @NotBlank(message = "Loại ghế không được để trống")
    private String type;

    @NotBlank(message = "Trạng thái bảo trì không được để trống")
    private String maintenanceStatus;
}
