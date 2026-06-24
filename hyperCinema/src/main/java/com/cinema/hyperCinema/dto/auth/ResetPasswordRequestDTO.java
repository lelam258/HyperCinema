package com.cinema.hyperCinema.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    @NotBlank(message = "Ma xac nhan khong duoc de trong")
    private String code;

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Size(min = 6, message = "Mat khau phai tu 6 ky tu tro len")
    private String password;

    @NotBlank(message = "Xac nhan mat khau khong duoc de trong")
    private String confirmPassword;
}
