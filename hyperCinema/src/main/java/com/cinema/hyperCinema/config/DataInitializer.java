package com.cinema.hyperCinema.config;

import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final List<String> DEFAULT_ROLES = List.of(
            "Admin",
            "Manager",
            "Staff",
            "Customer"
    );

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (String roleName : DEFAULT_ROLES) {
            roleRepository.findByNameIgnoreCase(roleName)
                    .orElseGet(() -> createRole(roleName));
        }
    }

    private Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }
}
