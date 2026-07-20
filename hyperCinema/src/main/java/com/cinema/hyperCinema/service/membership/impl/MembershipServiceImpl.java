package com.cinema.hyperCinema.service.membership.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanCreateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanSearchCriteria;
import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanUpdateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipCreateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipSearchCriteria;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipUpdateRequest;
import com.cinema.hyperCinema.dto.admin.membership.response.MembershipPlanDetailView;
import com.cinema.hyperCinema.dto.admin.membership.response.MembershipPlanListItem;
import com.cinema.hyperCinema.dto.admin.membership.response.MembershipPlanOption;
import com.cinema.hyperCinema.dto.admin.membership.response.UpdateResult;
import com.cinema.hyperCinema.dto.admin.membership.response.UserMembershipDetailView;
import com.cinema.hyperCinema.dto.admin.membership.response.UserMembershipListItem;
import com.cinema.hyperCinema.dto.admin.membership.response.UserOption;
import com.cinema.hyperCinema.exception.membership.MembershipAccessDeniedException;
import com.cinema.hyperCinema.exception.membership.MembershipNotFoundException;
import com.cinema.hyperCinema.exception.membership.MembershipValidationException;
import com.cinema.hyperCinema.model.MembershipPlan;
import com.cinema.hyperCinema.model.MembershipStatus;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.UserMembership;
import com.cinema.hyperCinema.repository.MembershipPlanRepository;
import com.cinema.hyperCinema.repository.UserMembershipRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.membership.MembershipService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final String PLAN_ACTIVE = "ACTIVE";
    private static final String PLAN_INACTIVE = "INACTIVE";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final MembershipPlanRepository membershipPlanRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<MembershipPlanListItem> searchPlans(MembershipPlanSearchCriteria criteria,
                                                    Pageable pageable,
                                                    User actor) {
        assertAdmin(actor);
        MembershipPlanSearchCriteria normalized =
                (criteria == null ? new MembershipPlanSearchCriteria() : criteria).normalize();
        return membershipPlanRepository.searchPlans(normalized.getKeyword(), normalized.getStatus(), pageable)
                .map(this::toPlanListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlanDetailView findPlan(Integer planId, User actor) {
        assertAdmin(actor);
        return toPlanDetail(loadPlan(planId));
    }

    @Override
    public MembershipPlanDetailView createPlan(MembershipPlanCreateRequest request, User actor) {
        assertAdmin(actor);
        validatePlanRequest(request, null);
        List<MembershipPlan> orderedPlans = new ArrayList<>(membershipPlanRepository.findAllByOrderByLevelAscNameAsc());

        MembershipPlan plan = new MembershipPlan();
        plan.setName(request.getName().trim());
        plan.setDiscountPercent(request.getDiscountPercent());
        plan.setPrice(request.getPrice());
        Integer requestedLevel = request.getLevel() != null ? request.getLevel() : orderedPlans.size() + 1;
        plan.setLevel(clampLevel(requestedLevel, orderedPlans.size() + 1));
        plan.setDurationDays(0);
        plan.setStatus(normalizePlanStatus(request.getStatus()));

        orderedPlans.add(plan.getLevel() - 1, plan);
        reassignLevels(orderedPlans);
        validatePlanSequence(orderedPlans);
        saveExistingPlans(orderedPlans, null);
        return toPlanDetail(membershipPlanRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    public int suggestedPlanLevel(Integer beforePlanId, Integer afterPlanId, User actor) {
        assertAdmin(actor);
        if (beforePlanId != null && afterPlanId != null) {
            throw new MembershipValidationException("membership.plan.position.invalid");
        }
        if (beforePlanId != null) {
            return loadPlan(beforePlanId).getLevel();
        }
        if (afterPlanId != null) {
            return loadPlan(afterPlanId).getLevel() + 1;
        }
        return membershipPlanRepository.findMaxLevel() + 1;
    }

    @Override
    public UpdateResult updatePlan(Integer planId, MembershipPlanUpdateRequest request, User actor) {
        assertAdmin(actor);
        MembershipPlan plan = loadPlan(planId);
        validatePlanRequest(request, planId);

        String nextName = request.getName().trim();
        BigDecimal nextDiscount = request.getDiscountPercent();
        Integer nextRequiredPoints = request.getPrice();
        String nextStatus = normalizePlanStatus(request.getStatus());

        boolean changed = false;
        changed |= !Objects.equals(plan.getName(), nextName);
        changed |= !Objects.equals(plan.getDiscountPercent(), nextDiscount);
        changed |= !Objects.equals(plan.getPrice(), nextRequiredPoints);
        changed |= !Objects.equals(plan.getStatus(), nextStatus);
        if (!changed) {
            return UpdateResult.builder().id(planId).hasChanges(false).build();
        }

        plan.setName(nextName);
        plan.setDiscountPercent(nextDiscount);
        plan.setPrice(nextRequiredPoints);
        plan.setStatus(nextStatus);
        List<MembershipPlan> orderedPlans = new ArrayList<>(membershipPlanRepository.findAllByOrderByLevelAscNameAsc());
        for (int i = 0; i < orderedPlans.size(); i++) {
            if (Objects.equals(orderedPlans.get(i).getPlanId(), planId)) {
                orderedPlans.set(i, plan);
                break;
            }
        }
        validatePlanSequence(orderedPlans);
        membershipPlanRepository.save(plan);
        return UpdateResult.builder().id(planId).hasChanges(changed).build();
    }

    @Override
    public void deactivatePlan(Integer planId, User actor) {
        assertAdmin(actor);
        MembershipPlan plan = loadPlan(planId);
        plan.setStatus(PLAN_INACTIVE);
        membershipPlanRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserMembershipListItem> searchUserMemberships(UserMembershipSearchCriteria criteria,
                                                              Pageable pageable,
                                                              User actor) {
        assertAdmin(actor);
        UserMembershipSearchCriteria normalized =
                (criteria == null ? new UserMembershipSearchCriteria() : criteria).normalize();
        return userMembershipRepository.searchMemberships(
                        normalized.getKeyword(), normalized.getPlanId(), normalized.getStatus(), pageable)
                .map(this::toUserMembershipListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public UserMembershipDetailView findUserMembership(Integer membershipId, User actor) {
        assertAdmin(actor);
        return toUserMembershipDetail(loadUserMembership(membershipId));
    }

    @Override
    public UserMembershipDetailView createUserMembership(UserMembershipCreateRequest request, User actor) {
        assertAdmin(actor);
        User customer = loadCustomer(request.getUserId());
        MembershipPlan plan = loadPlan(request.getPlanId());
        String status = normalizeMembershipStatus(request.getStatus());
        validateMembershipAssignment(customer, plan, status, null);

        UserMembership membership = new UserMembership();
        membership.setUser(customer);
        membership.setPlan(plan);
        membership.setStatus(status);
        membership.setStartDate(LocalDate.now());
        membership.setEndDate(null);
        return toUserMembershipDetail(userMembershipRepository.save(membership));
    }

    @Override
    public UpdateResult updateUserMembership(Integer membershipId,
                                             UserMembershipUpdateRequest request,
                                             User actor) {
        assertAdmin(actor);
        UserMembership membership = loadUserMembership(membershipId);
        MembershipPlan plan = loadPlan(request.getPlanId());
        String status = normalizeMembershipStatus(request.getStatus());
        validateMembershipAssignment(membership.getUser(), plan, status, membershipId);

        boolean changed = false;
        changed |= applyChange(membership.getPlan().getPlanId(), plan.getPlanId(), ignored -> membership.setPlan(plan));
        changed |= applyChange(membership.getStatus(), status, membership::setStatus);
        userMembershipRepository.save(membership);
        return UpdateResult.builder().id(membershipId).hasChanges(changed).build();
    }

    @Override
    public void cancelUserMembership(Integer membershipId, User actor) {
        assertAdmin(actor);
        UserMembership membership = loadUserMembership(membershipId);
        membership.setStatus(MembershipStatus.CANCELLED.name());
        userMembershipRepository.save(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPlanOption> activePlanOptions() {
        return membershipPlanRepository.findByStatusIgnoreCaseOrderByLevelAscNameAsc(PLAN_ACTIVE).stream()
                .map(this::toPlanOption)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPlanOption> allPlanOptions() {
        return membershipPlanRepository.findAllByOrderByLevelAscNameAsc().stream()
                .map(this::toPlanOption)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOption> customerOptions(String keyword) {
        return userRepository.findActiveCustomers(normalizeBlank(keyword)).stream()
                .limit(100)
                .map(user -> UserOption.builder()
                        .userId(user.getUserId())
                        .displayName(user.getFullName() + " - " + user.getEmail())
                        .build())
                .toList();
    }

    private void validatePlanRequest(MembershipPlanCreateRequest request, Integer currentPlanId) {
        if (request == null) {
            throw new MembershipValidationException("membership.plan.invalid");
        }
        String name = normalizeBlank(request.getName());
        if (name == null || name.length() > 100) {
            throw new MembershipValidationException("membership.plan.name.invalid");
        }
        if (currentPlanId == null && membershipPlanRepository.existsByNameIgnoreCase(name)) {
            throw new MembershipValidationException("membership.plan.name.duplicate");
        }
        if (currentPlanId != null && membershipPlanRepository.existsByNameIgnoreCaseAndPlanIdNot(name, currentPlanId)) {
            throw new MembershipValidationException("membership.plan.name.duplicate");
        }
        if (request.getDiscountPercent() == null
                || request.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0
                || request.getDiscountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new MembershipValidationException("membership.plan.discount.invalid");
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            throw new MembershipValidationException("membership.plan.required_points.invalid");
        }
        if (request.getLevel() != null && request.getLevel() < 1) {
            throw new MembershipValidationException("membership.plan.level.invalid");
        }
        normalizePlanStatus(request.getStatus());
    }

    private void validatePlanSequence(List<MembershipPlan> orderedPlans) {
        MembershipPlan previous = null;
        for (MembershipPlan current : orderedPlans) {
            if (previous != null) {
                if (requiredPoints(current) <= requiredPoints(previous)) {
                    throw new MembershipValidationException("membership.plan.required_points.sequence_invalid");
                }
                BigDecimal currentDiscount = safeDiscount(current);
                BigDecimal previousDiscount = safeDiscount(previous);
                if (currentDiscount.compareTo(previousDiscount) < 0) {
                    throw new MembershipValidationException("membership.plan.discount.sequence_invalid");
                }
            }
            previous = current;
        }
    }

    private void reassignLevels(List<MembershipPlan> orderedPlans) {
        for (int i = 0; i < orderedPlans.size(); i++) {
            orderedPlans.get(i).setLevel(i + 1);
        }
    }

    private void saveExistingPlans(List<MembershipPlan> plans, Integer skippedPlanId) {
        plans.stream()
                .filter(plan -> plan.getPlanId() != null)
                .filter(plan -> skippedPlanId == null || !Objects.equals(plan.getPlanId(), skippedPlanId))
                .forEach(membershipPlanRepository::save);
    }

    private int clampLevel(Integer level, int maxLevel) {
        if (level == null || level < 1) {
            return 1;
        }
        return Math.min(level, Math.max(1, maxLevel));
    }

    private int requiredPoints(MembershipPlan plan) {
        return plan != null && plan.getPrice() != null ? plan.getPrice() : 0;
    }

    private BigDecimal safeDiscount(MembershipPlan plan) {
        return plan != null && plan.getDiscountPercent() != null ? plan.getDiscountPercent() : BigDecimal.ZERO;
    }

    private void validateMembershipAssignment(User customer,
                                              MembershipPlan plan,
                                              String status,
                                              Integer currentMembershipId) {
        if (customer == null || !hasRole(customer, ROLE_CUSTOMER)) {
            throw new MembershipValidationException("membership.user.customer_required");
        }
        if (MembershipStatus.ACTIVE.name().equals(status)) {
            if (!PLAN_ACTIVE.equalsIgnoreCase(plan.getStatus())) {
                throw new MembershipValidationException("membership.plan.inactive");
            }
            if (userMembershipRepository.existsOtherActiveForUser(customer.getUserId(), currentMembershipId)) {
                throw new MembershipValidationException("membership.user.duplicate_active");
            }
        }
    }

    private MembershipPlan loadPlan(Integer planId) {
        if (planId == null) {
            throw MembershipNotFoundException.plan();
        }
        return membershipPlanRepository.findById(planId)
                .orElseThrow(MembershipNotFoundException::plan);
    }

    private UserMembership loadUserMembership(Integer membershipId) {
        if (membershipId == null) {
            throw MembershipNotFoundException.userMembership();
        }
        return userMembershipRepository.findByIdWithUserAndPlan(membershipId)
                .orElseThrow(MembershipNotFoundException::userMembership);
    }

    private User loadCustomer(Integer userId) {
        if (userId == null) {
            throw new MembershipValidationException("membership.user.required");
        }
        User user = userRepository.findByIdWithRoleAndBranch(userId)
                .orElseThrow(() -> new MembershipValidationException("membership.user.not_found"));
        if (!hasRole(user, ROLE_CUSTOMER)) {
            throw new MembershipValidationException("membership.user.customer_required");
        }
        return user;
    }

    private void assertAdmin(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new MembershipAccessDeniedException();
        }
        User loaded = userRepository.findByIdWithRoleAndBranch(actor.getUserId())
                .orElseThrow(MembershipAccessDeniedException::new);
        if (!hasRole(loaded, ROLE_ADMIN)) {
            throw new MembershipAccessDeniedException();
        }
    }

    private boolean hasRole(User user, String expected) {
        Role role = user == null ? null : user.getRole();
        return role != null && normalizeRole(role.getName()).equals(normalizeRole(expected));
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim();
        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }
        return normalized.replaceAll("[\\s_]+", "").toUpperCase();
    }

    private String normalizePlanStatus(String status) {
        String normalized = normalizeBlank(status);
        if (normalized == null) {
            return PLAN_ACTIVE;
        }
        normalized = normalized.toUpperCase();
        if (!PLAN_ACTIVE.equals(normalized) && !PLAN_INACTIVE.equals(normalized)) {
            throw new MembershipValidationException("membership.plan.status.invalid");
        }
        return normalized;
    }

    private String normalizeMembershipStatus(String status) {
        String normalized = MembershipStatus.normalize(status);
        if (normalized == null) {
            throw new MembershipValidationException("membership.user.status.invalid");
        }
        return normalized;
    }

    private MembershipPlanListItem toPlanListItem(MembershipPlan plan) {
        return MembershipPlanListItem.builder()
                .planId(plan.getPlanId())
                .name(plan.getName())
                .discountPercent(plan.getDiscountPercent())
                .price(plan.getPrice())
                .level(plan.getLevel())
                .status(plan.getStatus())
                .activeUserCount(activeUserCount(plan))
                .inUse(userMembershipRepository.existsByPlan_PlanId(plan.getPlanId()))
                .build();
    }

    private MembershipPlanDetailView toPlanDetail(MembershipPlan plan) {
        return MembershipPlanDetailView.builder()
                .planId(plan.getPlanId())
                .name(plan.getName())
                .discountPercent(plan.getDiscountPercent())
                .price(plan.getPrice())
                .level(plan.getLevel())
                .status(plan.getStatus())
                .activeUserCount(activeUserCount(plan))
                .inUse(userMembershipRepository.existsByPlan_PlanId(plan.getPlanId()))
                .build();
    }

    private long activeUserCount(MembershipPlan plan) {
        if (plan == null || plan.getPlanId() == null) {
            return 0L;
        }
        return userMembershipRepository.countByPlan_PlanIdAndStatusIgnoreCase(plan.getPlanId(), MembershipStatus.ACTIVE.name());
    }

    private MembershipPlanOption toPlanOption(MembershipPlan plan) {
        return MembershipPlanOption.builder()
                .planId(plan.getPlanId())
                .name(plan.getName())
                .build();
    }

    private UserMembershipListItem toUserMembershipListItem(UserMembership membership) {
        User user = membership.getUser();
        MembershipPlan plan = membership.getPlan();
        return UserMembershipListItem.builder()
                .membershipId(membership.getUserMembershipId())
                .userId(user != null ? user.getUserId() : null)
                .customerName(user != null ? user.getFullName() : "")
                .customerEmail(user != null ? user.getEmail() : "")
                .customerPhone(user != null ? user.getPhone() : "")
                .planId(plan != null ? plan.getPlanId() : null)
                .planName(plan != null ? plan.getName() : "")
                .discountPercent(plan != null ? plan.getDiscountPercent() : null)
                .status(membership.getStatus())
                .build();
    }

    private UserMembershipDetailView toUserMembershipDetail(UserMembership membership) {
        User user = membership.getUser();
        MembershipPlan plan = membership.getPlan();
        return UserMembershipDetailView.builder()
                .membershipId(membership.getUserMembershipId())
                .userId(user != null ? user.getUserId() : null)
                .customerName(user != null ? user.getFullName() : "")
                .customerEmail(user != null ? user.getEmail() : "")
                .customerPhone(user != null ? user.getPhone() : "")
                .planId(plan != null ? plan.getPlanId() : null)
                .planName(plan != null ? plan.getName() : "")
                .discountPercent(plan != null ? plan.getDiscountPercent() : null)
                .price(plan != null ? plan.getPrice() : null)
                .status(membership.getStatus())
                .build();
    }

    private static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static <T> boolean applyChange(T current, T next, java.util.function.Consumer<T> setter) {
        if (Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }
}
