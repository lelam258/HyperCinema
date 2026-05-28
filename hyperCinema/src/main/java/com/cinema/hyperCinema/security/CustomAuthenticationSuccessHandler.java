package com.cinema.hyperCinema.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String targetUrl = "/";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                targetUrl = "/admin/users";
                break;
            } else if (role.equals("ROLE_MANAGER")) {
                targetUrl = "/manager/branches";
                break;
            } else if (role.equals("ROLE_BRANCH_MANAGER") || role.equals("ROLE_BRANCHMANAGER")) {
                targetUrl = "/branch/halls";
                break;
            } else if (role.equals("ROLE_STAFF")) {
                targetUrl = "/staff/booking";
                break;
            } else if (role.equals("ROLE_CUSTOMER")) {
                targetUrl = "/my/dashboard";
                break;
            }
        }
        response.sendRedirect(targetUrl);
    }
}
