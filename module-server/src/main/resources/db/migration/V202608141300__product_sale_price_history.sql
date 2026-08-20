-- Product sale price history table + API permissions
-- Idempotent — safe to re-run

CREATE TABLE IF NOT EXISTS product_sale_price_history (
    id              VARCHAR(36) PRIMARY KEY,
    product_id      VARCHAR(36) NOT NULL,
    old_price       DOUBLE PRECISION,
    new_price       DOUBLE PRECISION NOT NULL,
    note            VARCHAR(500),
    is_deleted      BOOLEAN DEFAULT false,
    created_date    TIMESTAMP,
    created_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_product_sale_price_history_product
    ON product_sale_price_history (product_id, created_date DESC)
    WHERE COALESCE(is_deleted, false) = false;

INSERT INTO permission (id, code, name, api_method, api_path, action, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, v.code, v.name, v.api_method, v.api_path, v.action, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('PRODUCT_ID_SALE_PRICE_UPDATE', '/product/{id}/sale-price - UPDATE', 'POST', '/product/{id}/sale-price', 'UPDATE'),
    ('PRODUCT_ID_SALE_PRICE_HISTORY_VIEW', '/product/{id}/sale-price-history - VIEW', 'GET', '/product/{id}/sale-price-history', 'VIEW')
) AS v(code, name, api_method, api_path, action)
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = v.code);

-- ADMIN + MANAGER: full product sale price access
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, r.id, p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM roles r
CROSS JOIN permission p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND r.app_code = 'QTHT'
  AND (r.is_deleted = false OR r.is_deleted IS NULL)
  AND p.code IN ('PRODUCT_ID_SALE_PRICE_UPDATE', 'PRODUCT_ID_SALE_PRICE_HISTORY_VIEW')
  AND p.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND (rp.is_deleted = false OR rp.is_deleted IS NULL)
  );

-- STAFF: view history + update sale price (same as other product catalog ops)
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid()::text, r.id, p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM roles r
CROSS JOIN permission p
WHERE r.code = 'STAFF'
  AND r.app_code = 'QTHT'
  AND (r.is_deleted = false OR r.is_deleted IS NULL)
  AND p.code IN ('PRODUCT_ID_SALE_PRICE_UPDATE', 'PRODUCT_ID_SALE_PRICE_HISTORY_VIEW')
  AND p.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id AND (rp.is_deleted = false OR rp.is_deleted IS NULL)
  );
