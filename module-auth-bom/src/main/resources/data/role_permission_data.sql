-- ============================================================
-- SCRIPT: Role ↔ Permission mapping
--   - ADMIN   : full toàn bộ permission (~160)
--   - MANAGER : tất cả trừ các permission "hệ thống nhạy cảm"
--               (qtht/role, qtht/permission, qtht/menu, qtht/user CREATE/UPDATE/DELETE,
--                qtht/setting UPDATE, qtht/ip-* CREATE/DELETE, dmdc/* DELETE)
--   - STAFF   : chỉ VIEW ở tất cả module business + task/CRUD trên task cá nhân
-- Created: 2026-07-16 (Batch I3)
-- IDEMPOTENT: NOT EXISTS trên (role_id, permission_id)
-- ============================================================

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
  -- Loại các permission siêu nhạy cảm
  AND NOT (p.api_path = 'qtht/role'       AND p.action IN ('CREATE','UPDATE','DELETE'))
  AND NOT (p.api_path = 'qtht/permission' AND p.action IN ('CREATE','UPDATE','DELETE'))
  AND NOT (p.api_path = 'qtht/menu'       AND p.action IN ('CREATE','UPDATE','DELETE'))
  AND NOT (p.api_path = 'qtht/user'       AND p.action IN ('CREATE','DELETE'))
  AND NOT (p.api_path = 'qtht/setting'    AND p.action = 'UPDATE')
  AND NOT (p.api_path LIKE 'qtht/ip-%'    AND p.action IN ('CREATE','DELETE'))
  AND NOT (p.api_path LIKE 'dmdc/%'       AND p.action = 'DELETE')
  AND NOT (p.api_path = 'qtht/audit-log') -- Không xem audit log
  AND NOT (p.api_path = 'qtht/api-log'    AND p.action = 'DELETE')
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
        -- 1) VIEW ở phần lớn module
        (p.action = 'VIEW' AND p.api_path NOT IN (
             'qtht/role','qtht/permission','qtht/menu','qtht/user',
             'qtht/setting','qtht/audit-log','qtht/api-log',
             'qtht/ip-blacklist','qtht/ip-whitelist','qtht/ip-trust',
             'cms/customer','cms/order','cms/voucher',
             'fb/account','fb/group','fb/lead','fb/automation',
             -- CYCLE-DEP: Staff không xem/ghi sổ khấu hao (QA G2)
             '/asset/depreciation'
        ))
        -- 2) Task cá nhân: được CRUD task/ticket
        OR (p.api_path IN ('task/task','task/ticket') AND p.action IN ('CREATE','UPDATE'))
        -- 3) Attendance cá nhân: CREATE/UPDATE
        OR (p.api_path = 'qlns/attendance' AND p.action IN ('CREATE','UPDATE'))
        -- 4) Leave request cá nhân: CREATE
        OR (p.api_path IN ('qlns/leave','qlns/leave-request') AND p.action = 'CREATE')
        -- 5) Notification cá nhân
        OR (p.api_path = 'qtht/notification' AND p.action IN ('VIEW','UPDATE'))
        -- 6) Cart cá nhân
        OR (p.api_path = 'product/cart' AND p.action IN ('CREATE','DELETE'))
        -- 7) QTBV Articles (CYCLE-QTLV-ART / BE-ART-003): Content Writer tạo nháp + gửi duyệt
        OR (p.api_path = '/qtbv/articles' AND p.action IN ('CREATE','UPDATE'))
        OR (p.api_path = '/qtbv/articles/submit' AND p.action = 'UPDATE')
  )
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission rp
      JOIN roles r ON rp.role_id = r.id
      WHERE r.code = 'STAFF'
        AND rp.permission_id = p.id
  );
