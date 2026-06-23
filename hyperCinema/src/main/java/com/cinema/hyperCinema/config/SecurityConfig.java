package com.cinema.hyperCinema.config;

import com.cinema.hyperCinema.security.CustomAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password", "/activate", "/api/auth/verify-email", "/movies/**", "/movies", "/api/payment/vnpay-return").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // ===== Branch Management — REQ 11.1, 11.2, 11.4 =====
                // Mutation endpoints (POST/PUT/PATCH/DELETE) — Admin only
                .requestMatchers(HttpMethod.POST,   "/admin/branches", "/admin/branches/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/admin/branches/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/admin/branches/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/admin/branches/**").hasRole("ADMIN")
                // GET list / new form / edit form / managers-assign — Admin only
                .requestMatchers(HttpMethod.GET,
                        "/admin/branches",
                        "/admin/branches/",
                        "/admin/branches/new",
                        "/admin/branches/*/edit",
                        "/admin/branches/*/managers/assign",
                        "/admin/branches/*/staff/assign").hasRole("ADMIN")
                // GET detail — Admin or Manager (REQ 11.3 refined by @PreAuthorize on controller)
                .requestMatchers(HttpMethod.GET, "/admin/branches/*").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.POST,   "/admin/movies", "/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/admin/movies", "/api/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/admin/movies",
                        "/admin/movies/",
                        "/admin/movies/new",
                        "/admin/movies/*/edit").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/admin/movies/*").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.GET,
                        "/admin/halls",
                        "/admin/halls/",
                        "/admin/halls/new",
                        "/admin/halls/*",
                        "/admin/halls/*/edit").hasAnyRole("ADMIN", "MANAGER", "BRANCH_MANAGER", "BRANCHMANAGER")
                .requestMatchers(HttpMethod.POST,
                        "/admin/halls",
                        "/admin/halls/**").hasAnyRole("ADMIN", "MANAGER", "BRANCH_MANAGER", "BRANCHMANAGER")
                .requestMatchers(HttpMethod.PUT,
                        "/admin/halls/**").hasAnyRole("ADMIN", "MANAGER", "BRANCH_MANAGER", "BRANCHMANAGER")
                .requestMatchers(HttpMethod.PATCH,
                        "/admin/halls/**").hasAnyRole("ADMIN", "MANAGER", "BRANCH_MANAGER", "BRANCHMANAGER")
                .requestMatchers(HttpMethod.DELETE,
                        "/admin/halls/**").hasAnyRole("ADMIN", "MANAGER", "BRANCH_MANAGER", "BRANCHMANAGER")
                // Catch-all for /admin/**
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/branch/**").hasAnyRole("BRANCH_MANAGER", "BRANCHMANAGER")
                .requestMatchers("/staff/**").hasRole("STAFF")
                .requestMatchers("/my/**").hasRole("CUSTOMER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
