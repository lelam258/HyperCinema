package com.cinema.hyperCinema.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (role.equals("ROLE_MANAGER")) {
                return "redirect:/manager/dashboard";
            } else if (role.equals("ROLE_BRANCH_MANAGER") || role.equals("ROLE_BRANCHMANAGER")) {
                return "redirect:/branch/dashboard";
            } else if (role.equals("ROLE_STAFF")) {
                return "redirect:/staff/dashboard";
            } else if (role.equals("ROLE_CUSTOMER")) {
                return "redirect:/my/dashboard";
            }
        }

        return "redirect:/login";
    }
}
