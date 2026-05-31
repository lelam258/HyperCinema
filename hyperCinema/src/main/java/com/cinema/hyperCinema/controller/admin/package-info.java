/**
 * Web controllers cho khu vực quản trị (Admin) của HyperCinema.
 *
 * <p>Phạm vi: chứa các Spring MVC {@code @Controller} điều phối request HTTP
 * dưới prefix {@code /admin/**}, ví dụ {@code BranchController} cho tính năng
 * Quản lý Chi nhánh. Các controller ở đây chịu trách nhiệm:</p>
 * <ul>
 *   <li>Bind &amp; validate DTO request (Bean Validation).</li>
 *   <li>Uỷ quyền nghiệp vụ cho service layer (vd. {@code BranchService}).</li>
 *   <li>Áp phân quyền method-level qua {@code @PreAuthorize} cho các rule
 *       phụ thuộc ngữ cảnh (REQ 11).</li>
 *   <li>Trả về Thymeleaf view hoặc redirect kèm flash attribute.</li>
 * </ul>
 *
 * <p>Hỗ trợ trace requirement: REQ 2, 4, 5, 6, 7, 8, 9, 10, 11
 * (xem {@code .kiro/specs/branch-management/requirements.md}).</p>
 */
package com.cinema.hyperCinema.controller.admin;
