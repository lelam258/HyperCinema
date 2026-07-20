package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.dto.auth.ChangePasswordRequestDTO;
import com.cinema.hyperCinema.security.CustomUserDetails;
import com.cinema.hyperCinema.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountSecurityController {

    private final AuthService authService;

    public AccountSecurityController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/change-password")
    public String showChangePassword(Model model) {
        if (!model.containsAttribute("changePasswordDTO")) {
            model.addAttribute("changePasswordDTO", new ChangePasswordRequestDTO());
        }
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDTO") ChangePasswordRequestDTO dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }

        try {
            authService.changePassword(userDetails.getUser(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Mat khau da duoc cap nhat.");
            return "redirect:/account/change-password";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("password.change.invalid", e.getMessage());
            return "account/change-password";
        }
    }
}
