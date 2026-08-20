-- Warehouse product context API permissions (fix api_method + app_code + role mapping)
-- Idempotent — safe to re-run

INSERT INTO permission (id, code, name, api_method, api_path, action, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, v.code, v.name, v.api_method, v.api_path, v.action, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('WAREHOUSE_PRODUCT_CONTEXT_VIEW', '/warehouse/product/{productId}/context - VIEW', 'GET', '/warehouse/product/{productId}/context', 'VIEW'),
    ('WAREHOUSE_PRODUCT_STOCK_CHART_VIEW', '/warehouse/product/{productId}/stock-chart - VIEW', 'GET', '/warehouse/product/{productId}/stock-chart', 'VIEW'),
    ('WAREHOUSE_PRODUCT_STOCK_LEVELS_VIEW', '/warehouse/product/{productId}/stock-levels - VIEW', 'GET', '/warehouse/product/{productId}/stock-levels', 'VIEW'),
    ('WAREHOUSE_PRODUCT_REORDER_RULES_VIEW', '/warehouse/product/{productId}/reorder-rules - VIEW', 'GET', '/warehouse/product/{productId}/reorder-rules', 'VIEW')
) AS v(code, name, api_method, api_path, action)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = v.code);

UPDATE permission
SET app_code = COALESCE(app_code, 'QTHT'),
    api_method = COALESCE(api_method, 'GET'),
    updated_date = NOW(),
    updated_by = 'system'
WHERE code IN (
    'WAREHOUSE_PRODUCT_CONTEXT_VIEW',
    'WAREHOUSE_PRODUCT_STOCK_CHART_VIEW',
    'WAREHOUSE_PRODUCT_STOCK_LEVELS_VIEW',
    'WAREHOUSE_PRODUCT_REORDER_RULES_VIEW'
);

-- ADMIN + MANAGER: all QTHT permissions (matches role_permission_data.sql)
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, r.id, p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM roles r
CROSS JOIN permission p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND r.app_code = 'QTHT'
  AND (r.is_deleted = false OR r.is_deleted IS NULL)
  AND p.code IN (
      'WAREHOUSE_PRODUCT_CONTEXT_VIEW',
      'WAREHOUSE_PRODUCT_STOCK_CHART_VIEW',
      'WAREHOUSE_PRODUCT_STOCK_LEVELS_VIEW',
      'WAREHOUSE_PRODUCT_REORDER_RULES_VIEW'
  )
  AND p.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND (rp.is_deleted = false OR rp.is_deleted IS NULL)
  );

-- WAREHOUSE / PURCHASING / DELIVERY: warehouse product VIEW
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, r.id, p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM roles r
CROSS JOIN permission p
WHERE r.code IN ('WAREHOUSE', 'PURCHASING', 'DELIVERY')
  AND r.app_code = 'QTHT'
  AND (r.is_deleted = false OR r.is_deleted IS NULL)
  AND p.code IN (
      'WAREHOUSE_PRODUCT_CONTEXT_VIEW',
      'WAREHOUSE_PRODUCT_STOCK_CHART_VIEW',
      'WAREHOUSE_PRODUCT_STOCK_LEVELS_VIEW',
      'WAREHOUSE_PRODUCT_REORDER_RULES_VIEW'
  )
  AND p.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND (rp.is_deleted = false OR rp.is_deleted IS NULL)
  );

-- STAFF: VIEW on business modules (product warehouse context is not in deny list)
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, r.id, p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM roles r
CROSS JOIN permission p
WHERE r.code = 'STAFF'
  AND r.app_code = 'QTHT'
  AND (r.is_deleted = false OR r.is_deleted IS NULL)
  AND p.code IN (
      'WAREHOUSE_PRODUCT_CONTEXT_VIEW',
      'WAREHOUSE_PRODUCT_STOCK_CHART_VIEW',
      'WAREHOUSE_PRODUCT_STOCK_LEVELS_VIEW',
      'WAREHOUSE_PRODUCT_REORDER_RULES_VIEW'
  )
  AND p.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND (rp.is_deleted = false OR rp.is_deleted IS NULL)
  );
