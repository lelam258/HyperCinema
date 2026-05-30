package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.RoleRepository;
import com.cinema.hyperCinema.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public AdminUserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        List<User> users = userService.searchAndFilterUsers(search, role, status);
        List<Role> roles = roleRepository.findAll();
        Map<String, Long> stats = userService.getUserStatistics();

        model.addAttribute("users", users);
        model.addAttribute("roles", roles);
        model.addAttribute("stats", stats);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);

        return "admin/users";
    }

    @GetMapping("/{id}")
    public String userDetails(@PathVariable("id") Integer id, Model model) {
        User user = userService.getUserById(id);
        List<Role> roles = roleRepository.findAll();

        // Map role-based permissions for Wireframe 4
        Map<String, String> permissions = new HashMap<>();
        String roleName = user.getRole() != null ? user.getRole().getName() : "Viewer";

        if ("Administrator".equalsIgnoreCase(roleName)) {
            permissions.put("Dashboard Access", "Granted");
            permissions.put("Reports Access", "Granted");
            permissions.put("User Management", "Granted");
        } else if ("Manager".equalsIgnoreCase(roleName)) {
            permissions.put("Dashboard Access", "Granted");
            permissions.put("Reports Access", "Granted");
            permissions.put("User Management", "Denied");
        } else if ("User".equalsIgnoreCase(roleName)) {
            permissions.put("Dashboard Access", "Granted");
            permissions.put("Reports Access", "Denied");
            permissions.put("User Management", "Denied");
        } else { // Viewer
            permissions.put("Dashboard Access", "Granted");
            permissions.put("Reports Access", "Denied");
            permissions.put("User Management", "Denied");
        }

        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        model.addAttribute("permissions", permissions);

        return "admin/user-details";
    }
}
