package com.cinema.hyperCinema;

import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.RoleRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Roles
        List<String> roleNames = Arrays.asList("Administrator", "Manager", "User", "Viewer");
        for (String name : roleNames) {
            if (roleRepository.findByName(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                roleRepository.save(role);
            }
        }

        // 2. Seed Default Admin User (john.admin)
        if (userRepository.findByUsername("john.admin").isEmpty()) {
            Role adminRole = roleRepository.findByName("Administrator")
                    .orElseThrow(() -> new IllegalStateException("Administrator role should exist"));

            User admin = new User();
            admin.setName("John Admin");
            admin.setFullName("John Admin"); // Bổ sung fullName tránh lỗi ràng buộc DB
            admin.setUsername("john.admin");
            admin.setEmail("john.admin@company.com");
            admin.setPasswordHash("admin123");
            admin.setPhone("0123456789");
            admin.setRole(adminRole);
            admin.setStatus("Active");
            admin.setLastLogin(LocalDateTime.now().minusDays(1).withHour(9).withMinute(30).withSecond(0));

            userRepository.save(admin);

            // Seed a few other mock users for the dashboard grid as shown in wireframe 2
            seedMockUsers();
        }
    }

    private void seedMockUsers() {
        Role managerRole = roleRepository.findByName("Manager").orElse(null);
        Role userRole = roleRepository.findByName("User").orElse(null);
        Role viewerRole = roleRepository.findByName("Viewer").orElse(null);

        if (managerRole != null && userRepository.findByUsername("sarah.manager").isEmpty()) {
            User user = new User();
            user.setName("Sarah Manager");
            user.setFullName("Sarah Manager"); // Bổ sung
            user.setUsername("sarah.manager");
            user.setEmail("sarah.m@company.com");
            user.setPasswordHash("password123"); // ĐÃ SỬA: setPassword -> setPasswordHash
            user.setPhone("0987654321");
            user.setRole(managerRole);
            user.setStatus("Active");
            user.setLastLogin(LocalDateTime.now().minusDays(2).withHour(16).withMinute(45).withSecond(0));
            userRepository.save(user);
        }

        if (userRole != null && userRepository.findByUsername("mike.user").isEmpty()) {
            User user = new User();
            user.setName("Mike User");
            user.setFullName("Mike User"); // Bổ sung
            user.setUsername("mike.user");
            user.setEmail("mike.user@company.com");
            user.setPasswordHash("password123"); // ĐÃ SỬA: setPassword -> setPasswordHash
            user.setPhone("0111222333");
            user.setRole(userRole);
            user.setStatus("Active");
            user.setLastLogin(LocalDateTime.now().minusDays(1).withHour(8).withMinute(15).withSecond(0));
            userRepository.save(user);
        }

        if (viewerRole != null && userRepository.findByUsername("emma.viewer").isEmpty()) {
            User user = new User();
            user.setName("Emma Viewer");
            user.setFullName("Emma Viewer"); // Bổ sung
            user.setUsername("emma.viewer");
            user.setEmail("emma.v@company.com");
            user.setPasswordHash("password123"); // ĐÃ SỬA: setPassword -> setPasswordHash
            user.setPhone("0222333444");
            user.setRole(viewerRole);
            user.setStatus("Inactive");
            user.setLastLogin(LocalDateTime.now().minusDays(10).withHour(14).withMinute(20).withSecond(0));
            userRepository.save(user);
        }

        if (managerRole != null && userRepository.findByUsername("david.manager").isEmpty()) {
            User user = new User();
            user.setName("David Manager");
            user.setFullName("David Manager"); // Bổ sung
            user.setUsername("david.manager");
            user.setEmail("david.m@company.com");
            user.setPasswordHash("password123"); // ĐÃ SỬA: setPassword -> setPasswordHash
            user.setPhone("0333444555");
            user.setRole(managerRole);
            user.setStatus("Active");
            user.setLastLogin(LocalDateTime.now().minusDays(3).withHour(11).withMinute(30).withSecond(0));
            userRepository.save(user);
        }
    }
}