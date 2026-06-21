package com.cinema.hyperCinema.repository;

import com.cinema.hyperCinema.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho entity {@link User}, được mở rộng phục vụ module
 * Branch Management.
 *
 * <p>Các truy vấn manager / staff được dùng bởi {@code BranchValidator} và
 * {@code BranchServiceImpl} để kiểm tra ràng buộc và xây dựng view chi tiết
 * chi nhánh. Trace requirement: REQ 5.6, 8.3, 9.1, 9.4, 9.6 — xem
 * {@code .kiro/specs/branch-management/design.md} mục B.1.3.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    
//    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /**
     * Tìm User theo username kèm eager-load Role.
     *
     * <p>Dùng cho authentication flow để tránh LazyInitializationException
     * khi truy cập role.name ngoài Hibernate session.</p>
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    /**
     * Tìm User theo email kèm eager-load Role.
     *
     * <p>Dùng cho authentication flow để tránh LazyInitializationException
     * khi truy cập role.name ngoài Hibernate session.</p>
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Kiểm tra còn user nào đang gắn vào chi nhánh hay không.
     *
     * <p>Dùng cho luồng xoá cứng chi nhánh: chỉ cho phép xoá khi không có
     * Hall và không có User liên kết. Trace requirement: REQ 8.3.</p>
     *
     * @param branchId id chi nhánh cần kiểm tra
     * @return {@code true} nếu tồn tại user có {@code branch_id = branchId}
     */
    boolean existsByBranch_BranchId(Integer branchId);

    /**
     * Tìm Manager đang Active của một chi nhánh.
     *
     * <p>Filter {@code role.name = 'Manager' AND status = 'Active' AND
     * branch.branchId = :branchId}. Mỗi chi nhánh chỉ có tối đa một Manager
     * Active tại một thời điểm; bộ lọc {@code status = 'Active'} là cố ý —
     * REQ 9.4 chỉ chặn xung đột khi đã có Manager Active cho chi nhánh đó.
     * Trace requirement: REQ 9.4.</p>
     *
     * @param branchId id chi nhánh
     * @return {@link Optional} chứa Manager Active nếu có
     */
    @Query("SELECT u FROM User u "
            + "WHERE u.role.name = 'Manager' "
            + "AND u.status = 'Active' "
            + "AND u.branch.branchId = :branchId")
    Optional<User> findActiveManagerByBranchId(@Param("branchId") Integer branchId);

    /**
     * Liệt kê các Manager chưa được gán vào chi nhánh nào.
     *
     * <p>Filter {@code role.name = 'Manager' AND branch IS NULL}. Dùng để
     * dựng danh sách candidate cho màn hình "Gán Manager". Trace
     * requirement: REQ 9.1.</p>
     */
    @Query("SELECT u FROM User u "
            + "WHERE u.role.name = 'Manager' "
            + "AND u.branch IS NULL")
    List<User> findUnassignedManagers();

    @Query("SELECT u FROM User u "
            + "WHERE u.role.name = 'Staff' "
            + "AND u.branch IS NULL")
    List<User> findUnassignedStaff();

    @Query("SELECT u FROM User u "
            + "WHERE u.role.name = 'Manager' "
            + "AND u.branch.branchId = :branchId")
    List<User> findManagersByBranchId(@Param("branchId") Integer branchId);

    /**
     * Bỏ gán cột {@code manager_id} cho mọi staff đang trỏ tới manager này.
     *
     * <p>Bulk update phục vụ thao tác cascade khi unassign Manager khỏi chi
     * nhánh: mọi staff thuộc Manager đó sẽ có {@code manager = null}. Phải
     * được gọi trong transaction (service layer đảm bảo). Trace
     * requirement: REQ 9.6.</p>
     *
     * @param managerId id của Manager cần gỡ liên kết
     * @return số dòng bị ảnh hưởng
     */
    @Modifying
    @Query("UPDATE User u SET u.manager = null "
            + "WHERE u.manager.userId = :managerId")
    int clearManagerIdByManagerId(@Param("managerId") Integer managerId);

    /**
     * Tìm User theo chi nhánh và tên role.
     *
     * <p>Dùng để load danh sách Manager và Staff cho
     * {@code BranchDetailView}. Trace requirement: REQ 5.6.</p>
     *
     * @param branchId id chi nhánh
     * @param roleName tên role (ví dụ "Manager", "Staff")
     */
    @Query("SELECT u FROM User u "
            + "WHERE u.branch.branchId = :branchId "
            + "AND u.role.name = :roleName")
    List<User> findByBranchIdAndRoleName(@Param("branchId") Integer branchId,
                                         @Param("roleName") String roleName);

    // ── Dashboard aggregate queries ──

    long countByStatus(String status);

    @Query("SELECT u.role.name, COUNT(u) FROM User u GROUP BY u.role.name")
    List<Object[]> countUsersByRole();
}
