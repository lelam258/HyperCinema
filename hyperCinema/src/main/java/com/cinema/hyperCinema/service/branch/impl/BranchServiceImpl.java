package com.cinema.hyperCinema.service.branch.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.branch.request.BranchCreateRequest;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchDetailView;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchListItem;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchSearchCriteria;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchUpdateRequest;
import com.cinema.hyperCinema.dto.admin.branch.response.FieldChange;
import com.cinema.hyperCinema.dto.admin.branch.response.UpdateResult;
import com.cinema.hyperCinema.dto.admin.branch.response.UserSummary;
import com.cinema.hyperCinema.exception.branch.BranchNotFoundException;
import com.cinema.hyperCinema.exception.branch.BranchValidationException;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.BranchSpecifications;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.audit.BranchAuditLogger;
import com.cinema.hyperCinema.service.branch.BranchDiffer;
import com.cinema.hyperCinema.service.branch.BranchService;
import com.cinema.hyperCinema.util.BranchMapper;
import com.cinema.hyperCinema.validation.BranchValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final BranchValidator branchValidator;
    private final BranchAuditLogger branchAuditLogger;
    private final BranchDiffer branchDiffer;
    private final ShowtimeRepository showtimeRepository;
    private final HallRepository hallRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public BranchDetailView findById(Integer branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));
        return branchMapper.toDetailView(branch);
    }

    @Override
    public BranchDetailView create(BranchCreateRequest request, User admin) {

        branchValidator.validateCreate(request);

        Branch entity = new Branch();
        entity.setName(normalizeText(request.getName()));
        entity.setAddress(normalizeText(request.getAddress()));
        entity.setCity(normalizeText(request.getCity()));
        entity.setPhone(normalizeText(request.getPhone()));
        entity.setOpeningTime(request.getOpeningTime());
        entity.setClosingTime(request.getClosingTime());
        entity.setStatus("Active");
        entity.setCreatedAt(LocalDateTime.now());

        Branch saved = branchRepository.save(entity);

        auditSafe(() -> branchAuditLogger.logCreate(saved, admin));

        return branchMapper.toDetailView(saved);
    }

    @Override
    public UpdateResult update(Integer branchId, BranchUpdateRequest request, User admin) {

        Branch existing = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        branchValidator.validateUpdate(branchId, request);

        Branch oldSnapshot = snapshot(existing);

        Branch proposed = snapshot(existing);
        applyRequest(proposed, request);

        List<FieldChange> changes = branchDiffer.diff(oldSnapshot, proposed);

        if (changes.isEmpty()) {
            return UpdateResult.builder()
                    .branch(branchMapper.toDetailView(existing))
                    .hasChanges(false)
                    .changes(List.of())
                    .build();
        }

        applyRequest(existing, request);
        Branch saved = branchRepository.save(existing);

        auditSafe(() -> branchAuditLogger.logUpdate(oldSnapshot, saved, changes, admin));

        return UpdateResult.builder()
                .branch(branchMapper.toDetailView(saved))
                .hasChanges(true)
                .changes(changes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchListItem> search(BranchSearchCriteria criteria, Pageable pageable) {
        Specification<Branch> spec = BranchSpecifications.matches(criteria);
        Page<Branch> page = branchRepository.findAll(spec, pageable);
        return page.map(this::toListItem);
    }

    private BranchListItem toListItem(Branch branch) {
        long hallCount = hallRepository.countByBranch_BranchId(branch.getBranchId());
        return BranchListItem.builder()
                .branchId(branch.getBranchId())
                .name(branch.getName())
                .city(branch.getCity())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .status(branch.getStatus())
                .hallCount(hallCount)
                .createdAt(branch.getCreatedAt())
                .build();
    }

    @Override
    public void changeStatus(Integer branchId, String newStatus, User admin) {

        branchValidator.validateStatusValue(newStatus);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        String oldStatus = branch.getStatus();
        if (Objects.equals(oldStatus, newStatus)) {
            return;
        }

        if ("Inactive".equals(newStatus)
                && showtimeRepository.existsByHall_Branch_BranchIdAndStartTimeAfter(
                        branchId, LocalDateTime.now())) {
            throw new BranchValidationException(
                    "branch.cannot_deactivate_with_future_showtimes");
        }

        branch.setStatus(newStatus);
        Branch saved = branchRepository.save(branch);

        auditSafe(() -> branchAuditLogger.logStatusChange(saved, oldStatus, newStatus, admin));
    }

    @Override
    public void deleteHard(Integer branchId, User admin) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        if (hallRepository.existsByBranch_BranchId(branchId)) {
            throw new BranchValidationException("branch.cannot_delete_with_halls");
        }

        if (userRepository.existsByBranch_BranchId(branchId)) {
            throw new BranchValidationException("branch.cannot_delete_with_users");
        }

        branchRepository.delete(branch);
    }

    @Override
    public void assignManager(Integer branchId, Integer userId, User admin) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BranchValidationException(
                        "branch.assign_manager.role_invalid"));

        branchValidator.validateManagerRoleAndConflict(user, branchId);

        user.setBranch(branch);
        userRepository.save(user);

        auditSafe(() -> branchAuditLogger.logAssignManager(branchId, userId, admin));
    }

    @Override
    public void unassignManager(Integer branchId, Integer userId, User admin) {

        branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new BranchValidationException(
                        "branch.assign_manager.role_invalid"));

        manager.setBranch(null);
        userRepository.save(manager);

        userRepository.clearManagerIdByManagerId(userId);

        auditSafe(() -> branchAuditLogger.logUnassignManager(branchId, userId, admin));
    }

    @Override
    public void assignStaff(Integer branchId, Integer userId, Integer managerId, User admin) {

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new BranchValidationException(
                        "branch.assign_staff.role_invalid"));

        branchValidator.validateStaffRole(staff);

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BranchValidationException(
                        "branch.assign_staff.manager_branch_mismatch"));

        branchValidator.validateManagerOwnsBranch(manager, branchId);

        staff.setBranch(branch);
        staff.setManager(manager);
        userRepository.save(staff);

        auditSafe(() -> branchAuditLogger.logAssignStaff(branchId, userId, managerId, admin));
    }

    @Override
    public void unassignStaff(Integer branchId, Integer userId, User admin) {

        branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new BranchValidationException(
                        "branch.assign_staff.role_invalid"));

        staff.setBranch(null);
        staff.setManager(null);
        userRepository.save(staff);

        auditSafe(() -> branchAuditLogger.logUnassignStaff(branchId, userId, admin));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listUnassignedManagers() {
        return userRepository.findUnassignedManagers().stream()
                .map(BranchMapper::toUserSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listUnassignedStaff() {
        return userRepository.findUnassignedStaff().stream()
                .map(BranchMapper::toUserSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listManagersForBranch(Integer branchId) {
        branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));
        return userRepository.findManagersByBranchId(branchId).stream()
                .map(BranchMapper::toUserSummary)
                .toList();
    }

    private static Branch snapshot(Branch source) {
        Branch copy = new Branch();
        copy.setBranchId(source.getBranchId());
        copy.setName(source.getName());
        copy.setAddress(source.getAddress());
        copy.setCity(source.getCity());
        copy.setPhone(source.getPhone());
        copy.setStatus(source.getStatus());
        copy.setOpeningTime(source.getOpeningTime());
        copy.setClosingTime(source.getClosingTime());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }

    private static void applyRequest(Branch target, BranchUpdateRequest request) {
        target.setName(normalizeText(request.getName()));
        target.setAddress(normalizeText(request.getAddress()));
        target.setCity(normalizeText(request.getCity()));
        target.setPhone(normalizeText(request.getPhone()));
        target.setOpeningTime(request.getOpeningTime());
        target.setClosingTime(request.getClosingTime());
    }

    private static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private void auditSafe(Runnable auditAction) {
        try {
            auditAction.run();
        } catch (Throwable auditEx) {
            try {
                log.warn("Branch audit log failed: {}", auditEx.getMessage(), auditEx);
            } catch (Throwable ignored) {

            }
        }
    }
}
