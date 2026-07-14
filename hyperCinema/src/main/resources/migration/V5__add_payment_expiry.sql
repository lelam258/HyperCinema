ALTER TABLE `payment`
    ADD COLUMN `expires_at` DATETIME NULL;

CREATE INDEX `idx_payment_status_expires`
    ON `payment` (`status`, `expires_at`);
