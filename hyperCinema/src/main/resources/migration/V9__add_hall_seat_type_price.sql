CREATE TABLE `hall_seat_type_price` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `hall_id` INT NOT NULL,
    `seat_type` VARCHAR(20) NOT NULL,
    `price` INT NOT NULL,
    `active` BIT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hall_seat_type_price` (`hall_id`, `seat_type`),
    CONSTRAINT `fk_hall_seat_type_price_hall`
        FOREIGN KEY (`hall_id`) REFERENCES `hall` (`hall_id`)
);

INSERT INTO `hall_seat_type_price` (`hall_id`, `seat_type`, `price`, `active`)
SELECT h.`hall_id`, t.`seat_type`,
       CASE WHEN t.`seat_type` = 'DISABLED' THEN 0 ELSE COALESCE(h.`ticket_price`, 80000) END,
       1
FROM `hall` h
CROSS JOIN (
    SELECT 'STANDARD' AS `seat_type`
    UNION ALL SELECT 'VIP'
    UNION ALL SELECT 'COUPLE'
    UNION ALL SELECT 'DISABLED'
) t;

UPDATE `seat`
SET `seat_type` = CASE
    WHEN UPPER(TRIM(`seat_type`)) = 'DOUBLE' THEN 'COUPLE'
    WHEN UPPER(TRIM(`seat_type`)) = 'STANDARD' THEN 'STANDARD'
    WHEN UPPER(TRIM(`seat_type`)) = 'VIP' THEN 'VIP'
    WHEN UPPER(TRIM(`seat_type`)) = 'COUPLE' THEN 'COUPLE'
    WHEN UPPER(TRIM(`seat_type`)) = 'DISABLED' THEN 'DISABLED'
    ELSE 'STANDARD'
END;
