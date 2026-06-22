-- =============================================================================
-- Migration: V3 - Create ComboPackage table
-- Feature  : Order Management (Quản lý Đơn hàng)
-- Requirements: R3 (AC 3.1, 3.4, 3.6)
--
-- Bảng ComboPackage lưu các gói combo (vé + đồ ăn hoặc nhiều đồ ăn) được bán
-- với giá ưu đãi dưới dạng một gói. Mỗi combo có giá combo (price) và giá gốc
-- khi mua riêng lẻ (original_price) để tính mức tiết kiệm hiển thị cho khách.
--
-- branch_id NULL nghĩa là combo áp dụng toàn hệ thống (system-wide); nếu có
-- giá trị thì combo chỉ áp dụng cho chi nhánh tương ứng. Schema theo design.md
-- (Data Models / Database Schema và mục Entity ComboPackage).
--
-- Lưu ý: Các index hiệu năng có tên được tạo ở migration riêng (sub-task 1.8).
-- =============================================================================

CREATE TABLE `ComboPackage` (
    `combo_id`       INT AUTO_INCREMENT PRIMARY KEY,
    `name`           VARCHAR(150) NOT NULL,
    `description`    VARCHAR(500) NULL,
    `price`          BIGINT NOT NULL,
    `original_price` BIGINT NOT NULL,
    `is_active`      TINYINT(1) NOT NULL DEFAULT 1,
    `branch_id`      INT NULL,
    `image_url`      VARCHAR(500) NULL,
    FOREIGN KEY (`branch_id`) REFERENCES `Branch`(`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
