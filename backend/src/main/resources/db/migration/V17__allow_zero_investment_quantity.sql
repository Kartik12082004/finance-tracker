ALTER TABLE investments
DROP CONSTRAINT chk_investments_quantity_positive;

ALTER TABLE investments
ADD CONSTRAINT chk_investments_quantity_non_negative
CHECK (quantity IS NULL OR quantity >= 0);
