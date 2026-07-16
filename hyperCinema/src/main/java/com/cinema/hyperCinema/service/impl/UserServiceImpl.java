package com.cinema.hyperCinema.service.impl;

import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.RoleRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findByStatusIgnoreCase("Active");
    }

    @Override
    public List<User> searchAndFilterUsers(String query, String roleName, String status) {
        List<User> users = shouldIncludeAllStatuses(status)
                ? userRepository.findAll()
                : userRepository.findByStatusIgnoreCase("Active");

        return users.stream()
                .filter(u -> {
                    if (query == null || query.trim().isEmpty()) return true;
                    String q = query.toLowerCase().trim();
                    return (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
                            || (u.getRole() != null && u.getRole().getName().toLowerCase().contains(q));
                })
                .filter(u -> {
                    if (roleName == null || roleName.isEmpty() || roleName.equalsIgnoreCase("All") || roleName.equalsIgnoreCase("All Roles")) return true;
                    return u.getRole() != null && u.getRole().getName().equalsIgnoreCase(roleName);
                })
                .filter(u -> {
                    if (status == null || status.isEmpty() || status.equalsIgnoreCase("All") || status.equalsIgnoreCase("All Status")) return true;
                    return u.getStatus() != null && u.getStatus().equalsIgnoreCase(status);
                })
                .collect(Collectors.toList());
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    @Override
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        if (user.getFullName() == null) {
            user.setFullName(user.getUsername());
        }

        if (user.getRole() == null || user.getRole().getRoleId() == null) {
            // Default to Viewer if no role specified
            Role defaultRole = roleRepository.findByName("Viewer")
                    .orElseThrow(() -> new IllegalStateException("Default role 'Viewer' not found"));
            user.setRole(defaultRole);
        } else {
            Role role = roleRepository.findById(user.getRole().getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + user.getRole().getRoleId()));
            user.setRole(role);
        }

        if (user.getStatus() == null) {
            user.setStatus("Active");
        }

        if (user.getPasswordHash() != null) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Integer id, User userDetails) {
        User user = getUserById(id);

        if (!user.getUsername().equals(userDetails.getUsername()) && userRepository.existsByUsername(userDetails.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + userDetails.getUsername());
        }
        if (!user.getEmail().equals(userDetails.getEmail()) && userRepository.existsByEmail(userDetails.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + userDetails.getEmail());
        }

        user.setFullName(userDetails.getFullName());
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        
        if (userDetails.getRole() != null && userDetails.getRole().getRoleId() != null) {
            Role role = roleRepository.findById(userDetails.getRole().getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + userDetails.getRole().getRoleId()));
            user.setRole(role);
        }

        if (userDetails.getStatus() != null) {
            user.setStatus(userDetails.getStatus());
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Integer id) {
        User user = getUserById(id);
        user.setStatus("Inactive");
        userRepository.save(user);
    }

    @Override
    public User toggleUserStatus(Integer id) {
        User user = getUserById(id);
        if ("Active".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("Inactive");
        } else {
            user.setStatus("Active");
        }
        return userRepository.save(user);
    }

    @Override
    public User assignUserRole(Integer id, Integer roleId) {
        User user = getUserById(id);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    public User resetUserPassword(Integer id, String newPassword) {
        User user = getUserById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Override
    public Map<String, Long> getUserStatistics() {
        List<User> users = userRepository.findAll();
        Map<String, Long> stats = new HashMap<>();

        long total = users.size();
        long active = users.stream().filter(u -> "Active".equalsIgnoreCase(u.getStatus())).count();
        long inactive = users.stream().filter(u -> "Inactive".equalsIgnoreCase(u.getStatus())).count();
        long admins = users.stream().filter(u -> u.getRole() != null && "Administrator".equalsIgnoreCase(u.getRole().getName())).count();

        stats.put("total", total);
        stats.put("active", active);
        stats.put("inactive", inactive);
        stats.put("admins", admins);

        return stats;
    }

    private static boolean shouldIncludeAllStatuses(String status) {
        return status != null
                && !status.isBlank()
                && !status.equalsIgnoreCase("All")
                && !status.equalsIgnoreCase("All Status");
    }
}
