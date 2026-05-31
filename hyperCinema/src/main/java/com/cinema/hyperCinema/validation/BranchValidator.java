package com.cinema.hyperCinema.validation;

import com.cinema.hyperCinema.dto.admin.branch.request.BranchCreateRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchUpdateRequest;
import com.cinema.hyperCinema.exception.branch.BranchValidationException;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BranchValidator {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("Active", "Inactive", "Maintenance");

    private static final String ROLE_MANAGER = "Manager";
    private static final String ROLE_STAFF = "Staff";

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    public void validateCreate(BranchCreateRequest request) {
        if (branchRepository.existsByCityIgnoreCaseAndNameIgnoreCase(
                request.getCity(), request.getName())) {
            throw new BranchValidationException("branch.duplicate_name_in_city");
        }
    }

    public void validateUpdate(Integer branchId, BranchUpdateRequest request) {
        if (branchRepository.existsByCityIgnoreCaseAndNameIgnoreCaseAndBranchIdNot(
                request.getCity(), request.getName(), branchId)) {
            throw new BranchValidationException("branch.duplicate_name_in_city");
        }
    }

    public void validateStatusValue(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new BranchValidationException("branch.status.invalid");
        }
    }

    public void validateManagerRoleAndConflict(User user, Integer branchId) {
        if (!hasRole(user, ROLE_MANAGER)) {
            throw new BranchValidationException("branch.assign_manager.role_invalid");
        }
        Optional<User> existingActiveManager =
                userRepository.findActiveManagerByBranchId(branchId);
        if (existingActiveManager.isPresent()
                && !existingActiveManager.get().getUserId().equals(user.getUserId())) {
            throw new BranchValidationException("branch.manager_conflict");
        }
    }

    public void validateStaffRole(User user) {
        if (!hasRole(user, ROLE_STAFF)) {
            throw new BranchValidationException("branch.assign_staff.role_invalid");
        }
    }

    public void validateManagerOwnsBranch(User manager, Integer branchId) {
        if (!hasRole(manager, ROLE_MANAGER)
                || manager.getBranch() == null
                || !branchId.equals(manager.getBranch().getBranchId())) {
            throw new BranchValidationException(
                    "branch.assign_staff.manager_branch_mismatch");
        }
    }

    private static boolean hasRole(User user, String expectedRoleName) {
        if (user == null) {
            return false;
        }
        Role role = user.getRole();
        return role != null && expectedRoleName.equals(role.getName());
    }
}
