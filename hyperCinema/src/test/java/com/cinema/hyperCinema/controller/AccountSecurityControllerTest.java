package com.cinema.hyperCinema.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.hyperCinema.dto.auth.ChangePasswordRequestDTO;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class AccountSecurityControllerTest {

    @Test
    void showChangePasswordAddsForm() {
        AuthService authService = mock(AuthService.class);
        AccountSecurityController controller = new AccountSecurityController(authService);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.showChangePassword(model);

        assertThat(view).isEqualTo("account/change-password");
        assertThat(model.get("changePasswordDTO")).isInstanceOf(ChangePasswordRequestDTO.class);
    }

    @Test
    void changePasswordReturnsFormWhenBindingErrors() {
        AuthService authService = mock(AuthService.class);
        AccountSecurityController controller = new AccountSecurityController(authService);
        ChangePasswordRequestDTO dto = request();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(dto, "changePasswordDTO");
        result.rejectValue("newPassword", "weak");

        String view = controller.changePassword(dto, result, details(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("account/change-password");
        verifyNoInteractions(authService);
    }

    @Test
    void changePasswordRedirectsWithFlashOnSuccess() {
        AuthService authService = mock(AuthService.class);
        AccountSecurityController controller = new AccountSecurityController(authService);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.changePassword(
                request(),
                new BeanPropertyBindingResult(request(), "changePasswordDTO"),
                details(),
                redirectAttributes);

        assertThat(view).isEqualTo("redirect:/account/change-password");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
        verify(authService).changePassword(any(User.class), any(ChangePasswordRequestDTO.class));
    }

    @Test
    void changePasswordReturnsFormWhenServiceRejectsRequest() {
        AuthService authService = mock(AuthService.class);
        AccountSecurityController controller = new AccountSecurityController(authService);
        ChangePasswordRequestDTO dto = request();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(dto, "changePasswordDTO");
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Mat khau hien tai khong dung"))
                .when(authService).changePassword(any(User.class), any(ChangePasswordRequestDTO.class));

        String view = controller.changePassword(dto, result, details(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("account/change-password");
        assertThat(result.getGlobalError()).isNotNull();
    }

    private static ChangePasswordRequestDTO request() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO();
        dto.setCurrentPassword("old-pass");
        dto.setNewPassword("new-pass");
        dto.setConfirmPassword("new-pass");
        return dto;
    }

    private static CustomUserDetails details() {
        Role role = new Role();
        role.setName("Customer");
        User user = new User();
        user.setUserId(7);
        user.setUsername("member");
        user.setPasswordHash("x");
        user.setStatus("Active");
        user.setRole(role);
        return new CustomUserDetails(user);
    }
}
