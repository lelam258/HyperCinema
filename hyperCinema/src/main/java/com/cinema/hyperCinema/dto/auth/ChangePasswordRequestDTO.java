package com.cinema.hyperCinema.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequestDTO {

    @NotBlank(message = "Mat khau hien tai khong duoc de trong")
    private String currentPassword;

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Size(min = PasswordPolicy.MIN_LENGTH, message = PasswordPolicy.MIN_LENGTH_MESSAGE)
    private String newPassword;

    @NotBlank(message = "Xac nhan mat khau khong duoc de trong")
    private String confirmPassword;
}
