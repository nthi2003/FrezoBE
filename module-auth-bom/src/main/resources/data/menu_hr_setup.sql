-- HR Setup hub menu — Module Hồ sơ nhân sự (2026-08-06)
-- Idempotent seed for /qlns/settings

INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), 'QLNS_HR_SETUP', 'Thiết lập HR', 'HR Setup', 'QTHT', '/qlns/settings', 'src/modules/qlns', 'MENU_HRM', 13, 1, 'fa-solid fa-sliders', true, true, false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE code = 'QLNS_HR_SETUP' AND app_code = 'QTHT');

UPDATE menu
SET fe_url = '/qlns/settings',
    name = 'Thiết lập HR',
    parent_code = 'MENU_HRM',
    order_index = 13,
    is_deleted = false,
    status = true,
    updated_date = NOW(),
    updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLNS_HR_SETUP';
