package com.cinema.hyperCinema.dto.admin.branch.request;

import com.cinema.hyperCinema.validation.BranchTimeRangeValid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@BranchTimeRangeValid
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchCreateRequest {

    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(min = 1, max = 150, message = "Tên chi nhánh phải có độ dài từ 1 đến 150 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(min = 1, max = 255, message = "Địa chỉ phải có độ dài từ 1 đến 255 ký tự")
    private String address;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(min = 1, max = 100, message = "Thành phố phải có độ dài từ 1 đến 100 ký tự")
    private String city;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9,10}$",
            message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    private String phone;

    @NotNull(message = "Giờ mở cửa không được để trống")
    private LocalTime openingTime;

    @NotNull(message = "Giờ đóng cửa không được để trống")
    private LocalTime closingTime;
}
