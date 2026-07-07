package com.cinema.hyperCinema.dto.admin.hall.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "Suc chua khong duoc de trong")
    @Positive(message = "Suc chua phai lon hon 0")
    private Integer capacity;

    @NotBlank(message = "Trang thai khong duoc de trong")
    @Size(max = 50, message = "Trang thai khong duoc vuot qua 50 ky tu")
    private String status = "Active";
}
