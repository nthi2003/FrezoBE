-- ============================================================
-- SCRIPT: Thêm Roles Mặc Định (ADMIN, MANAGER, STAFF)
-- ============================================================

-- 1. ADMIN
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT 
    gen_random_uuid(), 
    'ADMIN', 
    'Quản trị viên', 
    'Quản trị viên hệ thống, có toàn quyền truy cập.',
    'QTHT', 
    'A', 
    false,
    NOW(), 
    'system', 
    NOW(), 
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'ADMIN' AND app_code = 'QTHT'
);

-- 2. MANAGER
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT 
    gen_random_uuid(), 
    'MANAGER', 
    'Quản lý', 
    'Quản lý bộ phận hoặc nhóm, quyền hạn giới hạn.',
    'QTHT', 
    'A', 
    false,
    NOW(), 
    'system', 
    NOW(), 
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'MANAGER' AND app_code = 'QTHT'
);

-- 3. STAFF
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT 
    gen_random_uuid(), 
    'STAFF', 
    'Nhân viên', 
    'Nhân viên bình thường, chỉ truy cập các chức năng cơ bản.',
    'QTHT', 
    'A', 
    false,
    NOW(), 
    'system', 
    NOW(), 
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'STAFF' AND app_code = 'QTHT'
);
