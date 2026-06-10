package com.cinema.hyperCinema.service.hall.impl;

import java.util.List;

import com.cinema.hyperCinema.dto.admin.hall.request.HallSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.hall.request.HallCreateRequest;

import com.cinema.hyperCinema.dto.admin.hall.request.HallUpdateRequest;
import com.cinema.hyperCinema.dto.admin.hall.response.BranchOption;
import com.cinema.hyperCinema.dto.admin.hall.response.HallDetailView;
import com.cinema.hyperCinema.dto.admin.hall.response.HallListItem;
import com.cinema.hyperCinema.dto.admin.hall.response.HallManagementContext;
import com.cinema.hyperCinema.exception.hall.HallNotFoundException;
import com.cinema.hyperCinema.exception.hall.HallValidationException;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Hall;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.HallSpecifications;
import com.cinema.hyperCinema.repository.SeatRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.hall.HallService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class HallServiceImpl implements HallService {

    private final HallRepository hallRepository;
    private final BranchRepository branchRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<HallListItem> search(HallSearchCriteria criteria, Pageable pageable, User actor) {
        User current = loadActor(actor);
        Integer forcedBranchId = forcedBranchId(current);
        if (!isAdmin(current) && forcedBranchId == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        Specification<Hall> spec = HallSpecifications.matches(criteria, forcedBranchId);
        return hallRepository.findAll(spec, pageable).map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public HallDetailView findById(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));
        return toDetailView(hall);
    }

    @Override
    public HallDetailView create(HallCreateRequest request, User actor) {
        User current = loadActor(actor);
        String name = normalizeName(request.getName());
        String hallType = normalizeRequiredText(request.getHallType(), "hall.type.required", 50, "hall.type.too_long");
        Integer capacity = normalizeCapacity(request.getCapacity());
        String status = normalizeRequiredText(request.getStatus(), "hall.status.required", 50, "hall.status.too_long");
        Integer targetBranchId = resolveTargetBranchId(request.getBranchId(), current);
        Branch branch = branchRepository.findById(targetBranchId)
                .orElseThrow(() -> new HallValidationException("hall.branch.required"));

        if (hallRepository.existsByBranch_BranchIdAndNameIgnoreCase(targetBranchId, name)) {
            throw new HallValidationException("hall.name.duplicate");
        }

        Hall hall = new Hall();
        hall.setName(name);
        hall.setBranch(branch);
        hall.setHallType(hallType);
        hall.setCapacity(capacity);
        hall.setStatus(status);
        return toDetailView(hallRepository.save(hall));
    }

    @Override
    public HallDetailView update(Integer hallId, HallUpdateRequest request, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));

        String name = normalizeName(request.getName());
        String hallType = normalizeRequiredText(request.getHallType(), "hall.type.required", 50, "hall.type.too_long");
        Integer capacity = normalizeCapacity(request.getCapacity());
        String status = normalizeRequiredText(request.getStatus(), "hall.status.required", 50, "hall.status.too_long");
        Integer targetBranchId = hallBranchId(hall);

        if (isAdmin(current) && request.getBranchId() != null
                && !request.getBranchId().equals(targetBranchId)) {
            ensureCanMove(hallId);
            Branch targetBranch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new HallValidationException("hall.branch.required"));
            hall.setBranch(targetBranch);
            targetBranchId = targetBranch.getBranchId();
        } else if (!isAdmin(current) && request.getBranchId() != null
                && !request.getBranchId().equals(targetBranchId)) {
            throw new HallValidationException("hall.branch.scope_denied");
        }

        if (hallRepository.existsByBranch_BranchIdAndNameIgnoreCaseAndHallIdNot(
                targetBranchId, name, hallId)) {
            throw new HallValidationException("hall.name.duplicate");
        }

        hall.setName(name);
        hall.setHallType(hallType);
        hall.setCapacity(capacity);
        hall.setStatus(status);
        return toDetailView(hallRepository.save(hall));
    }

    @Override
    public void delete(Integer hallId, User actor) {
        User current = loadActor(actor);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException(hallId));
        assertCanManageBranch(current, hallBranchId(hall));
        ensureCanDelete(hallId);
        hallRepository.delete(hall);
    }

    @Override
    @Transactional(readOnly = true)
    public HallManagementContext managementContext(User actor) {
        User current = loadActor(actor);
        boolean admin = isAdmin(current);
        BranchOption lockedBranch = null;
        if (!admin && current.getBranch() != null) {
            lockedBranch = toBranchOption(current.getBranch());
        }
        List<BranchOption> branches = admin
                ? branchRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                        .map(this::toBranchOption)
                        .toList()
                : List.of();
        return HallManagementContext.builder()
                .admin(admin)
                .sidebar(sidebarFor(current))
                .lockedBranch(lockedBranch)
                .branchOptions(branches)
                .build();
    }

    private User loadActor(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new HallValidationException("hall.access.denied");
        }
        return userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new HallValidationException("hall.access.denied"));
    }

    private Integer resolveTargetBranchId(Integer requestedBranchId, User actor) {
        if (isAdmin(actor)) {
            if (requestedBranchId == null) {
                throw new HallValidationException("hall.branch.required");
            }
            return requestedBranchId;
        }
        Integer branchId = forcedBranchId(actor);
        if (branchId == null) {
            throw new HallValidationException("hall.branch.scope_required");
        }
        if (requestedBranchId != null && !requestedBranchId.equals(branchId)) {
            throw new HallValidationException("hall.branch.scope_denied");
        }
        return branchId;
    }

    private void assertCanManageBranch(User actor, Integer branchId) {
        if (isAdmin(actor)) {
            return;
        }
        Integer scopedBranchId = forcedBranchId(actor);
        if (scopedBranchId == null || !scopedBranchId.equals(branchId)) {
            throw new HallValidationException("hall.branch.scope_denied");
        }
    }

    private Integer forcedBranchId(User actor) {
        if (isManager(actor) || isBranchManager(actor)) {
            Branch branch = actor.getBranch();
            return branch == null ? null : branch.getBranchId();
        }
        if (isAdmin(actor)) {
            return null;
        }
        throw new HallValidationException("hall.access.denied");
    }

    private void ensureCanMove(Integer hallId) {
        if (seatRepository.existsByHall_HallId(hallId) || showtimeRepository.existsByHall_HallId(hallId)) {
            throw new HallValidationException("hall.branch.cannot_move_with_dependencies");
        }
    }

    private void ensureCanDelete(Integer hallId) {
        if (seatRepository.existsByHall_HallId(hallId) || showtimeRepository.existsByHall_HallId(hallId)) {
            throw new HallValidationException("hall.cannot_delete_with_dependencies");
        }
    }

    private HallListItem toListItem(Hall hall) {
        Integer hallId = hall.getHallId();
        long seatCount = seatRepository.countByHall_HallId(hallId);
        long showtimeCount = showtimeRepository.countByHall_HallId(hallId);
        Branch branch = hall.getBranch();
        return HallListItem.builder()
                .hallId(hallId)
                .name(hall.getName())
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? "" : branch.getName())
                .city(branch == null ? "" : branch.getCity())
                .hallType(hall.getHallType())
                .capacity(hall.getCapacity())
                .status(hall.getStatus())
                .seatCount(seatCount)
                .showtimeCount(showtimeCount)
                .canDelete(seatCount == 0 && showtimeCount == 0)
                .build();
    }

    private HallDetailView toDetailView(Hall hall) {
        Integer hallId = hall.getHallId();
        long seatCount = seatRepository.countByHall_HallId(hallId);
        long showtimeCount = showtimeRepository.countByHall_HallId(hallId);
        Branch branch = hall.getBranch();
        return HallDetailView.builder()
                .hallId(hallId)
                .name(hall.getName())
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? "" : branch.getName())
                .city(branch == null ? "" : branch.getCity())
                .address(branch == null ? "" : branch.getAddress())
                .hallType(hall.getHallType())
                .capacity(hall.getCapacity())
                .status(hall.getStatus())
                .seatCount(seatCount)
                .showtimeCount(showtimeCount)
                .canDelete(seatCount == 0 && showtimeCount == 0)
                .build();
    }

    private BranchOption toBranchOption(Branch branch) {
        return BranchOption.builder()
                .branchId(branch.getBranchId())
                .name(branch.getName())
                .city(branch.getCity())
                .build();
    }

    private static Integer hallBranchId(Hall hall) {
        Branch branch = hall.getBranch();
        return branch == null ? null : branch.getBranchId();
    }

    private static String normalizeName(String name) {
        return normalizeRequiredText(name, "hall.name.required", 50, "hall.name.too_long");
    }

    private static String normalizeRequiredText(String value, String requiredKey, int maxLength, String tooLongKey) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new HallValidationException(requiredKey);
        }
        if (normalized.length() > maxLength) {
            throw new HallValidationException(tooLongKey);
        }
        return normalized;
    }

    private static Integer normalizeCapacity(Integer capacity) {
        if (capacity == null || capacity < 1) {
            throw new HallValidationException("hall.capacity.invalid");
        }
        return capacity;
    }

    private static boolean isAdmin(User user) {
        return isRole(user, "Admin") || isRole(user, "Administrator");
    }

    private static boolean isManager(User user) {
        return isRole(user, "Manager");
    }

    private static boolean isBranchManager(User user) {
        return isRole(user, "BranchManager") || isRole(user, "Branch Manager") || isRole(user, "Branch_Manager");
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

    private static String sidebarFor(User user) {
        if (isAdmin(user)) {
            return "admin";
        }
        if (isManager(user)) {
            return "manager";
        }
        return "branch";
    }
}
