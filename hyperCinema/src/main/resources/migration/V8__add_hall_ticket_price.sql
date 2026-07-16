ALTER TABLE `hall`
    ADD COLUMN `ticket_price` INT NOT NULL DEFAULT 80000;

UPDATE `hall` h
SET h.`ticket_price` = COALESCE(
    (
        SELECT s.`price`
        FROM `showtime` s
        WHERE s.`hall_id` = h.`hall_id`
          AND s.`price` IS NOT NULL
          AND s.`price` > 0
        ORDER BY s.`start_time` DESC
        LIMIT 1
    ),
    h.`ticket_price`
);

UPDATE `booking`
SET `seat_subtotal` = CASE
        WHEN `order_subtotal` IS NOT NULL AND `order_subtotal` > 0 THEN `order_subtotal`
        ELSE `total_price`
    END
WHERE `seat_subtotal` IS NULL OR `seat_subtotal` = 0;
