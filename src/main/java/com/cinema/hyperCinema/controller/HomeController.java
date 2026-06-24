package com.cinema.hyperCinema.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        if (hasRole(authentication, "ROLE_MANAGER")) {
            return "redirect:/manager/dashboard";
        }
        if (hasRole(authentication, "ROLE_BRANCH_MANAGER") || hasRole(authentication, "ROLE_BRANCHMANAGER")) {
            return "redirect:/branch/dashboard";
        }
        if (hasRole(authentication, "ROLE_STAFF")) {
            return "redirect:/staff/dashboard";
        }
        if (hasRole(authentication, "ROLE_CUSTOMER")) {
            return "redirect:/my/dashboard";
        }
        return "redirect:/login";
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
