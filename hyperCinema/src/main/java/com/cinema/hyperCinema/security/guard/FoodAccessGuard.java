package com.cinema.hyperCinema.security.guard;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("foodAccessGuard")
@RequiredArgsConstructor
@Slf4j
public class FoodAccessGuard {

    private final UserRepository userRepository;

    /**
     * Admin và Manager có thể quản lý mặt hàng (CRUD).
     * Staff không có quyền CRUD mặt hàng.
     */
    public boolean canManageItem(Authentication authentication) {
        Optional<User> user = findUser(authentication);
        if (user.isEmpty()) {
            return false;
        }
        User u = user.get();
        return isAdmin(u) || isManager(u);
    }

    /**
     * Admin, Manager, Staff đều có thể thao tác đơn đặt đồ ăn.
     */
    public boolean canManageOrder(Authentication authentication) {
        Optional<User> user = findUser(authentication);
        if (user.isEmpty()) {
            return false;
        }
        User u = user.get();
        return isAdmin(u) || isManager(u) || isStaff(u);
    }

    /**
     * Chỉ Admin mới có quyền xóa mặt hàng.
     */
    public boolean canDeleteItem(Authentication authentication) {
        Optional<User> user = findUser(authentication);
        if (user.isEmpty()) {
            return false;
        }
        return isAdmin(user.get());
    }

    private Optional<User> findUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<User> user = userRepository.findByUsername(authentication.getName());
            if (user.isEmpty()) {
                user = userRepository.findByEmail(authentication.getName());
            }
            return user;
        } catch (RuntimeException ex) {
            log.warn("FoodAccessGuard user lookup failed for principal '{}': {}",
                    authentication.getName(), ex.toString());
            return Optional.empty();
        }
    }

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
    }

    private static boolean isManager(User user) {
        return isRole(user, "Manager") || isRole(user, "BranchManager")
                || isRole(user, "Branch Manager") || isRole(user, "Branch_Manager");
    }

    private static boolean isStaff(User user) {
        return isRole(user, "Staff");
    }

    private static boolean isRole(User user, String expected) {
        Role role = user.getRole();
        return role != null && normalizeRoleName(expected).equals(normalizeRoleName(role.getName()));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        String normalized = roleName.trim();
        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }
        return normalized.replaceAll("[\\s_]+", "").toUpperCase();
    }
}
