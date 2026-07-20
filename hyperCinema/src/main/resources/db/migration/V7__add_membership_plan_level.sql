ALTER TABLE membership_plan
    ADD COLUMN level INT NOT NULL DEFAULT 1;

SET @row_number = 0;
UPDATE membership_plan
SET level = (@row_number := @row_number + 1)
ORDER BY price ASC, discount_percent ASC, name ASC;
