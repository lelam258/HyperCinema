package com.cinema.hyperCinema.service.voucher;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherCreateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherSearchCriteria;
import com.cinema.hyperCinema.dto.admin.voucher.request.VoucherUpdateRequest;
import com.cinema.hyperCinema.dto.admin.voucher.response.UpdateResult;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherDetailView;
import com.cinema.hyperCinema.dto.admin.voucher.response.VoucherListItem;
import com.cinema.hyperCinema.model.User;

/**
 * Service quản trị voucher (Promotion): tìm kiếm, xem chi tiết, tạo, cập nhật,
 * xóa, đổi trạng thái và tự động đánh dấu voucher hết hạn.
 *
 * <p>Tuân theo actor-based authorization pattern: tham số {@code actor} là người
 * dùng đã xác thực thực hiện thao tác; service kiểm tra vai trò và phạm vi chi
 * nhánh trước khi thực thi.
 */
public interface VoucherService {

    /**
     * Tìm kiếm và lọc danh sách voucher theo tiêu chí, sắp xếp mặc định theo
     * {@code createdAt} giảm dần. Phạm vi kết quả được giới hạn theo vai trò/chi
     * nhánh của {@code actor}.
     *
     * @param criteria tiêu chí tìm kiếm (từ khóa, trạng thái, phân trang)
     * @param pageable thông tin phân trang/sắp xếp
     * @param actor    người dùng đang thực hiện thao tác
     * @return trang các {@link VoucherListItem}
     */
    Page<VoucherListItem> search(VoucherSearchCriteria criteria, Pageable pageable, User actor);

    /**
     * Lấy chi tiết một voucher theo id.
     *
     * @param voucherId id voucher
     * @param actor     người dùng đang thực hiện thao tác
     * @return chi tiết voucher
     */
    VoucherDetailView findById(Integer voucherId, User actor);

    /**
     * Tạo voucher mới. Voucher mới mặc định có trạng thái ACTIVE và usedCount = 0.
     *
     * @param request dữ liệu tạo voucher
     * @param actor   người dùng đang thực hiện thao tác
     * @return chi tiết voucher vừa tạo
     */
    VoucherDetailView create(VoucherCreateRequest request, User actor);

    /**
     * Cập nhật thông tin một voucher đang tồn tại.
     *
     * @param voucherId id voucher cần cập nhật
     * @param request   dữ liệu cập nhật
     * @param actor     người dùng đang thực hiện thao tác
     * @return kết quả cập nhật (có thay đổi hay không)
     */
    UpdateResult update(Integer voucherId, VoucherUpdateRequest request, User actor);

    /**
     * Xóa một voucher. Bị từ chối nếu voucher còn Active_Booking_Reference.
     *
     * @param voucherId id voucher cần xóa
     * @param actor     người dùng đang thực hiện thao tác
     */
    void delete(Integer voucherId, User actor);

    /**
     * Đổi trạng thái voucher thủ công (chỉ cho phép ACTIVE &lt;-&gt; INACTIVE).
     *
     * @param voucherId id voucher
     * @param newStatus trạng thái mới (ACTIVE | INACTIVE)
     * @param actor     người dùng đang thực hiện thao tác
     */
    void changeStatus(Integer voucherId, String newStatus, User actor);

    /**
     * Đánh dấu mọi voucher đang ACTIVE đã quá {@code endDate} thành EXPIRED.
     *
     * @param now thời điểm hiện tại dùng để so sánh
     * @return số bản ghi đã đổi trạng thái
     */
    int expireOverdueVouchers(LocalDateTime now);
}
