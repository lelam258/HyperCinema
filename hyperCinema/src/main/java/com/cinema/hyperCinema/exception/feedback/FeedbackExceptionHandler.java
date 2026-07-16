package com.cinema.hyperCinema.exception.feedback;

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

@ControllerAdvice(basePackages = "com.cinema.hyperCinema.controller")
public class FeedbackExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FeedbackExceptionHandler.class);

    static final String DEFAULT_REDIRECT = "/feedback";
    static final String NOT_FOUND_VIEW = "error/feedback-not-found";
    static final String FORBIDDEN_VIEW = "error/feedback-forbidden";

    @ExceptionHandler(FeedbackNotFoundException.class)
    public String handleNotFound(FeedbackNotFoundException ex,
                                 Model model,
                                 HttpServletResponse response) {
        log.debug("Feedback not found: key={}", ex.getKey());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("errorKey", ex.getKey());
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        return NOT_FOUND_VIEW;
    }

    @ExceptionHandler(FeedbackValidationException.class)
    public String handleValidation(FeedbackValidationException ex,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   Model model) {
        log.debug("Feedback validation/business logic failed: key={}", ex.getKey());
        
        // Nếu lỗi là do vi phạm quyền truy cập (Access Denied)
        if ("feedback.access_denied".equals(ex.getKey())) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            model.addAttribute("errorKey", ex.getKey());
            model.addAttribute("status", HttpStatus.FORBIDDEN.value());
            return FORBIDDEN_VIEW;
        }

        redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        return "redirect:" + resolveRedirectTarget(request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleSpringAccessDenied(AccessDeniedException ex,
                                           Model model,
                                           HttpServletResponse response) {
        log.debug("Access denied for feedback route: {}", ex.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("errorKey", "feedback.access_denied");
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        return FORBIDDEN_VIEW;
    }

    private String resolveRedirectTarget(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Referer"))
                .filter(ref -> ref.startsWith("/") || ref.startsWith(buildBaseUrl(request)))
                .map(FeedbackExceptionHandler::stripBaseUrl)
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
