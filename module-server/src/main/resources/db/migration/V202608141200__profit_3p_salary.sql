-- Lương 3P trên hợp đồng + thuế TNDN + auto tính KPI doanh thu

ALTER TABLE contract ADD COLUMN IF NOT EXISTS position_salary INTEGER;
ALTER TABLE contract ADD COLUMN IF NOT EXISTS person_allowance INTEGER;
ALTER TABLE contract ADD COLUMN IF NOT EXISTS kpi_bonus INTEGER;

-- Backfill: P1 = value hiện tại khi chưa có 3P
UPDATE contract
SET position_salary = value
WHERE position_salary IS NULL AND value IS NOT NULL;

UPDATE contract SET person_allowance = 0 WHERE person_allowance IS NULL;
UPDATE contract SET kpi_bonus = 0 WHERE kpi_bonus IS NULL;

ALTER TABLE profit_settings ADD COLUMN IF NOT EXISTS corporate_tax_pct DOUBLE PRECISION DEFAULT 20;

UPDATE profit_settings SET corporate_tax_pct = 20 WHERE corporate_tax_pct IS NULL;

ALTER TABLE profit_employee_allocation ADD COLUMN IF NOT EXISTS auto_calculate_target BOOLEAN DEFAULT TRUE;

UPDATE profit_employee_allocation SET auto_calculate_target = TRUE WHERE auto_calculate_target IS NULL;
