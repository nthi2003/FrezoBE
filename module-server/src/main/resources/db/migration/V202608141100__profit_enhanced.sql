-- Profit module Phase 2: cost centers, employee allocation, revenue gap dashboard
-- Idempotent — safe to re-run

ALTER TABLE profit_settings ADD COLUMN IF NOT EXISTS default_cogs_pct DOUBLE PRECISION DEFAULT 55;
ALTER TABLE profit_settings ADD COLUMN IF NOT EXISTS sales_commission_pct DOUBLE PRECISION DEFAULT 3;
ALTER TABLE profit_settings ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'VND';

UPDATE profit_settings SET default_cogs_pct = 55 WHERE default_cogs_pct IS NULL;
UPDATE profit_settings SET sales_commission_pct = 3 WHERE sales_commission_pct IS NULL;
UPDATE profit_settings SET currency = 'VND' WHERE currency IS NULL OR currency = '';

CREATE TABLE IF NOT EXISTS profit_cost_category (
    id              VARCHAR(36) PRIMARY KEY,
    code            VARCHAR(40) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    name_en         VARCHAR(200),
    cost_type       VARCHAR(20) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    market_code     VARCHAR(10) NOT NULL DEFAULT 'VN',
    sort_order      INTEGER DEFAULT 0,
    active          BOOLEAN DEFAULT true,
    note            VARCHAR(1000),
    is_deleted      BOOLEAN DEFAULT false,
    created_date    TIMESTAMP,
    created_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS profit_fixed_overhead (
    id              VARCHAR(36) PRIMARY KEY,
    category_id     VARCHAR(36) NOT NULL,
    year            INTEGER NOT NULL,
    month           INTEGER NOT NULL,
    amount          DOUBLE PRECISION NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    note            VARCHAR(500),
    is_deleted      BOOLEAN DEFAULT false,
    created_date    TIMESTAMP,
    created_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS profit_employee_allocation (
    id                      VARCHAR(36) PRIMARY KEY,
    person_id               VARCHAR(36) NOT NULL,
    contract_id             VARCHAR(36),
    cost_category_code      VARCHAR(40) NOT NULL,
    monthly_salary          DOUBLE PRECISION,
    use_payroll_source      BOOLEAN DEFAULT false,
    revenue_target_monthly  DOUBLE PRECISION,
    commission_pct          DOUBLE PRECISION,
    currency                VARCHAR(3) NOT NULL DEFAULT 'VND',
    active                  BOOLEAN DEFAULT true,
    note                    VARCHAR(500),
    is_deleted              BOOLEAN DEFAULT false,
    created_date            TIMESTAMP,
    created_by              VARCHAR(50),
    updated_date            TIMESTAMP,
    updated_by              VARCHAR(50),
    deleted_at              TIMESTAMP,
    deleted_by              VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS profit_analysis_snapshot (
    id              VARCHAR(36) PRIMARY KEY,
    year            INTEGER NOT NULL,
    month           INTEGER NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    calculated_at   TIMESTAMP NOT NULL,
    calculated_by   VARCHAR(50),
    input_json      TEXT,
    result_json     TEXT,
    is_deleted      BOOLEAN DEFAULT false,
    created_date    TIMESTAMP,
    created_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_profit_fixed_oh_period ON profit_fixed_overhead (year, month, category_id);
CREATE INDEX IF NOT EXISTS idx_profit_emp_alloc_person ON profit_employee_allocation (person_id);
CREATE INDEX IF NOT EXISTS idx_profit_snapshot_period ON profit_analysis_snapshot (year, month);

-- Seed VN default cost categories
INSERT INTO profit_cost_category (id, code, name, name_en, cost_type, currency, market_code, sort_order, active, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT v.id, v.code, v.name, v.name_en, v.cost_type, 'VND', 'VN', v.sort_order, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('profit-cat-sales-var', 'SALES_VARIABLE', 'Nhân viên kinh doanh', 'Sales staff', 'VARIABLE', 1),
    ('profit-cat-fixed-it', 'FIXED_IT', 'IT / Hệ thống', 'IT / Systems', 'FIXED', 2),
    ('profit-cat-fixed-log', 'FIXED_LOGISTICS', 'Vận chuyển', 'Logistics', 'FIXED', 3),
    ('profit-cat-fixed-qc', 'FIXED_QC', 'Kiểm định / QC', 'Quality control', 'FIXED', 4),
    ('profit-cat-fixed-admin', 'FIXED_ADMIN', 'Hành chính', 'Administration', 'FIXED', 5),
    ('profit-cat-var-risk', 'VARIABLE_RISK', 'Chi phí rủi ro', 'Risk costs', 'VARIABLE', 6),
    ('profit-cat-var-comm', 'VARIABLE_COMMISSION', 'Hoa hồng bán hàng', 'Sales commission', 'VARIABLE', 7)
) AS v(id, code, name, name_en, cost_type, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM profit_cost_category c WHERE c.id = v.id);

-- Menus Phase 2
INSERT INTO menu (id, code, name, name_en, fe_url, folder_path, parent_code, app_code, order_index, icon, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, 'PROFIT_DASHBOARD', 'Dashboard hòa vốn', 'Break-even Dashboard', '/profit/dashboard', 'src/modules/profit', 'MENU_PROFIT', 'QTHT', 3, 'BarChart3', true, false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE code = 'PROFIT_DASHBOARD' AND app_code = 'QTHT');

INSERT INTO menu (id, code, name, name_en, fe_url, folder_path, parent_code, app_code, order_index, icon, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, 'PROFIT_COST_STRUCTURE', 'Cơ cấu chi phí', 'Cost Structure', '/profit/cost-structure', 'src/modules/profit', 'MENU_PROFIT', 'QTHT', 4, 'Layers', true, false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE code = 'PROFIT_COST_STRUCTURE' AND app_code = 'QTHT');

-- Permissions Phase 2
INSERT INTO permission (id, code, name, method, api_path, action, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, v.code, v.name, v.method, v.api_path, v.action, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('PROFIT_DASHBOARD_VIEW', '/profit/dashboard - VIEW', 'GET', '/profit/dashboard', 'VIEW'),
    ('PROFIT_SALES_KPI_VIEW', '/profit/dashboard/sales-kpi - VIEW', 'GET', '/profit/dashboard/sales-kpi', 'VIEW'),
    ('PROFIT_COST_CAT_VIEW', '/profit/cost-structure/categories - VIEW', 'GET', '/profit/cost-structure/categories', 'VIEW'),
    ('PROFIT_COST_CAT_CREATE', '/profit/cost-structure/categories - CREATE', 'POST', '/profit/cost-structure/categories', 'CREATE'),
    ('PROFIT_COST_CAT_UPDATE', '/profit/cost-structure/categories/{id} - UPDATE', 'PUT', '/profit/cost-structure/categories/{id}', 'UPDATE'),
    ('PROFIT_COST_CAT_DELETE', '/profit/cost-structure/categories/{id} - DELETE', 'DELETE', '/profit/cost-structure/categories/{id}', 'DELETE'),
    ('PROFIT_FIXED_OH_VIEW', '/profit/cost-structure/fixed-overhead - VIEW', 'GET', '/profit/cost-structure/fixed-overhead', 'VIEW'),
    ('PROFIT_FIXED_OH_CREATE', '/profit/cost-structure/fixed-overhead - CREATE', 'POST', '/profit/cost-structure/fixed-overhead', 'CREATE'),
    ('PROFIT_FIXED_OH_DELETE', '/profit/cost-structure/fixed-overhead/{id} - DELETE', 'DELETE', '/profit/cost-structure/fixed-overhead/{id}', 'DELETE'),
    ('PROFIT_EMP_ALLOC_VIEW', '/profit/cost-structure/employee-allocations - VIEW', 'GET', '/profit/cost-structure/employee-allocations', 'VIEW'),
    ('PROFIT_EMP_ALLOC_CREATE', '/profit/cost-structure/employee-allocations - CREATE', 'POST', '/profit/cost-structure/employee-allocations', 'CREATE'),
    ('PROFIT_EMP_ALLOC_UPDATE', '/profit/cost-structure/employee-allocations/{id} - UPDATE', 'PUT', '/profit/cost-structure/employee-allocations/{id}', 'UPDATE'),
    ('PROFIT_EMP_ALLOC_DELETE', '/profit/cost-structure/employee-allocations/{id} - DELETE', 'DELETE', '/profit/cost-structure/employee-allocations/{id}', 'DELETE')
) AS v(code, name, method, api_path, action)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = v.code);
