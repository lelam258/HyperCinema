package com.cinema.hyperCinema.security;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.NotificationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository,
                                              NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        User user = userRepository.findByUsernameWithRole(authentication.getName()).orElse(null);
        if (user != null) {
            LocalDateTime loginTime = LocalDateTime.now();
            user.setLastLogin(loginTime);
            userRepository.save(user);
            notificationService.sendToUser(
                    user,
                    "Đăng nhập thành công",
                    "Tài khoản của bạn vừa đăng nhập thành công lúc "
                            + loginTime.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                            + ". Nếu đây không phải bạn, hãy đổi mật khẩu ngay.",
                    "Security");
        }

        String targetUrl = "/";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                targetUrl = "/admin/dashboard";
                break;
            } else if (role.equals("ROLE_MANAGER")) {
                targetUrl = "/manager/dashboard";
                break;
            } else if (role.equals("ROLE_BRANCH_MANAGER") || role.equals("ROLE_BRANCHMANAGER")) {
                targetUrl = "/branch/dashboard";
                break;
            } else if (role.equals("ROLE_STAFF")) {
                targetUrl = "/staff/dashboard";
                break;
            } else if (role.equals("ROLE_CUSTOMER")) {
                targetUrl = "/my/dashboard";
                break;
            }
        }
        response.sendRedirect(targetUrl);
    }
}
