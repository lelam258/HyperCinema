package com.cinema.hyperCinema.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.hyperCinema.dto.auth.ChangePasswordRequestDTO;
import com.cinema.hyperCinema.dto.auth.ResetPasswordRequestDTO;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.RoleRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.EmailService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmailService emailService = mock(EmailService.class);
    private final AuthServiceImpl service = new AuthServiceImpl(
            userRepository,
            roleRepository,
            passwordEncoder,
            emailService);

    @Test
    void changePasswordUpdatesEncodedPasswordWhenCurrentPasswordIsCorrect() {
        User user = userWithPassword("old-pass");
        ChangePasswordRequestDTO dto = changeRequest("old-pass", "new-pass", "new-pass");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        service.changePassword(user, dto);

        assertThat(passwordEncoder.matches("new-pass", user.getPasswordHash())).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = userWithPassword("old-pass");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(user, changeRequest("bad-pass", "new-pass", "new-pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hien tai");

        assertThat(passwordEncoder.matches("old-pass", user.getPasswordHash())).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePasswordRejectsMismatchedWeakAndUnchangedPasswords() {
        User user = userWithPassword("old-pass");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(user, changeRequest("old-pass", "new-pass", "other-pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong khop");
        assertThatThrownBy(() -> service.changePassword(user, changeRequest("old-pass", "123", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6");
        assertThatThrownBy(() -> service.changePassword(user, changeRequest("old-pass", "old-pass", "old-pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khac");

        assertThat(passwordEncoder.matches("old-pass", user.getPasswordHash())).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void requestForgotPasswordGeneratesFreshCodeAndExpiration() {
        User user = userWithPassword("old-pass");
        user.setEmail("member@example.com");
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        service.requestForgotPassword(" member@example.com ");

        assertThat(user.getForgotPasswordCode()).isNotBlank();
        assertThat(user.getForgotPasswordCodeExpire()).isAfter(LocalDateTime.now());
        verify(userRepository).save(user);
        verify(emailService).sendForgotPasswordCode("member@example.com", user.getForgotPasswordCode());
    }

    @Test
    void requestForgotPasswordForUnknownEmailDoesNotGenerateCode() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.requestForgotPassword("missing@example.com");

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendForgotPasswordCode(any(), any());
    }

    @Test
    void resetPasswordUpdatesPasswordAndClearsCode() {
        User user = userWithPassword("old-pass");
        user.setEmail("member@example.com");
        user.setForgotPasswordCode("123456");
        user.setForgotPasswordCodeExpire(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        service.resetPassword(resetRequest("member@example.com", "123456", "new-pass", "new-pass"));

        assertThat(passwordEncoder.matches("new-pass", user.getPasswordHash())).isTrue();
        assertThat(user.getForgotPasswordCode()).isNull();
        assertThat(user.getForgotPasswordCodeExpire()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void resetPasswordRejectsInvalidOrExpiredCodeWithoutChangingPassword() {
        User user = userWithPassword("old-pass");
        user.setEmail("member@example.com");
        user.setForgotPasswordCode("123456");
        user.setForgotPasswordCodeExpire(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resetPassword(resetRequest("member@example.com", "123456", "new-pass", "new-pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("het han");

        user.setForgotPasswordCodeExpire(LocalDateTime.now().plusMinutes(5));
        assertThatThrownBy(() -> service.resetPassword(resetRequest("member@example.com", "000000", "new-pass", "new-pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong dung");

        assertThat(passwordEncoder.matches("old-pass", user.getPasswordHash())).isTrue();
    }

    private User userWithPassword(String rawPassword) {
        User user = new User();
        user.setUserId(7);
        user.setUsername("member");
        user.setEmail("member@example.com");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    private static ChangePasswordRequestDTO changeRequest(String currentPassword, String newPassword, String confirmPassword) {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO();
        dto.setCurrentPassword(currentPassword);
        dto.setNewPassword(newPassword);
        dto.setConfirmPassword(confirmPassword);
        return dto;
    }

    private static ResetPasswordRequestDTO resetRequest(String email, String code, String password, String confirmPassword) {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setEmail(email);
        dto.setCode(code);
        dto.setPassword(password);
        dto.setConfirmPassword(confirmPassword);
        return dto;
    }
}
