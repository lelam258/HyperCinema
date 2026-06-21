-- =============================================================================
-- Migration: V1 - Create Orders table
-- Feature  : Order Management (Quản lý Đơn hàng)
-- Requirements: R1 (AC 1.1, 1.4), R3 (AC 3.1, 3.4), R5 (AC 5.1, 5.2),
--               R6 (AC 6.3-6.5)
--
-- Bảng Orders là thực thể trung tâm của module Order Management, liên kết
-- Booking (vé), FoodOrder (đồ ăn) và Payment (thanh toán). Schema theo
-- design.md (Data Models / Database Schema).
--
-- Lưu ý: Các index hiệu năng (idx_order_code, idx_order_user,
-- idx_order_status_expires) được tạo ở migration riêng (sub-task 1.8).
-- =============================================================================

CREATE TABLE `Orders` (
    `order_id`        INT AUTO_INCREMENT PRIMARY KEY,
    `order_code`      VARCHAR(20) NOT NULL UNIQUE,
    `user_id`         INT NULL,
    `branch_id`       INT NOT NULL,
    `status`          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `subtotal`        BIGINT NOT NULL DEFAULT 0,
    `discount_amount` BIGINT NOT NULL DEFAULT 0,
    `total_amount`    BIGINT NOT NULL DEFAULT 0,
    `promotion_id`    INT NULL,
    `expires_at`      DATETIME NULL,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NULL,
    FOREIGN KEY (`user_id`)      REFERENCES `User`(`user_id`),
    FOREIGN KEY (`branch_id`)    REFERENCES `Branch`(`branch_id`),
    FOREIGN KEY (`promotion_id`) REFERENCES `Promotion`(`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
