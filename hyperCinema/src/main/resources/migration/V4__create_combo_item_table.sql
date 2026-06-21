-- =============================================================================
-- Migration: V4 - Create ComboItem table
-- Feature  : Order Management (Quản lý Đơn hàng)
-- Requirements: R3 (AC 3.3, 3.4)
--
-- Bảng ComboItem lưu các thành phần của một ComboPackage. Mỗi dòng đại diện
-- cho một thành phần combo: TICKET (vé) hoặc FOOD (đồ ăn/thức uống) kèm số
-- lượng. Liên kết tới ComboPackage qua khóa ngoại combo_id (ON DELETE CASCADE
-- để xóa các thành phần khi combo bị xóa).
--
-- food_item_id NULL khi thành phần là TICKET; khi là FOOD thì tham chiếu tới
-- mặt hàng F&B tương ứng. Schema theo design.md (mục Entity ComboItem).
--
-- Lưu ý: Các index hiệu năng có tên được tạo ở migration riêng (sub-task 1.8).
-- =============================================================================

CREATE TABLE `ComboItem` (
    `combo_item_id` INT AUTO_INCREMENT PRIMARY KEY,
    `combo_id`      INT NOT NULL,
    `item_type`     VARCHAR(20) NOT NULL,
    `food_item_id`  INT NULL,
    `quantity`      INT NOT NULL DEFAULT 1,
    FOREIGN KEY (`combo_id`) REFERENCES `ComboPackage`(`combo_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
