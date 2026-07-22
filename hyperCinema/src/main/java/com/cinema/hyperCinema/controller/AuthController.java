package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.dto.auth.ForgotPasswordRequestDTO;
import com.cinema.hyperCinema.dto.auth.RegisterRequestDTO;
import com.cinema.hyperCinema.dto.auth.ResetPasswordRequestDTO;
import com.cinema.hyperCinema.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) {
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        boolean bookingRequired = savedRequest != null
                && savedRequest.getRedirectUrl() != null
                && savedRequest.getRedirectUrl().contains("/booking/movies/");
        model.addAttribute("bookingRequired", bookingRequired);
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerDTO", new RegisterRequestDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerDTO") RegisterRequestDTO dto,
                                      BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        
        try {
            authService.registerCustomer(dto);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi, vui lòng thử lại sau.");
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("forgotPasswordDTO", new ForgotPasswordRequestDTO());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("forgotPasswordDTO") ForgotPasswordRequestDTO dto,
                                        BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            authService.requestForgotPassword(dto.getEmail());
            model.addAttribute("successMessage", "Neu email ton tai trong he thong, huong dan dat lai mat khau se duoc gui den email do.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Khong the gui email, vui long thu lai sau.");
        }

        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(required = false) String email,
                                        @RequestParam(required = false) String code,
                                        Model model) {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setEmail(email);
        dto.setCode(code);
        model.addAttribute("resetPasswordDTO", dto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute("resetPasswordDTO") ResetPasswordRequestDTO dto,
                                       BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            authService.resetPassword(dto);
            return "redirect:/login?resetSuccess=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Da xay ra loi, vui long thu lai sau.");
            return "auth/reset-password";
        }
    }

    @GetMapping("/activate")
    public String activateAccount(@RequestParam String email, @RequestParam String code) {
        return verifyEmail(email, code);
    }

    @GetMapping("/api/auth/verify-email")
    public String verifyEmail(@RequestParam String email, @RequestParam String code) {
        try {
            authService.activateAccount(email, code);
            return "redirect:/login?activated=true";
        } catch (Exception e) {
            return "redirect:/login?activationError=true";
        }
    }
}
