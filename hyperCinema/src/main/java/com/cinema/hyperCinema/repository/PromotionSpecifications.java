package com.cinema.hyperCinema.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.VoucherStatus;

import jakarta.persistence.criteria.Predicate;

public final class PromotionSpecifications {

    private PromotionSpecifications() {
    }

    public static Specification<Promotion> codeOrTitleContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("title")), pattern)
            );
        };
    }

    public static Specification<Promotion> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }

    public static Specification<Promotion> inBranchScope(User actor) {
        return (root, query, cb) -> {
            if (actor == null) {
                return cb.disjunction();
            }
            if (isAdmin(actor)) {
                return cb.conjunction();
            }
            if (isManager(actor) || isBranchManager(actor)) {
                Branch branch = actor.getBranch();
                if (branch == null || branch.getBranchId() == null) {
                    return cb.disjunction();
                }
                // matches the promotion's branch ID
                return cb.equal(root.get("branch").get("branchId"), branch.getBranchId());
            }
            return cb.disjunction();
        };
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
        if (user == null) {
            return false;
        }
        Role role = user.getRole();
        return role != null && normalizeRoleName(expected).equals(normalizeRoleName(role.getName()));
    }

    private static String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        String normalized = roleName.trim();
        return normalized.toLowerCase().replaceAll("[\\s_-]+", "");
    }
}
