package com.cinema.hyperCinema.service.voucher;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherCreateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherSearchCriteria;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherUpdateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.response.UpdateResult;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherDetailView;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherListItem;
import com.cinema.hyperCinema.exception.voucher.VoucherAccessDeniedException;
import com.cinema.hyperCinema.exception.voucher.VoucherNotFoundException;
import com.cinema.hyperCinema.exception.voucher.VoucherValidationException;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.model.DiscountType;
import com.cinema.hyperCinema.model.Promotion;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.model.VoucherStatus;
import com.cinema.hyperCinema.repository.BookingRepository;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.PromotionRepository;
import com.cinema.hyperCinema.repository.PromotionSpecifications;
import com.cinema.hyperCinema.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Triển khai {@link VoucherService} cho quản trị voucher (Promotion).
 *
 * <p>Tuân theo actor-based authorization pattern đồng bộ với
 * {@code MovieServiceImpl}/{@code HallServiceImpl}: mỗi thao tác nạp lại
 * {@link User} actor đầy đủ qua {@link UserRepository} (để có role + branch),
 * sau đó kiểm tra phạm vi quản lý qua {@link #assertCanManage(User, Promotion)}.
 *
 * <p>Các method nghiệp vụ ({@code search}, {@code findById}, {@code create},
 * {@code update}, {@code delete}, {@code changeStatus},
 * {@code expireOverdueVouchers}) được scaffold ở task 6.2 và sẽ được hiện thực
 * đầy đủ ở các task 6.3-6.6.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final PromotionRepository promotionRepository;
    private final BranchRepository branchRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherListItem> search(VoucherSearchCriteria criteria, Pageable pageable, User actor) {
        // Chuẩn hóa tiêu chí (trim keyword, chuẩn hóa status/sort/direction/paging).
        VoucherSearchCriteria normalized =
                (criteria == null ? new VoucherSearchCriteria() : criteria).normalize();

        // Nạp lại actor đầy đủ để có role + branch phục vụ phạm vi chi nhánh.
        User loadedActor = loadActor(actor);

        // Kết hợp các specification: tìm kiếm theo keyword + lọc trạng thái + phạm vi chi nhánh.
        Specification<Promotion> spec = Specification
                .where(PromotionSpecifications.codeOrTitleContains(normalized.getKeyword()))
                .and(PromotionSpecifications.hasStatus(normalized.getStatus()))
                .and(PromotionSpecifications.inBranchScope(loadedActor));

        // Sắp xếp: mặc định createdAt DESC, tôn trọng sort/direction trong tiêu chí nếu được đặt.
        Sort sort = Sort.by(Sort.Direction.fromString(normalized.getDirection()), normalized.getSort());
        Pageable effective = resolvePageable(pageable, normalized, sort);

        LocalDateTime now = LocalDateTime.now();
        // Page rỗng (không có voucher) được trả nguyên trạng (Page.empty) — controller hiển thị thông báo trống.
        return promotionRepository.findAll(spec, effective)
                .map(voucher -> toListItem(voucher, now));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherDetailView findById(Integer voucherId, User actor) {
        User loadedActor = loadActor(actor);
        Promotion voucher = promotionRepository.findById(voucherId)
                .orElseThrow(() -> new VoucherNotFoundException(voucherId));
        assertCanManage(loadedActor, voucher);
        return toDetailView(voucher);
    }

    /**
     * Quyết định {@link Pageable} hiệu lực: dùng pageable từ controller nếu nó đã chỉ định sort;
     * nếu không thì áp dụng sort suy ra từ tiêu chí (mặc định createdAt DESC).
     */
    private static Pageable resolvePageable(Pageable pageable, VoucherSearchCriteria criteria, Sort sort) {
        if (pageable == null) {
            return PageRequest.of(criteria.getPage(), criteria.getSize(), sort);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /**
     * Map {@link Promotion} sang {@link VoucherListItem}, đặt cờ {@code expired} = trạng thái EXPIRED
     * HOẶC đã quá {@code endDate} so với thời điểm hiện tại (chỉ báo trực quan — Requirement 6.3).
     */
    private static VoucherListItem toListItem(Promotion voucher, LocalDateTime now) {
        boolean expired = VoucherStatus.EXPIRED.name().equals(voucher.getStatus())
                || (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now));
        return VoucherListItem.builder()
                .voucherId(voucher.getPromotionId())
                .code(voucher.getCode())
                .title(voucher.getTitle())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .usedCount(voucher.getUsedCount())
                .maxUses(voucher.getMaxUses())
                .status(voucher.getStatus())
                .expired(expired)
                .build();
    }

    /**
     * Map {@link Promotion} sang {@link VoucherDetailView}, kèm thông tin chi nhánh (nếu có).
     */
    private static VoucherDetailView toDetailView(Promotion voucher) {
        Branch branch = voucher.getBranch();
        return VoucherDetailView.builder()
                .voucherId(voucher.getPromotionId())
                .title(voucher.getTitle())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrderValue(voucher.getMinOrderValue())
                .maxUses(voucher.getMaxUses())
                .usedCount(voucher.getUsedCount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .branchSpecific(Boolean.TRUE.equals(voucher.getBranchSpecific()))
                .branchId(branch == null ? null : branch.getBranchId())
                .branchName(branch == null ? null : branch.getName())
                .status(voucher.getStatus())
                .build();
    }

    @Override
    public VoucherDetailView create(VoucherCreateRequest request, User actor) {
        // Nạp lại actor đầy đủ (role + branch) phục vụ kiểm tra phạm vi quản lý.
        User loadedActor = loadActor(actor);

        // 1) Xác thực nghiệp vụ các trường đầu vào (Req 2.3-2.8).
        validateBusinessRules(
                request.getCode(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.getMaxUses(),
                request.getMinOrderValue());

        // 2) Trùng code (không phân biệt hoa thường) — Req 2.2.
        if (promotionRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new VoucherValidationException("voucher.code.duplicate");
        }

        // 3) Dựng entity từ request; mặc định status=ACTIVE, usedCount=0 (do @PrePersist đảm bảo).
        Promotion voucher = new Promotion();
        voucher.setTitle(request.getTitle());
        voucher.setCode(request.getCode().trim());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setMaxUses(request.getMaxUses());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setBranchSpecific(request.isBranchSpecific());
        voucher.setUsedCount(0);
        voucher.setStatus(VoucherStatus.ACTIVE.name());

        // 4) Gắn chi nhánh khi branchSpecific (Req 2.9); ngược lại voucher toàn hệ thống (Req 7.5).
        if (request.isBranchSpecific()) {
            if (request.getBranchId() == null) {
                throw new VoucherValidationException("voucher.branch_required");
            }
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new VoucherValidationException("voucher.branch.not_found"));
            ensureBranchActive(branch);
            voucher.setBranch(branch);
        } else {
            voucher.setBranch(null);
        }

        // 5) Phân quyền theo chi nhánh: Manager chỉ tạo được voucher thuộc chi nhánh mình;
        //    Admin tạo được mọi chi nhánh và voucher toàn hệ thống (Req 7.1, 7.2, 7.5).
        assertCanManage(loadedActor, voucher);

        Promotion saved = promotionRepository.save(voucher);
        return toDetailView(saved);
    }

    /**
     * Xác thực các ràng buộc nghiệp vụ dùng chung cho {@code create} (task 6.4) và
     * {@code update} (task 6.5). Ném {@link VoucherValidationException} với i18n key
     * tương ứng ở vi phạm đầu tiên phát hiện được.
     *
     * <ul>
     *   <li>{@code code} rỗng hoặc > 50 ký tự → {@code voucher.code.invalid} (Req 2.3)</li>
     *   <li>{@code discountType} không thuộc {PERCENTAGE, FIXED_AMOUNT} → {@code voucher.discount_type.invalid} (Req 2.5)</li>
     *   <li>{@code discountValue <= 0} → {@code voucher.discount_value.invalid} (Req 2.4)</li>
     *   <li>{@code discountType=PERCENTAGE} và {@code discountValue > 100} → {@code voucher.discount_percentage.invalid} (Req 2.5)</li>
     *   <li>{@code endDate <= startDate} → {@code voucher.date_range.invalid} (Req 2.6)</li>
     *   <li>{@code maxUses < 1} → {@code voucher.max_uses.invalid} (Req 2.7)</li>
     *   <li>{@code minOrderValue < 0} → {@code voucher.min_order_value.invalid} (Req 2.8)</li>
     * </ul>
     */
    void validateBusinessRules(String code, String discountType, Integer discountValue,
            LocalDateTime startDate, LocalDateTime endDate, Integer maxUses, Integer minOrderValue) {
        // Code: không rỗng, tối đa 50 ký tự.
        if (code == null || code.trim().isEmpty() || code.trim().length() > 50) {
            throw new VoucherValidationException("voucher.code.invalid");
        }

        // Discount type: phải thuộc {PERCENTAGE, FIXED_AMOUNT}.
        DiscountType parsedType = parseDiscountType(discountType);
        if (parsedType == null) {
            throw new VoucherValidationException("voucher.discount_type.invalid");
        }

        // Discount value: > 0.
        if (discountValue == null || discountValue <= 0) {
            throw new VoucherValidationException("voucher.discount_value.invalid");
        }

        // Percentage: 1..100.
        if (parsedType == DiscountType.PERCENTAGE && discountValue > 100) {
            throw new VoucherValidationException("voucher.discount_percentage.invalid");
        }

        // Khoảng ngày: endDate phải sau startDate.
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new VoucherValidationException("voucher.date_range.invalid");
        }

        // Max uses: >= 1.
        if (maxUses == null || maxUses < 1) {
            throw new VoucherValidationException("voucher.max_uses.invalid");
        }

        // Min order value: >= 0.
        if (minOrderValue == null || minOrderValue < 0) {
            throw new VoucherValidationException("voucher.min_order_value.invalid");
        }
    }

    /**
     * Phân giải {@code discountType} thành {@link DiscountType}; trả {@code null} nếu không hợp lệ.
     */
    private static DiscountType parseDiscountType(String discountType) {
        if (discountType == null) {
            return null;
        }
        try {
            return DiscountType.valueOf(discountType.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public UpdateResult update(Integer voucherId, VoucherUpdateRequest request, User actor) {
        // Nạp lại actor đầy đủ (role + branch) phục vụ kiểm tra phạm vi quản lý.
        User loadedActor = loadActor(actor);

        // 1) Load voucher đang tồn tại — Req 3.3.
        Promotion voucher = promotionRepository.findById(voucherId)
                .orElseThrow(() -> new VoucherNotFoundException(voucherId));

        // 2) Phân quyền: Manager chỉ sửa được voucher thuộc chi nhánh mình; Admin mọi voucher (Req 7.x).
        assertCanManage(loadedActor, voucher);

        // 3) Xác thực nghiệp vụ các trường đầu vào — Req 3.4.
        validateBusinessRules(
                request.getCode(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.getMaxUses(),
                request.getMinOrderValue());

        // 4) Trùng code (không phân biệt hoa thường), loại trừ chính voucher đang sửa — Req 3.2.
        String normalizedCode = request.getCode().trim();
        if (promotionRepository.existsByCodeIgnoreCaseAndPromotionIdNot(normalizedCode, voucherId)) {
            throw new VoucherValidationException("voucher.code.duplicate");
        }

        // 5) Nếu request có status thì chỉ chấp nhận ACTIVE/INACTIVE; từ chối EXPIRED thủ công — Req 3.5.
        String requestedStatus = request.getStatus() == null ? null : request.getStatus().trim();
        if (requestedStatus != null && !requestedStatus.isEmpty()) {
            if (!VoucherStatus.ACTIVE.name().equals(requestedStatus)
                    && !VoucherStatus.INACTIVE.name().equals(requestedStatus)) {
                throw new VoucherValidationException("voucher.status.invalid");
            }
        }

        // 6) Áp dụng thay đổi, theo dõi xem có trường nào thực sự đổi (hasChanges).
        boolean hasChanges = false;

        hasChanges |= applyChange(voucher.getTitle(), request.getTitle(), voucher::setTitle);
        hasChanges |= applyChange(voucher.getCode(), normalizedCode, voucher::setCode);
        hasChanges |= applyChange(voucher.getDiscountType(), request.getDiscountType(), voucher::setDiscountType);
        hasChanges |= applyChange(voucher.getDiscountValue(), request.getDiscountValue(), voucher::setDiscountValue);
        hasChanges |= applyChange(voucher.getMinOrderValue(), request.getMinOrderValue(), voucher::setMinOrderValue);
        hasChanges |= applyChange(voucher.getMaxUses(), request.getMaxUses(), voucher::setMaxUses);
        hasChanges |= applyChange(voucher.getStartDate(), request.getStartDate(), voucher::setStartDate);
        hasChanges |= applyChange(voucher.getEndDate(), request.getEndDate(), voucher::setEndDate);

        // branchSpecific + phân giải chi nhánh (giống create).
        boolean newBranchSpecific = request.isBranchSpecific();
        hasChanges |= applyChange(Boolean.TRUE.equals(voucher.getBranchSpecific()), newBranchSpecific,
                voucher::setBranchSpecific);
        if (newBranchSpecific) {
            if (request.getBranchId() == null) {
                throw new VoucherValidationException("voucher.branch_required");
            }
            Integer currentBranchId = branchId(voucher.getBranch());
            if (!request.getBranchId().equals(currentBranchId)) {
                Branch branch = branchRepository.findById(request.getBranchId())
                        .orElseThrow(() -> new VoucherValidationException("voucher.branch.not_found"));
                ensureBranchActive(branch);
                voucher.setBranch(branch);
                hasChanges = true;
            }
            // Sau khi gắn chi nhánh, đảm bảo actor vẫn có quyền quản lý voucher ở chi nhánh mới.
            assertCanManage(loadedActor, voucher);
        } else if (voucher.getBranch() != null) {
            voucher.setBranch(null);
            hasChanges = true;
        }

        // Cập nhật status nếu được cung cấp.
        if (requestedStatus != null && !requestedStatus.isEmpty()) {
            hasChanges |= applyChange(voucher.getStatus(), requestedStatus, voucher::setStatus);
        }

        promotionRepository.save(voucher);
        return UpdateResult.builder()
                .hasChanges(hasChanges)
                .voucherId(voucherId)
                .build();
    }

    /**
     * Đặt giá trị mới qua {@code setter} nếu khác giá trị hiện tại; trả {@code true} khi có thay đổi.
     */
    private static <T> boolean applyChange(T current, T next, java.util.function.Consumer<T> setter) {
        if (java.util.Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    @Override
    public void delete(Integer voucherId, User actor) {
        // Nạp lại actor đầy đủ (role + branch) phục vụ kiểm tra phạm vi quản lý.
        User loadedActor = loadActor(actor);

        // 1) Load voucher đang tồn tại — Req 4.3.
        Promotion voucher = promotionRepository.findById(voucherId)
                .orElseThrow(() -> new VoucherNotFoundException(voucherId));

        // 2) Phân quyền: Manager chỉ xóa được voucher thuộc chi nhánh mình; Admin mọi voucher (Req 7.x).
        assertCanManage(loadedActor, voucher);

        voucher.setStatus(VoucherStatus.INACTIVE.name());
        promotionRepository.save(voucher);
    }

    @Override
    public void changeStatus(Integer voucherId, String newStatus, User actor) {
        // Nạp lại actor đầy đủ (role + branch) phục vụ kiểm tra phạm vi quản lý.
        User loadedActor = loadActor(actor);

        // Load voucher đang tồn tại — Req 3.3.
        Promotion voucher = promotionRepository.findById(voucherId)
                .orElseThrow(() -> new VoucherNotFoundException(voucherId));

        // Phân quyền: Manager chỉ thao tác voucher thuộc chi nhánh mình; Admin mọi voucher.
        assertCanManage(loadedActor, voucher);

        // Chỉ cho phép đổi sang ACTIVE hoặc INACTIVE; từ chối mọi giá trị khác kể cả EXPIRED — Req 3.5.
        String normalizedStatus = newStatus == null ? null : newStatus.trim();
        if (normalizedStatus == null
                || (!VoucherStatus.ACTIVE.name().equals(normalizedStatus)
                        && !VoucherStatus.INACTIVE.name().equals(normalizedStatus))) {
            throw new VoucherValidationException("voucher.status.invalid");
        }

        voucher.setStatus(normalizedStatus);
        promotionRepository.save(voucher);
    }

    @Override
    public int expireOverdueVouchers(LocalDateTime now) {
        // Tìm các voucher ACTIVE đã quá end_date so với thời điểm now — Req 6.1.
        List<Promotion> overdue =
                promotionRepository.findByStatusAndEndDateBefore(VoucherStatus.ACTIVE.name(), now);

        if (overdue.isEmpty()) {
            return 0;
        }

        // Chuyển sang EXPIRED và lưu lại; trả về số bản ghi đã đổi.
        for (Promotion voucher : overdue) {
            voucher.setStatus(VoucherStatus.EXPIRED.name());
        }
        promotionRepository.saveAll(overdue);
        return overdue.size();
    }

    // ----------------------------------------------------------------------
    // Actor-based authorization helpers (task 6.2)
    // ----------------------------------------------------------------------

    /**
     * Nạp lại {@link User} actor đầy đủ từ {@link UserRepository} để có role và
     * branch (actor truyền vào từ Security thường là entity nông/lazy).
     *
     * @param actor actor đã xác thực truyền từ controller
     * @return entity {@link User} đầy đủ
     * @throws VoucherAccessDeniedException nếu actor null hoặc không tồn tại
     */
    User loadActor(User actor) {
        if (actor == null || actor.getUserId() == null) {
            throw new VoucherAccessDeniedException();
        }
        return userRepository.findById(actor.getUserId())
                .orElseThrow(VoucherAccessDeniedException::new);
    }

    /**
     * Kiểm tra actor có quyền quản lý voucher hay không theo actor-based pattern.
     *
     * <ul>
     *   <li>Administrator: luôn được phép (mọi chi nhánh).</li>
     *   <li>Manager / BranchManager: chỉ được phép khi chi nhánh của voucher
     *       trùng với chi nhánh của actor.</li>
     *   <li>Vai trò khác: bị từ chối.</li>
     * </ul>
     *
     * @param actor   actor đã được nạp đầy đủ qua {@link #loadActor(User)}
     * @param voucher voucher cần thao tác
     * @throws VoucherAccessDeniedException nếu actor không có quyền quản lý voucher
     */
    void assertCanManage(User actor, Promotion voucher) {
        if (isAdmin(actor)) {
            return;
        }
        if (isManager(actor) || isBranchManager(actor)) {
            Integer actorBranchId = branchId(actor.getBranch());
            Integer voucherBranchId = voucher == null ? null : branchId(voucher.getBranch());
            if (actorBranchId != null && actorBranchId.equals(voucherBranchId)) {
                return;
            }
            throw new VoucherAccessDeniedException();
        }
        throw new VoucherAccessDeniedException();
    }

    private static Integer branchId(Branch branch) {
        return branch == null ? null : branch.getBranchId();
    }

    private static void ensureBranchActive(Branch branch) {
        if (branch == null || !"Active".equalsIgnoreCase(branch.getStatus())) {
            throw new VoucherValidationException("voucher.branch.inactive");
        }
    }

    // ----------------------------------------------------------------------
    // Role normalization helpers (đồng bộ với MovieServiceImpl/HallServiceImpl)
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
