-- ============================================================
-- Menu: Tiện ích Tin tức (/qtht/tien-ich)
-- Banner · Châm ngôn · Tin tức · Ghim tin
-- IDEMPOTENT
-- ============================================================

INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), 'QLHT_TIEN_ICH', 'Tiện ích', 'Utilities', 'QTHT', '/qtht/tien-ich', 'src/modules/news', 'MENU_QTHT', 17, 1, 'Sparkles', false, true, false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = 'QLHT_TIEN_ICH'
);

UPDATE menu
SET parent_code = 'MENU_QTHT',
    order_index = 17,
    fe_url = '/qtht/tien-ich',
    folder_path = 'src/modules/news',
    name = 'Tiện ích',
    name_en = 'Utilities',
    icon = 'Sparkles',
    is_deleted = false,
    status = true,
    updated_date = NOW(),
    updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_TIEN_ICH';

-- Shift siblings after insert
UPDATE menu SET order_index = 18, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_ARTICLE' AND order_index < 18;

UPDATE menu SET order_index = 19, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_GUIDE' AND order_index < 19;

UPDATE menu SET order_index = 20, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_JOBS' AND order_index < 20;
