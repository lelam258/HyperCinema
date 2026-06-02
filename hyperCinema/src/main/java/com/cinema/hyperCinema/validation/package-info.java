/**
 * Custom Bean Validation annotation và validator dùng chung cho toàn ứng dụng.
 *
 * <p>Phạm vi: chứa các annotation
 * {@code @Constraint(validatedBy = ...)} cùng class
 * {@link jakarta.validation.ConstraintValidator} đi kèm. Trong scope tính năng
 * Quản lý Chi nhánh, package này host {@code @BranchTimeRangeValid} +
 * {@code BranchTimeRangeValidator} — kiểm tra
 * {@code openingTime < closingTime} ở mức class-level cho cả
 * {@code BranchCreateRequest} và {@code BranchUpdateRequest} (REQ 3.6).</p>
 *
 * <p>Trong scope tính năng Quản lý Phim, package này bổ sung
 * {@code @MovieReleaseDateRangeValid} + {@code MovieReleaseDateRangeValidator} —
 * kiểm tra khoảng ngày phát hành của {@code MovieSearchCriteria} hợp lệ khi cả
 * hai cận {@code releaseDateFrom}/{@code releaseDateTo} cùng vắng mặt, hoặc cùng
 * có mặt và {@code from <= to} (REQ 4.8, 4.12). Constraint này được áp dụng tại
 * controller layer (soft check: bỏ qua filter + cảnh báo thay vì block 400).</p>
 *
 * <p>Quy ước:</p>
 * <ul>
 *   <li>Annotation đặt ở {@code @Target(ElementType.TYPE)} khi rule liên quan
 *       nhiều field; ở {@code FIELD} / {@code PARAMETER} khi chỉ áp một trường.</li>
 *   <li>Message default trả về key i18n (vd. {@code {branch.time_range.invalid}})
 *       để Thymeleaf resolve qua {@code messages.properties} /
 *       {@code messages_vi.properties}.</li>
 *   <li>Validator phải stateless và idempotent; không inject service layer.</li>
 * </ul>
 *
 * <p>Hỗ trợ trace requirement: REQ 3.6, 6.3 (đối xứng giữa create và update);
 * REQ 4.8, 4.12 (khoảng ngày phát hành trong tìm kiếm Phim).</p>
 */
package com.cinema.hyperCinema.validation;
