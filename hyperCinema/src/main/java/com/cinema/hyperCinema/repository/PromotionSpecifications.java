package com.cinema.hyperCinema.repository;

import org.springframework.data.jpa.domain.Specification;

import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;

import jakarta.persistence.criteria.Predicate;

/**
 * Specifications cho truy vấn {@link Promotion} (danh sách + tìm kiếm + lọc + phạm vi chi nhánh).
 *
 * <p>Đồng bộ với {@code MovieSpecifications}: các predicate được kết hợp bằng AND khi dùng cùng
 * {@code Specification.where(...).and(...)} ở tầng service.</p>
 */
public final class PromotionSpecifications {

    private PromotionSpecifications() {
    }

    /**
     * Khớp keyword trên {@code code} HOẶC {@code title} (không phân biệt hoa thường).
     * Trả về specification rỗng (luôn đúng) khi keyword null/blank.
     */
    public static Specification<Promotion> codeOrTitleContains(String keyword) {
        return (root, query, cb) -> {
            String normalized = trimToNull(keyword);
            if (normalized == null) {
                return cb.conjunction();
            }
            String pattern = "%" + normalized.toLowerCase() + "%";
            Predicate codeMatch = cb.like(cb.lower(root.get("code")), pattern);
            Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
            return cb.or(codeMatch, titleMatch);
        };
    }

    /**
     * Lọc theo {@code status}. Trả về specification rỗng (luôn đúng) khi status null/blank.
     */
    public static Specification<Promotion> hasStatus(String status) {
        return (root, query, cb) -> {
            String normalized = trimToNull(status);
            if (normalized == null) {
                return cb.equal(root.get("status"), "ACTIVE");
            }
            return cb.equal(root.get("status"), normalized);
        };
    }

    /**
     * Phạm vi hiển thị theo vai trò người dùng:
     * <ul>
     *   <li>Administrator: không ràng buộc (xem mọi voucher).</li>
     *   <li>Manager / BranchManager: chỉ voucher thuộc chi nhánh của họ HOẶC voucher toàn hệ thống
     *       ({@code branch_id IS NULL}).</li>
     * </ul>
     */
    public static Specification<Promotion> inBranchScope(User actor) {
        return (root, query, cb) -> {
            if (actor == null || isAdmin(actor)) {
                return cb.conjunction();
            }
            Integer branchId = actor.getBranch() == null ? null : actor.getBranch().getBranchId();
            Predicate systemWide = cb.isNull(root.get("branch"));
            if (branchId == null) {
                // Manager chưa gán chi nhánh: chỉ thấy voucher toàn hệ thống.
                return systemWide;
            }
            Predicate ownBranch = cb.equal(root.get("branch").get("branchId"), branchId);
            return cb.or(ownBranch, systemWide);
        };
    }

    private static boolean isAdmin(User user) {
        Role role = user.getRole();
        if (role == null) {
            return false;
        }
        String normalized = normalizeRoleName(role.getName());
        return "ADMIN".equals(normalized) || "ADMINISTRATOR".equals(normalized);
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
