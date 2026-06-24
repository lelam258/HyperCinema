-- =============================================================================
-- Migration: V2 - Create OrderItem table
-- Feature  : Order Management (Quản lý Đơn hàng)
-- Requirements: R1 (AC 1.5), R2 (AC 2.2), R3 (AC 3.4), R5 (AC 5.1, 5.4)
--
-- Bảng OrderItem lưu các dòng chi tiết của một Order, mỗi dòng đại diện cho
-- một loại sản phẩm: TICKET (vé), FOOD (đồ ăn/thức uống) hoặc COMBO (gói combo).
-- Liên kết tới Orders qua khóa ngoại order_id (ON DELETE CASCADE để xóa các
-- mục khi đơn hàng bị xóa). Schema theo design.md (Data Models / Database
-- Schema và mục Entity OrderItem).
--
-- Lưu ý: Các index hiệu năng có tên (idx_orderitem_order) được tạo ở migration
-- riêng (sub-task 1.8).
-- =============================================================================

CREATE TABLE `OrderItem` (
    `order_item_id` INT AUTO_INCREMENT PRIMARY KEY,
    `order_id`      INT NOT NULL,
    `item_type`     VARCHAR(20) NOT NULL,
    `item_name`     VARCHAR(255) NOT NULL,
    `reference_id`  INT NULL,
    `showtime_id`   INT NULL,
    `seat_id`       INT NULL,
    `unit_price`    BIGINT NOT NULL,
    `quantity`      INT NOT NULL DEFAULT 1,
    `line_total`    BIGINT NOT NULL,
    `combo_details` VARCHAR(1000) NULL,
    FOREIGN KEY (`order_id`) REFERENCES `Orders`(`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
