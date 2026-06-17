-- ============================================================
-- SCRIPT: Thêm Menu Từ Frontend Routes
-- Description: Insert menu items dựa trên router.tsx
-- Created: 2026-01-09
-- Updated: 2026-06-17 (fix folder_path -> src/modules)
-- ============================================================

-- Xóa dữ liệu cũ (Cẩn thận khi chạy production)
-- DELETE FROM role_menu WHERE app_code = 'QTHT';
-- DELETE FROM menu WHERE app_code = 'QTHT';

-- ============================================================
-- 1. ROOT MENUS
-- ============================================================

INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
VALUES 
  (gen_random_uuid(), 'DASHBOARD', 'Dashboard', 'Dashboard', 'QTHT', '/', 'src/modules/dashboard', NULL, 1, 1, 'fa-solid fa-chart-line', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_ORG', 'Quản Lý Tổ Chức', 'Organization Management', 'QTHT', '/qtht/organizations', 'src/modules/qtht', NULL, 2, 1, 'fa-solid fa-sitemap', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_STAFF', 'Quản Lý Nhân Viên', 'Employee Management', 'QTHT', '/qlns/persons', 'src/modules/qlns', NULL, 3, 1, 'fa-solid fa-users', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_ARTICLE', 'Quản Lý Bài Viết', 'Article Management', 'QTHT', '/admin/article-management', 'src/modules/cms', NULL, 4, 1, 'fa-solid fa-newspaper', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_EVENT', 'Quản Lý Sự Kiện', 'Event Management', 'QTHT', '/admin/events', 'src/modules/events', NULL, 5, 1, 'fa-solid fa-calendar', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_ATTENDANCE', 'Quản Lý Chấm Công', 'Attendance Management', 'QTHT', '/admin/attendance', 'src/modules/qlns', NULL, 6, 1, 'fa-solid fa-clock', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_ACCOUNT', 'Quản Lý Tài Khoản', 'Account Management', 'QTHT', '/qtht/users', 'src/modules/users', NULL, 7, 1, 'fa-solid fa-user-lock', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_ROLE', 'Quản Lý Phân Quyền', 'Role Management', 'QTHT', '/qtht/roles', 'src/modules/roles', NULL, 8, 1, 'fa-solid fa-shield', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_FEATURE', 'Quản Lý Chức Năng', 'Feature Management', 'QTHT', '/qtht/menus', 'src/modules/menus', NULL, 9, 1, 'fa-solid fa-cogs', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_CATEGORY', 'Quản Lý Danh Mục', 'Category Management', 'QTHT', '/admin/category-management', 'src/modules/qtht', NULL, 10, 1, 'fa-solid fa-list', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_DEPARTMENT', 'Quản Lý Phòng Ban', 'Department Management', 'QTHT', '/qtht/departments', 'src/modules/qtht', NULL, 11, 1, 'fa-solid fa-building', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_ASSET', 'Quản Lý Tài Sản', 'Asset Management', 'QTHT', '/admin/qlts', 'src/modules/assets', NULL, 12, 1, 'fa-solid fa-box', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_TICKET', 'Hỗ Trợ Khách Hàng', 'Customer Support', 'QTHT', '/task/tickets', 'src/modules/tasks', NULL, 13, 1, 'fa-solid fa-headset', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_BGHD', 'Quản Lý BGHD', 'Price list Management', 'QTHT', '/admin/qlbghd', 'src/modules/contracts', NULL, 14, 1, 'fa-solid fa-file-invoice', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_CV', 'Quản Lý Công Việc', 'Job Management', 'QTHT', '/task', 'src/modules/tasks', NULL, 15, 1, 'fa-solid fa-tasks', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'PROFILE', 'Thông tin cá nhân', 'Profile', 'QTHT', '/profile', 'src/modules/profile', NULL, 97, 1, 'fa-solid fa-user', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLHT_SETTING', 'Cài Đặt', 'Setting', 'QTHT', '/qtht/settings', 'src/modules/qtht', NULL, 99, 1, 'fa-solid fa-gear', true, true, false, NOW(), 'system', NOW(), 'system');

-- ============================================================
-- 2. SUB MENUS (DANH MỤC)
-- ============================================================

INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
VALUES 
  (gen_random_uuid(), 'QLDM_ISSUER', 'Cơ Quan Phát Hành', 'Issuer', 'QTHT', '/admin/category-management', 'src/modules/qtht', 'QLHT_CATEGORY', 1, 1, 'fa-solid fa-bank', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLDM_SIGNER', 'Người Ký', 'Signer', 'QTHT', '/admin/category-management/signer', 'src/modules/qtht', 'QLHT_CATEGORY', 2, 1, 'fa-solid fa-signature', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLDM_TITLE', 'Chức Danh', 'Title', 'QTHT', '/admin/category-management/title', 'src/modules/qtht', 'QLHT_CATEGORY', 3, 1, 'fa-solid fa-id-card', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLDM_LOCATION', 'Địa Bàn', 'Location', 'QTHT', '/admin/category-management/location', 'src/modules/qtht', 'QLHT_CATEGORY', 4, 1, 'fa-solid fa-map-location-dot', true, true, false, NOW(), 'system', NOW(), 'system'),
  (gen_random_uuid(), 'QLDM_INDUSTRY', 'Ngành', 'Industry', 'QTHT', '/admin/category-management/industry', 'src/modules/qtht', 'QLHT_CATEGORY', 5, 1, 'fa-solid fa-industry', true, true, false, NOW(), 'system', NOW(), 'system');
