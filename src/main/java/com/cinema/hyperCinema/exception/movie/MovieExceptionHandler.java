package com.cinema.hyperCinema.exception.movie;

import com.cinema.hyperCinema.controller.admin.MovieController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = MovieController.class)
public class MovieExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MovieExceptionHandler.class);

    static final String DEFAULT_REDIRECT = "/admin/movies";

    static final String NOT_FOUND_VIEW = "error/movie-not-found";

    static final String FORBIDDEN_VIEW = "error/movie-forbidden";

    @ExceptionHandler(MovieNotFoundException.class)
    public String handleNotFound(MovieNotFoundException ex,
                                 Model model,
                                 HttpServletResponse response) {
        log.debug("Movie not found: key={}", ex.getKey());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("errorKey", ex.getKey());
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        return NOT_FOUND_VIEW;
    }

    @ExceptionHandler(MovieValidationException.class)
    public String handleValidation(MovieValidationException ex,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request) {
        log.debug("Movie validation failed: key={}", ex.getKey());
        redirectAttributes.addFlashAttribute("errorKey", ex.getKey());
        return "redirect:" + resolveRedirectTarget(request);
    }

    @ExceptionHandler(MovieAccessDeniedException.class)
    public String handleMovieAccessDenied(MovieAccessDeniedException ex,
                                          Model model,
                                          HttpServletResponse response) {
        log.debug("Movie access denied (business): key={}", ex.getKey());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("errorKey", ex.getKey());
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        return FORBIDDEN_VIEW;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleSpringAccessDenied(AccessDeniedException ex,
                                           Model model,
                                           HttpServletResponse response) {
        log.debug("Spring Security access denied for admin movie route: {}", ex.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("errorKey", MovieAccessDeniedException.KEY);
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        return FORBIDDEN_VIEW;
    }

    private String resolveRedirectTarget(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Referer"))
                .filter(ref -> ref.startsWith("/")
                        || ref.startsWith(buildBaseUrl(request)))
                .map(MovieExceptionHandler::stripBaseUrl)
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
