package com.cinema.hyperCinema.security.guard;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("hallAccessGuard")
@RequiredArgsConstructor
@Slf4j
public class HallAccessGuard {

    private final UserRepository userRepository;
    private final HallRepository hallRepository;

    public boolean canManageBranch(Authentication authentication, Integer branchId) {
        if (branchId == null) {
            return false;
        }
        Optional<User> user = findUser(authentication);
        if (user.isEmpty()) {
            return false;
        }
        return canManageBranch(user.get(), branchId);
    }

    public boolean canManageHall(Authentication authentication, Integer hallId) {
        if (hallId == null) {
            return false;
        }
        try {
            Optional<Hall> hall = hallRepository.findById(hallId);
            if (hall.isEmpty() || hall.get().getBranch() == null) {
                return false;
            }
            Integer branchId = hall.get().getBranch().getBranchId();
            return canManageBranch(authentication, branchId);
        } catch (RuntimeException ex) {
            log.warn("HallAccessGuard.canManageHall failed for hallId={}: {}", hallId, ex.toString());
            return false;
        }
    }

    private boolean canManageBranch(User user, Integer branchId) {
        if (isAdmin(user)) {
            return true;
        }
        if (!isRole(user, "Manager") && !isBranchManager(user)) {
            return false;
        }
        Branch branch = user.getBranch();
        return branch != null && branchId.equals(branch.getBranchId());
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
            log.warn("HallAccessGuard user lookup failed for principal '{}': {}",
                    authentication.getName(), ex.toString());
            return Optional.empty();
        }
    }

    private static boolean isBranchManager(User user) {
        return isRole(user, "BranchManager") || isRole(user, "Branch Manager") || isRole(user, "Branch_Manager");
    }

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
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
