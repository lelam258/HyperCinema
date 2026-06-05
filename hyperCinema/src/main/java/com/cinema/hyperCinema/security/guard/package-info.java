/**
 * Authorization guard component cho các rule phân quyền phụ thuộc ngữ cảnh
 * không thể biểu diễn bằng path matcher trong {@code SecurityConfig}.
 *
 * <p>Phạm vi: host các Spring bean (vd. {@code BranchAccessGuard}) được tham chiếu
 * trực tiếp trong biểu thức SpEL của {@code @PreAuthorize} ở controller — ví dụ:</p>
 *
 * <pre>{@code
 * @PreAuthorize("hasRole('Admin') or "
 *             + "(hasRole('Manager') and @branchAccessGuard.canRead(authentication, #branchId))")
 * }</pre>
 *
 * <p>Quy ước:</p>
 * <ul>
 *   <li>Bean name dạng camelCase trùng tên class để dùng được trong SpEL
 *       ({@code @branchAccessGuard}).</li>
 *   <li>Method guard không ném exception cho rule "deny"; trả về {@code boolean}
 *       để Spring Security sinh 403 đồng nhất.</li>
 *   <li>Truy vấn dữ liệu phụ thuộc qua repository được inject; không gọi service
 *       layer để tránh vòng lặp transaction.</li>
 * </ul>
 *
 * <p>Hỗ trợ trace requirement: REQ 5.2, 5.3, 11.3, 11.4
 * (xem {@code .kiro/specs/branch-management/requirements.md}).</p>
 */
package com.cinema.hyperCinema.security.guard;
