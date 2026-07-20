ALTER TABLE `membership_plan`
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE `membership_plan`
    MODIFY COLUMN `duration_days` INT NOT NULL DEFAULT 0;

ALTER TABLE `user_membership`
    MODIFY COLUMN `start_date` DATE NULL,
    MODIFY COLUMN `end_date` DATE NULL;
