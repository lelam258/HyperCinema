package com.cinema.hyperCinema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật cơ chế lập lịch (Spring Scheduling) cho ứng dụng.
 *
 * <p>Tách riêng khỏi lớp khởi động {@code HyperCinemaApplication} để giữ lớp
 * chính tối giản, đồng thời cho phép kích hoạt/vô hiệu hóa scheduling độc lập
 * trong kiểm thử nếu cần. Cần thiết cho {@code VoucherExpiryScheduler} tự động
 * đánh dấu voucher hết hạn (Requirement 6.1).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
