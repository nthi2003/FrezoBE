-- ============================================================
-- SCRIPT: Phân quyền Full Menu cho ADMIN
-- Description: Lấy tất cả menu trong hệ thống và gán cho role ADMIN
-- ============================================================

INSERT INTO role_menu (id, role_id, menu_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT 
    gen_random_uuid(), 
    (SELECT id FROM roles WHERE code = 'ADMIN' AND app_code = 'QTHT' LIMIT 1), 
    m.id, 
    'QTHT', 
    false,
    NOW(), 
    'system', 
    NOW(), 
    'system'
FROM menu m
WHERE m.app_code = 'QTHT'
  AND NOT EXISTS (
      SELECT 1 
      FROM role_menu rm 
      JOIN roles r ON rm.role_id = r.id
      WHERE r.code = 'ADMIN' 
        AND rm.menu_id = m.id
        AND rm.app_code = 'QTHT'
  );

-- ============================================================
-- SCRIPT: Phân quyền cho MANAGER (Giống Admin nhưng trừ Cấu hình/Phân quyền)
-- ============================================================
INSERT INTO role_menu (id, role_id, menu_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT 
    gen_random_uuid(), 
    (SELECT id FROM roles WHERE code = 'MANAGER' AND app_code = 'QTHT' LIMIT 1), 
    m.id, 
    'QTHT', 
    false,
    NOW(), 
    'system', 
    NOW(), 
    'system'
FROM menu m
WHERE m.app_code = 'QTHT'
  AND m.code NOT IN ('QLHT_ROLE', 'QLHT_SETTING', 'QLHT_FEATURE') -- Trừ các menu admin
  AND NOT EXISTS (
      SELECT 1 
      FROM role_menu rm 
      JOIN roles r ON rm.role_id = r.id
      WHERE r.code = 'MANAGER' 
        AND rm.menu_id = m.id
        AND rm.app_code = 'QTHT'
  );

-- ============================================================
-- SCRIPT: Phân quyền cho STAFF (Chỉ dashboard và chấm công, công việc, tin tức)
-- ============================================================
INSERT INTO role_menu (id, role_id, menu_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT 
    gen_random_uuid(), 
    (SELECT id FROM roles WHERE code = 'STAFF' AND app_code = 'QTHT' LIMIT 1), 
    m.id, 
    'QTHT', 
    false,
    NOW(), 
    'system', 
    NOW(), 
    'system'
FROM menu m
WHERE m.app_code = 'QTHT'
  -- Leaves + domain parents so FE tree can nest (orphan children → root otherwise)
  -- HOME = portal mọi staff; DASHBOARD KPI chỉ Admin/Manager (không gán STAFF)
  AND m.code IN (
      'HOME',
      'MENU_HRM', 'QLHT_ATTENDANCE',
      'MENU_TASK', 'QLHT_CV', 'QLHT_TICKET',
      'MENU_GROWTH', 'QLHT_EVENT',
      'MENU_QTHT', 'QLHT_ARTICLE'
  )
  AND NOT EXISTS (
      SELECT 1 
      FROM role_menu rm 
      JOIN roles r ON rm.role_id = r.id
      WHERE r.code = 'STAFF' 
        AND rm.menu_id = m.id
        AND rm.app_code = 'QTHT'
  );

-- Revoke DASHBOARD KPI menu from STAFF (nếu đã seed trước khi tách HOME)
UPDATE role_menu rm
SET is_deleted = true,
    updated_date = NOW(),
    updated_by = 'system'
FROM roles r, menu m
WHERE rm.role_id = r.id
  AND rm.menu_id = m.id
  AND r.code = 'STAFF'
  AND r.app_code = 'QTHT'
  AND m.code = 'DASHBOARD'
  AND m.app_code = 'QTHT'
  AND (rm.is_deleted IS DISTINCT FROM true);

-- Ensure HOME portal on every active role (idempotent)
INSERT INTO role_menu (id, role_id, menu_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    r.id,
    m.id,
    'QTHT',
    false,
    NOW(),
    'system',
    NOW(),
    'system'
FROM roles r
CROSS JOIN menu m
WHERE r.app_code = 'QTHT'
  AND r.is_deleted = false
  AND (r.status = 'A' OR r.status IS NULL)
  AND m.app_code = 'QTHT'
  AND m.code = 'HOME'
  AND m.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.app_code = 'QTHT'
  );
