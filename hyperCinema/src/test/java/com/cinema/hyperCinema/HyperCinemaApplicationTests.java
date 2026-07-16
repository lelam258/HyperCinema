package com.cinema.hyperCinema;

import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@SpringBootTest
class HyperCinemaApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
    }

    @Test
    void debugUser() {
        Optional<User> userOpt = userRepository.findByUsername("khoa1");
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            System.out.println("=== DEBUG USER khoa1 ===");
            System.out.println("Username: " + u.getUsername());
            System.out.println("Full Name: " + u.getFullName());
            System.out.println("Password Hash: " + u.getPasswordHash());
            System.out.println("Email: " + u.getEmail());
            System.out.println("Phone: " + u.getPhone());
            System.out.println("Role: " + (u.getRole() != null ? u.getRole().getName() : "null"));
            System.out.println("Status: " + u.getStatus());
            boolean matches = passwordEncoder.matches("123456", u.getPasswordHash());
            System.out.println("Matches '123456': " + matches);
            System.out.println("=======================");
        } else {
            System.out.println("=== USER khoa1 NOT FOUND ===");
        }

        Optional<User> userOpt2 = userRepository.findByUsername("xhoa");
        if (userOpt2.isPresent()) {
            User u = userOpt2.get();
            System.out.println("=== DEBUG USER xhoa ===");
            System.out.println("Username: " + u.getUsername());
            System.out.println("Full Name: " + u.getFullName());
            System.out.println("Email: " + u.getEmail());
            System.out.println("Phone: " + u.getPhone());
            System.out.println("Status: " + u.getStatus());
            System.out.println("=======================");
        } else {
            System.out.println("=== USER xhoa NOT FOUND ===");
        }
    }
}
