package com.cinema.hyperCinema.security.guard;

import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Method-level authorization guard cho quản lý voucher, dùng trong các biểu thức
 * {@code @PreAuthorize} ở route chi tiết/sửa/xóa của {@code VoucherController},
 * ví dụ: {@code @PreAuthorize("@voucherAccessGuard.canManage(authentication, #voucherId)")}.
 *
 * <p>Tuân theo đúng mẫu của {@link MovieAccessGuard}/{@link BranchAccessGuard}:
 * bean được đặt tên ({@code voucherAccessGuard}), nạp {@link User} từ
 * {@link UserRepository} qua {@code username} rồi {@code email}, và bọc mọi lỗi
 * runtime để trả {@code false} thay vì ném ngoại lệ trong tầng security.
 *
 * <p>Phân quyền (Requirement 7.1, 7.2, 7.3):
 * <ul>
 *   <li>Administrator: luôn {@code true} (mọi chi nhánh).</li>
 *   <li>Manager / BranchManager: {@code true} khi voucher thuộc đúng chi nhánh
 *       của người dùng ({@code promotion.branch.branchId == user.branch.branchId}).</li>
 *   <li>Vai trò khác: {@code false}.</li>
 * </ul>
 */
@Component("voucherAccessGuard")
@RequiredArgsConstructor
@Slf4j
public class VoucherAccessGuard {

    private final UserRepository userRepository;
    private final PromotionRepository promotionRepository;

    public boolean canManage(Authentication authentication, Integer voucherId) {
        if (voucherId == null) {
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

            // Administrator: toàn quyền trên mọi chi nhánh (Req 7.1). Trả true sớm để
            // 404 (voucher không tồn tại) được tầng service/controller xử lý, không bị
            // che thành 403.
            if (isAdmin(user)) {
                return true;
            }

            // Manager / BranchManager: chỉ quản lý voucher thuộc chi nhánh mình (Req 7.2, 7.3).
            if (!isManager(user) && !isBranchManager(user)) {
                return false;
            }

            Branch userBranch = user.getBranch();
            if (userBranch == null || userBranch.getBranchId() == null) {
                return false;
            }

            // Voucher không tồn tại → không thiết lập được phạm vi chi nhánh → false
            // (đồng bộ hành vi của MovieAccessGuard khi không có liên kết hợp lệ).
            Optional<Promotion> voucherOpt = promotionRepository.findById(voucherId);
            if (voucherOpt.isEmpty()) {
                return false;
            }

            Branch voucherBranch = voucherOpt.get().getBranch();
            if (voucherBranch == null || voucherBranch.getBranchId() == null) {
                // Voucher toàn hệ thống (branch == null) không thuộc chi nhánh nào của
                // Branch_Manager để quản lý (Req 7.2).
                return false;
            }

            return userBranch.getBranchId().equals(voucherBranch.getBranchId());
        } catch (RuntimeException ex) {
            log.warn("VoucherAccessGuard.canManage failed for principal '{}', voucherId={}: {}",
                    principalName, voucherId, ex.toString());
            return false;
        }
    }

    // ----------------------------------------------------------------------
    // Role normalization helpers (đồng bộ với VoucherServiceImpl/MovieServiceImpl)
    // ----------------------------------------------------------------------

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
        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }
        return normalized.replaceAll("[\\s_]+", "").toUpperCase();
    }
}
