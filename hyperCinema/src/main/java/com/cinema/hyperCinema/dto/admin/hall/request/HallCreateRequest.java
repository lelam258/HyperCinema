package com.cinema.hyperCinema.dto.admin.hall.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class HallCreateRequest {

    @NotBlank(message = "Tên phòng chiếu không được để trống")
    @Size(max = 50, message = "Tên phòng chiếu không được vượt quá 50 ký tự")
    private String name;

    private Integer branchId;

    @NotBlank(message = "Loai phong khong duoc de trong")
    @Size(max = 50, message = "Loai phong khong duoc vuot qua 50 ky tu")
    private String hallType;

    private Integer capacity;

    @NotNull(message = "So hang khong duoc de trong")
    @Min(value = 1, message = "So hang phai lon hon 0")
    @Max(value = 26, message = "So hang toi da la 26")
    private Integer rowCount;

    @NotNull(message = "So cot khong duoc de trong")
    @Min(value = 1, message = "So cot phai lon hon 0")
    @Max(value = 50, message = "So cot toi da la 50")
    private Integer columnCount;

    @NotBlank(message = "Trang thai khong duoc de trong")
    @Size(max = 50, message = "Trang thai khong duoc vuot qua 50 ky tu")
    private String status = "Active";
}
