-- ============================================================
-- SCRIPT: Role ↔ Permission mapping
--   - ADMIN   : full toàn bộ permission
--   - MANAGER : tất cả trừ các permission "hệ thống nhạy cảm"
--               (qtht/role, qtht/permission, qtht/menu, qtht/user CREATE/UPDATE/DELETE,
--                qtht/setting UPDATE, qtht/ip-* CREATE/DELETE, dmdc/* DELETE)
--   - STAFF   : chỉ VIEW ở tất cả module business + task/CRUD trên task cá nhân
-- Created: 2026-07-16 (Batch I3)
-- Updated: 2026-08-04 — prefix match cho per-endpoint api_path
-- IDEMPOTENT: NOT EXISTS trên (role_id, permission_id)
-- ============================================================

-- Helper note: after per-endpoint split, filters use:
--   LTRIM(api_path,'/') = 'x' OR LTRIM(api_path,'/') LIKE 'x/%'
-- so both legacy entity paths and /x/{id}/action rows are covered.

-- ============================================================
-- 1) ADMIN → FULL
-- ============================================================
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    (SELECT id FROM roles WHERE code = 'ADMIN' AND app_code = 'QTHT' LIMIT 1),
    p.id,
    'QTHT',
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM permission p
WHERE p.is_deleted = false
  AND p.app_code = 'QTHT'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'ADMIN'
        AND rp.permission_id = p.id
  );

-- ============================================================
-- 2) MANAGER → tất cả trừ hệ thống nhạy cảm
-- ============================================================
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    (SELECT id FROM roles WHERE code = 'MANAGER' AND app_code = 'QTHT' LIMIT 1),
    p.id,
    'QTHT',
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM permission p
WHERE p.is_deleted = false
  AND p.app_code = 'QTHT'
  -- Loại các permission siêu nhạy cảm (prefix-aware)
  AND NOT (
        (LTRIM(p.api_path, '/') = 'qtht/role' OR LTRIM(p.api_path, '/') LIKE 'qtht/role/%')
    AND p.action IN ('CREATE','UPDATE','DELETE')
  )
  AND NOT (
        (LTRIM(p.api_path, '/') = 'qtht/permission' OR LTRIM(p.api_path, '/') LIKE 'qtht/permission/%')
    AND p.action IN ('CREATE','UPDATE','DELETE')
  )
  AND NOT (
        (LTRIM(p.api_path, '/') = 'qtht/menu' OR LTRIM(p.api_path, '/') LIKE 'qtht/menu/%')
    AND p.action IN ('CREATE','UPDATE','DELETE')
  )
  AND NOT (
        (LTRIM(p.api_path, '/') = 'qtht/user' OR LTRIM(p.api_path, '/') LIKE 'qtht/user/%')
    AND p.action IN ('CREATE','DELETE')
  )
  AND NOT (
        (LTRIM(p.api_path, '/') = 'qtht/setting' OR LTRIM(p.api_path, '/') LIKE 'qtht/setting/%')
    AND p.action = 'UPDATE'
  )
  AND NOT (LTRIM(p.api_path, '/') LIKE 'qtht/ip-%' AND p.action IN ('CREATE','DELETE'))
  AND NOT (LTRIM(p.api_path, '/') LIKE 'dmdc/%' AND p.action = 'DELETE')
  AND NOT (LTRIM(p.api_path, '/') = 'qtht/audit-log' OR LTRIM(p.api_path, '/') LIKE 'qtht/audit-log/%')
  AND NOT (
        (LTRIM(p.api_path, '/') = 'qtht/api-log' OR LTRIM(p.api_path, '/') LIKE 'qtht/api-log/%')
    AND p.action = 'DELETE'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'MANAGER'
        AND rp.permission_id = p.id
  );

-- ============================================================
-- 3) STAFF → chỉ VIEW + một số CRUD task/attendance/leave cá nhân
-- ============================================================
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    (SELECT id FROM roles WHERE code = 'STAFF' AND app_code = 'QTHT' LIMIT 1),
    p.id,
    'QTHT',
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM permission p
WHERE p.is_deleted = false
  AND p.app_code = 'QTHT'
  AND (
        -- 1) VIEW ở phần lớn module (deny list by path prefix)
        (p.action = 'VIEW' AND NOT (
             LTRIM(p.api_path, '/') = 'qtht/role' OR LTRIM(p.api_path, '/') LIKE 'qtht/role/%'
          OR LTRIM(p.api_path, '/') = 'qtht/permission' OR LTRIM(p.api_path, '/') LIKE 'qtht/permission/%'
          OR LTRIM(p.api_path, '/') = 'qtht/menu' OR LTRIM(p.api_path, '/') LIKE 'qtht/menu/%'
          OR LTRIM(p.api_path, '/') = 'qtht/user' OR LTRIM(p.api_path, '/') LIKE 'qtht/user/%'
          OR LTRIM(p.api_path, '/') = 'qtht/setting' OR LTRIM(p.api_path, '/') LIKE 'qtht/setting/%'
          OR LTRIM(p.api_path, '/') = 'qtht/audit-log' OR LTRIM(p.api_path, '/') LIKE 'qtht/audit-log/%'
          OR LTRIM(p.api_path, '/') = 'qtht/api-log' OR LTRIM(p.api_path, '/') LIKE 'qtht/api-log/%'
          OR LTRIM(p.api_path, '/') = 'qtht/usage' OR LTRIM(p.api_path, '/') LIKE 'qtht/usage/%'
          OR LTRIM(p.api_path, '/') LIKE 'qtht/ip-%'
          OR LTRIM(p.api_path, '/') = 'qtht/dashboard' OR LTRIM(p.api_path, '/') LIKE 'qtht/dashboard/%'
          OR LTRIM(p.api_path, '/') LIKE 'cms/customer%'
          OR LTRIM(p.api_path, '/') LIKE 'cms/order%'
          OR LTRIM(p.api_path, '/') LIKE 'cms/voucher%'
          OR LTRIM(p.api_path, '/') LIKE 'fb/account%'
          OR LTRIM(p.api_path, '/') LIKE 'fb/group%'
          OR LTRIM(p.api_path, '/') LIKE 'fb/lead%'
          OR LTRIM(p.api_path, '/') LIKE 'fb/automation%'
          OR LTRIM(p.api_path, '/') LIKE 'fb/accounts%'
          OR LTRIM(p.api_path, '/') LIKE 'fb/groups%'
          OR LTRIM(p.api_path, '/') LIKE 'fb/leads%'
          OR LTRIM(p.api_path, '/') = 'asset/depreciation' OR LTRIM(p.api_path, '/') LIKE 'asset/depreciation/%'
        ))
        -- 2) Task cá nhân: được CRUD task/ticket
        OR (
              (LTRIM(p.api_path, '/') = 'task/task' OR LTRIM(p.api_path, '/') LIKE 'task/task/%'
            OR LTRIM(p.api_path, '/') = 'task/ticket' OR LTRIM(p.api_path, '/') LIKE 'task/ticket/%')
          AND p.action IN ('CREATE','UPDATE')
        )
        -- 3) Attendance cá nhân: CREATE/UPDATE
        OR (
              (LTRIM(p.api_path, '/') = 'qlns/attendance' OR LTRIM(p.api_path, '/') LIKE 'qlns/attendance/%')
          AND p.action IN ('CREATE','UPDATE')
        )
        -- 4) Leave request cá nhân: CREATE
        OR (
              (LTRIM(p.api_path, '/') = 'qlns/leave' OR LTRIM(p.api_path, '/') LIKE 'qlns/leave/%'
            OR LTRIM(p.api_path, '/') = 'qlns/leave-request' OR LTRIM(p.api_path, '/') LIKE 'qlns/leave-request/%')
          AND p.action = 'CREATE'
        )
        -- 4b) Recognition: VIEW + gift/redeem CREATE (không APPROVE)
        OR (
              (LTRIM(p.api_path, '/') = 'qlns/recognition' OR LTRIM(p.api_path, '/') LIKE 'qlns/recognition/%')
          AND p.action IN ('VIEW','CREATE')
        )
        -- 5) Notification cá nhân
        OR (
              (LTRIM(p.api_path, '/') = 'qtht/notification' OR LTRIM(p.api_path, '/') LIKE 'qtht/notification/%')
          AND p.action IN ('VIEW','UPDATE')
        )
        -- 6) Cart cá nhân
        OR (
              (LTRIM(p.api_path, '/') = 'product/cart' OR LTRIM(p.api_path, '/') LIKE 'product/cart/%')
          AND p.action IN ('CREATE','DELETE')
        )
        -- 7) QTBV Articles: Content Writer tạo nháp + gửi duyệt (không publish/review)
        OR (p.api_path = '/qtbv/articles' AND p.action IN ('CREATE','UPDATE'))
        OR (p.api_path = '/qtbv/articles/{id}' AND p.action = 'UPDATE')
        OR (p.api_path = '/qtbv/articles/{id}/submit' AND p.action = 'UPDATE')
        OR (p.code IN ('QTBV_ARTICLES_CREATE','QTBV_ARTICLES_UPDATE','QTBV_ARTICLES_SUBMIT_UPDATE','QTBV_ARTICLES_ID_SUBMIT_UPDATE'))
  )
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'STAFF'
        AND rp.permission_id = p.id
  );

-- Revoke STAFF KPI dashboard permissions if previously granted (HOME portal không cần)
UPDATE role_permission rp
SET is_deleted = true,
    updated_date = NOW(),
    updated_by = 'system'
FROM roles r, permission p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND r.code = 'STAFF'
  AND r.app_code = 'QTHT'
  AND (LTRIM(p.api_path, '/') = 'qtht/dashboard' OR LTRIM(p.api_path, '/') LIKE 'qtht/dashboard/%')
  AND (rp.is_deleted IS DISTINCT FROM true);

-- ============================================================
-- 4) SME ops roles — API permission (menu đã scope; API vẫn cần code)
-- ============================================================

-- PURCHASING: warehouse + product + customer/ncc + approvals (no accounting)
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM roles WHERE code = 'PURCHASING' AND app_code = 'QTHT' LIMIT 1),
       p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM permission p
WHERE p.is_deleted = false AND p.app_code = 'QTHT'
  AND (
        LTRIM(p.api_path, '/') LIKE 'warehouse/%'
     OR LTRIM(p.api_path, '/') = 'warehouse'
     OR LTRIM(p.api_path, '/') LIKE 'product/%'
     OR LTRIM(p.api_path, '/') = 'product'
     OR LTRIM(p.api_path, '/') LIKE 'customer/%'
     OR LTRIM(p.api_path, '/') = 'customer'
     OR LTRIM(p.api_path, '/') LIKE 'ncc/%'
     OR LTRIM(p.api_path, '/') = 'ncc'
     OR p.api_path IN ('/approvals', '/approval-flows')
     OR LTRIM(p.api_path, '/') LIKE 'approvals/%'
     OR LTRIM(p.api_path, '/') LIKE 'approval-flows/%'
     OR p.code LIKE 'APPROVALS_%'
     OR p.code LIKE 'APPROVAL_FLOWS_%'
     OR (
           (LTRIM(p.api_path, '/') = 'qtht/notification' OR LTRIM(p.api_path, '/') LIKE 'qtht/notification/%')
       AND p.action IN ('VIEW','UPDATE')
     )
  )
  AND NOT (p.code LIKE 'ACCOUNTING_%')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'PURCHASING' AND rp.permission_id = p.id
  );

-- WAREHOUSE: full warehouse + product VIEW + approvals VIEW/APPROVE
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM roles WHERE code = 'WAREHOUSE' AND app_code = 'QTHT' LIMIT 1),
       p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM permission p
WHERE p.is_deleted = false AND p.app_code = 'QTHT'
  AND (
        LTRIM(p.api_path, '/') LIKE 'warehouse/%'
     OR LTRIM(p.api_path, '/') = 'warehouse'
     OR ((LTRIM(p.api_path, '/') LIKE 'product/%' OR LTRIM(p.api_path, '/') = 'product') AND p.action = 'VIEW')
     OR p.api_path IN ('/approvals')
     OR LTRIM(p.api_path, '/') LIKE 'approvals/%'
     OR p.code LIKE 'APPROVALS_%'
     OR (
           (LTRIM(p.api_path, '/') = 'qtht/notification' OR LTRIM(p.api_path, '/') LIKE 'qtht/notification/%')
       AND p.action IN ('VIEW','UPDATE')
     )
  )
  AND NOT (p.code LIKE 'ACCOUNTING_%')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'WAREHOUSE' AND rp.permission_id = p.id
  );

-- DELIVERY: GIN + customer VIEW + batch VIEW
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM roles WHERE code = 'DELIVERY' AND app_code = 'QTHT' LIMIT 1),
       p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM permission p
WHERE p.is_deleted = false AND p.app_code = 'QTHT'
  AND (
        (LTRIM(p.api_path, '/') LIKE 'warehouse/gin%' AND p.action IN ('VIEW','CREATE','UPDATE','APPROVE'))
     OR (LTRIM(p.api_path, '/') LIKE 'warehouse/batches%' AND p.action IN ('VIEW','CREATE','UPDATE','APPROVE'))
     OR (LTRIM(p.api_path, '/') LIKE 'warehouse/stock-balance%' AND p.action IN ('VIEW','CREATE','UPDATE','APPROVE'))
     OR (LTRIM(p.api_path, '/') LIKE 'warehouse/%' AND p.action = 'VIEW')
     OR ((LTRIM(p.api_path, '/') = 'customer' OR LTRIM(p.api_path, '/') LIKE 'customer/%') AND p.action = 'VIEW')
     OR ((LTRIM(p.api_path, '/') = 'product' OR LTRIM(p.api_path, '/') LIKE 'product/%') AND p.action = 'VIEW')
     OR (
           (LTRIM(p.api_path, '/') = 'qtht/notification' OR LTRIM(p.api_path, '/') LIKE 'qtht/notification/%')
       AND p.action IN ('VIEW','UPDATE')
     )
  )
  AND NOT (p.code LIKE 'ACCOUNTING_%')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'DELIVERY' AND rp.permission_id = p.id
  );

-- CSKH: customer + ticket + CRM-ish VIEW
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM roles WHERE code = 'CSKH' AND app_code = 'QTHT' LIMIT 1),
       p.id, 'QTHT', false, NOW(), 'system', NOW(), 'system'
FROM permission p
WHERE p.is_deleted = false AND p.app_code = 'QTHT'
  AND (
        ((LTRIM(p.api_path, '/') = 'customer' OR LTRIM(p.api_path, '/') LIKE 'customer/%') AND p.action IN ('VIEW','CREATE','UPDATE'))
     OR ((LTRIM(p.api_path, '/') = 'task/ticket' OR LTRIM(p.api_path, '/') LIKE 'task/ticket/%'
       OR LTRIM(p.api_path, '/') = 'task/task' OR LTRIM(p.api_path, '/') LIKE 'task/task/%'
       OR LTRIM(p.api_path, '/') = 'task/ticket-category' OR LTRIM(p.api_path, '/') LIKE 'task/ticket-category/%')
         AND p.action IN ('VIEW','CREATE','UPDATE'))
     OR (LTRIM(p.api_path, '/') LIKE 'crm/%' AND p.action IN ('VIEW','CREATE','UPDATE'))
     OR ((LTRIM(p.api_path, '/') = 'product' OR LTRIM(p.api_path, '/') LIKE 'product/%') AND p.action = 'VIEW')
     OR (
           (LTRIM(p.api_path, '/') = 'qtht/notification' OR LTRIM(p.api_path, '/') LIKE 'qtht/notification/%')
       AND p.action IN ('VIEW','UPDATE')
     )
  )
  AND NOT (p.code LIKE 'ACCOUNTING_%')
  AND NOT (LTRIM(p.api_path, '/') LIKE 'warehouse/%' OR LTRIM(p.api_path, '/') = 'warehouse')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'CSKH' AND rp.permission_id = p.id
  );
