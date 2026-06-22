package com.cinema.hyperCinema.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.dto.admin.branch.response.BranchDetailView;
import com.cinema.hyperCinema.dto.admin.branch.response.HallSummary;
import com.cinema.hyperCinema.dto.admin.branch.response.UserSummary;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BranchMapper {

    private static final String ROLE_MANAGER = "Manager";

    private static final String ROLE_STAFF = "Staff";

    private final HallRepository hallRepository;
    private final UserRepository userRepository;

    public BranchDetailView toDetailView(Branch branch) {
        Integer branchId = branch.getBranchId();

        List<HallSummary> halls = hallRepository.findByBranch_BranchId(branchId)
                .stream()
                .map(BranchMapper::toHallSummary)
                .toList();

        List<UserSummary> managers = userRepository
                .findByBranchIdAndRoleName(branchId, ROLE_MANAGER)
                .stream()
                .map(BranchMapper::toUserSummary)
                .toList();

        List<UserSummary> staffMembers = userRepository
                .findByBranchIdAndRoleName(branchId, ROLE_STAFF)
                .stream()
                .map(BranchMapper::toUserSummary)
                .toList();

        return BranchDetailView.builder()
                .branchId(branch.getBranchId())
                .name(branch.getName())
                .address(branch.getAddress())
                .city(branch.getCity())
                .phone(branch.getPhone())
                .status(branch.getStatus())
                .openingTime(branch.getOpeningTime())
                .closingTime(branch.getClosingTime())
                .createdAt(branch.getCreatedAt())
                .halls(halls)
                .managers(managers)
                .staffMembers(staffMembers)
                .build();
    }

    public static HallSummary toHallSummary(Hall hall) {
        return HallSummary.builder()
                .hallId(hall.getHallId())
                .name(hall.getName())
                .capacity(hall.getCapacity())
                .hallType(hall.getHallType())
                .status(hall.getStatus())
                .build();
    }

    public static UserSummary toUserSummary(User user) {
        Role role = user.getRole();
        return UserSummary.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(role == null ? null : role.getName())
                .status(user.getStatus())
                .build();
    }
}
