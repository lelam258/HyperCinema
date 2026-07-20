package com.cinema.hyperCinema.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cinema.hyperCinema.dto.auth.ForgotPasswordRequestDTO;
import com.cinema.hyperCinema.dto.auth.ResetPasswordRequestDTO;
import com.cinema.hyperCinema.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

class AuthControllerTest {

    @Test
    void forgotPasswordShowsSafeSuccessMessage() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setEmail("member@example.com");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.processForgotPassword(
                dto,
                new BeanPropertyBindingResult(dto, "forgotPasswordDTO"),
                model);

        assertThat(view).isEqualTo("auth/forgot-password");
        assertThat(model.get("successMessage").toString()).contains("Neu email ton tai");
        verify(authService).requestForgotPassword("member@example.com");
    }

    @Test
    void resetPasswordRedirectsToLoginOnSuccess() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        ResetPasswordRequestDTO dto = resetRequest();

        String view = controller.processResetPassword(
                dto,
                new BeanPropertyBindingResult(dto, "resetPasswordDTO"),
                new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login?resetSuccess=true");
        verify(authService).resetPassword(dto);
    }

    @Test
    void resetPasswordReturnsFormWhenServiceRejectsCode() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        ResetPasswordRequestDTO dto = resetRequest();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Ma xac nhan khong dung hoac da het han"))
                .when(authService).resetPassword(dto);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.processResetPassword(
                dto,
                new BeanPropertyBindingResult(dto, "resetPasswordDTO"),
                model);

        assertThat(view).isEqualTo("auth/reset-password");
        assertThat(model.get("errorMessage")).isEqualTo("Ma xac nhan khong dung hoac da het han");
    }

    private static ResetPasswordRequestDTO resetRequest() {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setEmail("member@example.com");
        dto.setCode("123456");
        dto.setPassword("new-pass");
        dto.setConfirmPassword("new-pass");
        return dto;
    }
}
