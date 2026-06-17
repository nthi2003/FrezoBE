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
  AND m.code IN ('DASHBOARD', 'QLHT_ATTENDANCE', 'QLHT_CV', 'QLHT_ARTICLE', 'QLHT_EVENT', 'QLHT_TICKET')
  AND NOT EXISTS (
      SELECT 1 
      FROM role_menu rm 
      JOIN roles r ON rm.role_id = r.id
      WHERE r.code = 'STAFF' 
        AND rm.menu_id = m.id
        AND rm.app_code = 'QTHT'
  );
