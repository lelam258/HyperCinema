ALTER TABLE `food_order`
    MODIFY COLUMN `booking_id` INT NULL,
    ADD COLUMN `branch_id` INT NULL AFTER `booking_id`,
    ADD COLUMN `staff_user_id` INT NULL AFTER `branch_id`,
    ADD COLUMN `customer_phone` VARCHAR(30) NULL AFTER `staff_user_id`,
    ADD COLUMN `payment_method` VARCHAR(20) NULL AFTER `customer_phone`,
    ADD COLUMN `payment_status` VARCHAR(20) NULL AFTER `payment_method`;
