CREATE TABLE `weekend_ticket_pricing` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `hall_id` INT NOT NULL,
    `name` VARCHAR(100) NOT NULL DEFAULT 'Weekend ticket pricing',
    `days_of_week` VARCHAR(80) NOT NULL DEFAULT 'SATURDAY,SUNDAY',
    `standard_price` INT NOT NULL DEFAULT 0,
    `vip_price` INT NOT NULL DEFAULT 0,
    `couple_price` INT NOT NULL DEFAULT 0,
    `disabled_price` INT NOT NULL DEFAULT 0,
    `active` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NULL,
    `updated_at` DATETIME NULL,
    UNIQUE KEY `uk_weekend_ticket_pricing_hall` (`hall_id`),
    CONSTRAINT `fk_weekend_ticket_pricing_hall`
        FOREIGN KEY (`hall_id`) REFERENCES `hall` (`hall_id`)
);

ALTER TABLE `ticket`
    ADD COLUMN `unit_price` BIGINT NULL;
