package com.cinema.hyperCinema.security.guard;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("branchAccessGuard")
@RequiredArgsConstructor
@Slf4j
public class BranchAccessGuard {

    private static final String MANAGER_ROLE_NAME = "Manager";

    private final UserRepository userRepository;

    public boolean canRead(Authentication authentication, Integer branchId) {
        if (branchId == null) {
            return false;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank()) {
            return false;
        }

        try {
            Optional<User> userOpt = userRepository.findByUsername(principalName);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(principalName);
            }
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            Role role = user.getRole();
            if (role == null || !MANAGER_ROLE_NAME.equals(role.getName())) {
                return false;
            }

            Branch branch = user.getBranch();
            if (branch == null || branch.getBranchId() == null) {
                return false;
            }

            return branchId.equals(branch.getBranchId());
        } catch (RuntimeException ex) {
            log.warn("BranchAccessGuard.canRead failed for principal '{}', branchId={}: {}",
                    principalName, branchId, ex.toString());
            return false;
        }
    }
}
