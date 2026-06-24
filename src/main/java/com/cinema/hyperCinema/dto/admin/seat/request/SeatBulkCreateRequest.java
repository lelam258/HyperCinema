package com.cinema.hyperCinema.dto.admin.seat.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatBulkCreateRequest {

    @NotEmpty(message = "Danh sách hàng ghế không được để trống")
    @Valid
    private List<RowSpec> rows;

    private String type;

    private String maintenanceStatus;

    private String columnAislesAfter;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowSpec {

        @NotBlank(message = "Nhãn hàng không được để trống")
        @Size(max = 5, message = "Nhãn hàng không được vượt quá 5 ký tự")
        private String rowLabel;

        @NotNull(message = "Số lượng ghế không được để trống")
        @Min(value = 1, message = "Số lượng ghế phải lớn hơn hoặc bằng 1")
        private Integer seatCount;
    }
}
