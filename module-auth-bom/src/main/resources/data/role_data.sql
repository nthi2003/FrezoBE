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

-- 4. HR (Leave step 2 / ApproverResolver)
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    'HR',
    'Nhân sự',
    'Duyệt nghỉ phép / hồ sơ nhân sự (Approval LEAVE_STANDARD step 2).',
    'QTHT',
    'A',
    false,
    NOW(),
    'system',
    NOW(),
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'HR' AND app_code = 'QTHT'
);

-- 5. CHIEF_ACC (Payroll period lock step 1)
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    'CHIEF_ACC',
    'Kế toán trưởng',
    'Duyệt khoá kỳ lương (Approval PAYROLL_PERIOD step 1).',
    'QTHT',
    'A',
    false,
    NOW(),
    'system',
    NOW(),
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'CHIEF_ACC' AND app_code = 'QTHT'
);

-- ============================================================
-- SME rau củ ops roles (vision checklist) — menu scoped in role_menu_data
-- ============================================================

-- 6. PURCHASING — Thu mua
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), 'PURCHASING', 'Thu mua',
       'Quản lý NCC, cảnh báo tồn → PR/PO; không vào Kế toán / QTHT.',
       'QTHT', 'A', false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PURCHASING' AND app_code = 'QTHT');

-- 7. WAREHOUSE — Thủ kho
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), 'WAREHOUSE', 'Thủ kho',
       'GRN/GIN, lô/FEFO, kiểm kê, hao hụt, cảnh báo tồn/cận hạn.',
       'QTHT', 'A', false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'WAREHOUSE' AND app_code = 'QTHT');

-- 8. DELIVERY — Giao hàng (proxied by GIN xuất bán; chưa có module logistics)
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), 'DELIVERY', 'Giao hàng',
       'Xuất kho bán (GIN) + xem KH; chưa có tuyến/tài xế/tracking.',
       'QTHT', 'A', false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'DELIVERY' AND app_code = 'QTHT');

-- 9. CSKH — Chăm sóc khách hàng
INSERT INTO roles (id, code, name, description, app_code, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), 'CSKH', 'CSKH',
       'Khách hàng + ticket khiếu nại chất lượng; không vào Kế toán / Thu mua.',
       'QTHT', 'A', false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'CSKH' AND app_code = 'QTHT');
