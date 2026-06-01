package com.cinema.hyperCinema.controller;

import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserRestController {

    private final UserService userService;

    public AdminUserRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
        try {
            User user = new User();
            user.setName(request.getName());
            user.setFullName(request.getName()); // Sửa lỗi: Thêm fullName vì entity yêu cầu nullable = false
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPasswordHash(request.getPassword()); // Sửa lỗi: Đổi từ setPassword sang setPasswordHash
            user.setPhone(request.getPhone());

            if (request.getRoleId() != null) {
                Role role = new Role();
                role.setRoleId(request.getRoleId());
                user.setRole(role);
            }

            user.setStatus(request.getStatus() != null ? request.getStatus() : "Active");

            User created = userService.createUser(user);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Integer id, @RequestBody UserRequest request) {
        try {
            User userDetails = new User();
            userDetails.setName(request.getName());
            userDetails.setFullName(request.getName()); // Sửa lỗi: Thêm fullName vì entity yêu cầu nullable = false
            userDetails.setUsername(request.getUsername());
            userDetails.setEmail(request.getEmail());
            userDetails.setPhone(request.getPhone());
            userDetails.setStatus(request.getStatus());

            if (request.getRoleId() != null) {
                Role role = new Role();
                role.setRoleId(request.getRoleId());
                userDetails.setRole(role);
            }

            User updated = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable("id") Integer id) {
        try {
            User updated = userService.toggleUserStatus(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable("id") Integer id, @RequestBody Map<String, String> body) {
        try {
            String newPassword = body.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password cannot be empty"));
            }
            User updated = userService.resetUserPassword(id, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Getter @Setter
    public static class     UserRequest {
        private String name;
        private String username;
        private String email;
        private String password;
        private String phone;
        private Integer roleId;
        private String status;
    }
}