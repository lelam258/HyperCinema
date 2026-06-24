package com.cinema.hyperCinema.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.service.voucher.VoucherService;

import lombok.RequiredArgsConstructor;

/**
 * Tác vụ định kỳ tự động đánh dấu các voucher đã quá {@code endDate} thành
 * EXPIRED.
 *
 * <p>Chạy mỗi 15 phút (cron {@code 0 *&#47;15 * * * *}) và ủy quyền cho
 * {@link VoucherService#expireOverdueVouchers(LocalDateTime)} (Requirement 6.1).
 * Cơ chế lập lịch được bật qua {@code SchedulingConfig} ({@code @EnableScheduling}).
 */
@Component
@RequiredArgsConstructor
public class VoucherExpiryScheduler {

    private final VoucherService voucherService;

    @Scheduled(cron = "0 */15 * * * *")
    public void expireOverdueVouchers() {
        voucherService.expireOverdueVouchers(LocalDateTime.now());
    }
}
