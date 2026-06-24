/**
 * Audit logging contracts và implementation cho các module nghiệp vụ.
 *
 * <p>Phạm vi: chứa các interface và component chịu trách nhiệm ghi
 * {@code AuditLog} cho các thao tác mutation. Trong scope tính năng
 * Quản lý Chi nhánh, package này host {@code BranchAuditLogger} và
 * {@code BranchAuditLoggerImpl} — ghi vết hành động {@code CREATE},
 * {@code UPDATE}, {@code STATUS_CHANGE}, {@code ASSIGN_MANAGER},
 * {@code UNASSIGN_MANAGER}, {@code ASSIGN_STAFF}, {@code UNASSIGN_STAFF}.</p>
 *
 * <p>Quy ước thực thi:</p>
 * <ul>
 *   <li>Mỗi method ghi audit annotate
 *       {@code @Transactional(propagation = Propagation.REQUIRES_NEW)} để cô lập
 *       lỗi audit khỏi business transaction (REQ 12.6).</li>
 *   <li>Field {@code entity_type} cố định {@code "Branch"} cho module này;
 *       {@code details} là JSON theo định dạng quy định ở {@code design.md} mục B.7.3.</li>
 *   <li>Service caller phải bọc lời gọi audit trong helper {@code auditSafe(...)}
 *       để downgrade mọi {@code Throwable} thành {@code log.warn} (REQ 12.6, 12.7).</li>
 * </ul>
 *
 * <p>Hỗ trợ trace requirement: REQ 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7
 * (xem {@code .kiro/specs/branch-management/requirements.md}).</p>
 */
package com.cinema.hyperCinema.service.audit;
