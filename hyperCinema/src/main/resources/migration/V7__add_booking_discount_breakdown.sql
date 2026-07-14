ALTER TABLE `booking`
    ADD COLUMN `seat_subtotal` BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN `food_subtotal` BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN `order_subtotal` BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN `voucher_discount_amount` BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN `membership_discount_amount` BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN `membership_plan_name` VARCHAR(100) NULL,
    ADD COLUMN `membership_discount_percent` DECIMAL(5,2) NULL;

UPDATE `booking`
SET `order_subtotal` = `total_price`
WHERE `order_subtotal` = 0;
