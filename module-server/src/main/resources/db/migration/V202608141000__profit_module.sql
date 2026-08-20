-- Profit module MVP: settings table + menu + permissions + default global config
-- Idempotent — safe to re-run

CREATE TABLE IF NOT EXISTS profit_settings (
    id                          VARCHAR(36) PRIMARY KEY,
    scope                       VARCHAR(20) NOT NULL,
    scope_ref_id                VARCHAR(36),
    name                        VARCHAR(200),
    profit_margin_pct           DOUBLE PRECISION DEFAULT 20,
    risk_cost_pct               DOUBLE PRECISION DEFAULT 2,
    labor_cost_per_unit         DOUBLE PRECISION DEFAULT 0,
    fixed_monthly_overhead      DOUBLE PRECISION DEFAULT 0,
    monthly_sales_target_units  DOUBLE PRECISION DEFAULT 1000,
    active                      BOOLEAN DEFAULT true,
    note                        VARCHAR(1000),
    is_deleted                  BOOLEAN DEFAULT false,
    created_date                TIMESTAMP,
    created_by                  VARCHAR(50),
    updated_date                TIMESTAMP,
    updated_by                  VARCHAR(50),
    deleted_at                  TIMESTAMP,
    deleted_by                  VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_profit_settings_scope
    ON profit_settings (scope, COALESCE(scope_ref_id, ''))
    WHERE COALESCE(is_deleted, false) = false;

INSERT INTO profit_settings (
    id, scope, scope_ref_id, name,
    profit_margin_pct, risk_cost_pct, labor_cost_per_unit,
    fixed_monthly_overhead, monthly_sales_target_units,
    active, note, is_deleted, created_date, created_by, updated_date, updated_by
)
SELECT
    'profit-setting-global-default',
    'GLOBAL',
    NULL,
    'Cấu hình mặc định',
    20, 2, 5000,
    50000000, 10000,
    true,
    'MVP — chỉnh trong Quản lý lợi nhuận',
    false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM profit_settings
    WHERE scope = 'GLOBAL' AND scope_ref_id IS NULL AND COALESCE(is_deleted, false) = false
);

INSERT INTO menu (id, code, name, name_en, fe_url, folder_path, parent_code, app_code, order_index, icon, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid()::text,
    'MENU_PROFIT',
    'Quản lý lợi nhuận',
    'Profit Management',
    '/profit',
    'src/modules/profit',
    NULL,
    'QTHT',
    45,
    'TrendingUp',
    true,
    false,
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE code = 'MENU_PROFIT' AND app_code = 'QTHT');

INSERT INTO menu (id, code, name, name_en, fe_url, folder_path, parent_code, app_code, order_index, icon, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid()::text,
    'PROFIT_SETTINGS',
    'Cấu hình chi phí',
    'Cost Settings',
    '/profit/settings',
    'src/modules/profit',
    'MENU_PROFIT',
    'QTHT',
    1,
    'Settings',
    true,
    false,
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE code = 'PROFIT_SETTINGS' AND app_code = 'QTHT');

INSERT INTO menu (id, code, name, name_en, fe_url, folder_path, parent_code, app_code, order_index, icon, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid()::text,
    'PROFIT_SIMULATOR',
    'Mô phỏng lợi nhuận',
    'Profit Simulator',
    '/profit/simulator',
    'src/modules/profit',
    'MENU_PROFIT',
    'QTHT',
    2,
    'Calculator',
    true,
    false,
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE code = 'PROFIT_SIMULATOR' AND app_code = 'QTHT');

INSERT INTO permission (id, code, name, api_method, api_path, action, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, v.code, v.name, v.api_method, v.api_path, v.action, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('WAREHOUSE_PRODUCT_CONTEXT_VIEW', '/warehouse/product/{productId}/context - VIEW', 'GET', '/warehouse/product/{productId}/context', 'VIEW'),
    ('WAREHOUSE_PRODUCT_STOCK_CHART_VIEW', '/warehouse/product/{productId}/stock-chart - VIEW', 'GET', '/warehouse/product/{productId}/stock-chart', 'VIEW'),
    ('WAREHOUSE_PRODUCT_STOCK_LEVELS_VIEW', '/warehouse/product/{productId}/stock-levels - VIEW', 'GET', '/warehouse/product/{productId}/stock-levels', 'VIEW'),
    ('WAREHOUSE_PRODUCT_REORDER_RULES_VIEW', '/warehouse/product/{productId}/reorder-rules - VIEW', 'GET', '/warehouse/product/{productId}/reorder-rules', 'VIEW'),
    ('PROFIT_SETTINGS_VIEW', '/profit/settings - VIEW', 'GET', '/profit/settings', 'VIEW'),
    ('PROFIT_SETTINGS_ID_VIEW', '/profit/settings/{id} - VIEW', 'GET', '/profit/settings/{id}', 'VIEW'),
    ('PROFIT_SETTINGS_RESOLVE_VIEW', '/profit/settings/resolve/{productId} - VIEW', 'GET', '/profit/settings/resolve/{productId}', 'VIEW'),
    ('PROFIT_SETTINGS_CREATE', '/profit/settings - CREATE', 'POST', '/profit/settings', 'CREATE'),
    ('PROFIT_SETTINGS_ID_UPDATE', '/profit/settings/{id} - UPDATE', 'PUT', '/profit/settings/{id}', 'UPDATE'),
    ('PROFIT_SETTINGS_ID_DELETE', '/profit/settings/{id} - DELETE', 'DELETE', '/profit/settings/{id}', 'DELETE'),
    ('PROFIT_CALCULATE_VIEW', '/profit/calculate - VIEW', 'POST', '/profit/calculate', 'VIEW')
) AS v(code, name, api_method, api_path, action)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = v.code);
