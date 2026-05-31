package com.cinema.hyperCinema.exception.branch;

import com.cinema.hyperCinema.controller.admin.BranchController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@ControllerAdvice(assignableTypes = BranchController.class)
public class BranchExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BranchExceptionHandler.class);

    static final String DEFAULT_REDIRECT = "/admin/branches";

    static final String NOT_FOUND_VIEW = "error/branch-not-found";

    static final String FORBIDDEN_VIEW = "error/branch-forbidden";

    @ExceptionHandler(BranchNotFoundException.class)
    public String handleNotFound(BranchNotFoundException ex,
                                 Model model,
                                 HttpServletResponse response) {
        log.debug("Branch not found: key={}", ex.getKey());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("errorKey", ex.getKey());
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        return NOT_FOUND_VIEW;
    }

    @ExceptionHandler(BranchValidationException.class)
    public String handleValidation(BranchValidationException ex,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request) {
        log.debug("Branch validation failed: key={}", ex.getKey());
        redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        return "redirect:" + resolveRedirectTarget(request);
    }

    @ExceptionHandler(BranchAccessDeniedException.class)
    public String handleBranchAccessDenied(BranchAccessDeniedException ex,
                                           Model model,
                                           HttpServletResponse response) {
        log.debug("Branch access denied (business): key={}", ex.getKey());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("errorKey", ex.getKey());
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        return FORBIDDEN_VIEW;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleSpringAccessDenied(AccessDeniedException ex,
                                           Model model,
                                           HttpServletResponse response) {
        log.debug("Spring Security access denied for admin branch route: {}", ex.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("errorKey", BranchAccessDeniedException.KEY);
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        return FORBIDDEN_VIEW;
    }

    private String resolveRedirectTarget(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Referer"))
                .filter(ref -> ref.startsWith("/")
                        || ref.startsWith(buildBaseUrl(request)))
                .map(BranchExceptionHandler::stripBaseUrl)
                .orElse(DEFAULT_REDIRECT);
    }

    private static String buildBaseUrl(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder()
                .append(request.getScheme()).append("://")
                .append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }

    private static String stripBaseUrl(String url) {
        if (url.startsWith("/")) {
            return url;
        }
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return DEFAULT_REDIRECT;
        }
        int pathStart = url.indexOf('/', schemeEnd + 3);
        return pathStart < 0 ? DEFAULT_REDIRECT : url.substring(pathStart);
    }
}
