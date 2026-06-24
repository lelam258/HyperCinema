package com.cinema.hyperCinema.security.guard;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchMovieRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("movieAccessGuard")
@RequiredArgsConstructor
@Slf4j
public class MovieAccessGuard {

    private static final String MANAGER_ROLE_NAME = "Manager";

    private final UserRepository userRepository;
    private final BranchMovieRepository branchMovieRepository;

    public boolean canRead(Authentication authentication, Integer movieId) {
        if (movieId == null) {
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

            return branchMovieRepository.existsByIdBranchIdAndIdMovieId(branch.getBranchId(), movieId);
        } catch (RuntimeException ex) {
            log.warn("MovieAccessGuard.canRead failed for principal '{}', movieId={}: {}",
                    principalName, movieId, ex.toString());
            return false;
        }
    }
}
