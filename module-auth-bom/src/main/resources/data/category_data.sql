-- ============================================================
-- SCRIPT: Category & CategoryGroup seed
-- Description: Danh mục dùng chung (Issuer, Signer, Chức danh, Location,
--              Industry, Đơn vị tính) — được menu QLHT_CATEGORY (sub QLDM_*)
--              và combobox ĐVT (sản phẩm / HĐ) tham chiếu.
-- Created: 2026-07-16 (Batch I5)
-- Updated: 2026-07-21 — QA-QLNS-001: canonical groupCode Chức danh = ChucDanh
--                       (legacy TITLE → migrate idempotent bên dưới)
-- Updated: 2026-07-30 — Seed groupCode DonVi (Đơn vị tính) + UOM_* cho SME
--                       rau củ / thương mại. Align FE category.schema.ts.
-- IDEMPOTENT: NOT EXISTS trên (group_code, code)
-- ============================================================

-- ============================================================
-- 0) MIGRATE legacy TITLE → ChucDanh (idempotent)
--    SA-QLNS-CD-001 / BE-QLNS-CD-001: FE + USER dùng groupCode=ChucDanh.
--    Giữ mã item TTL_* không đổi.
-- ============================================================
INSERT INTO category_group (code, name, cat_group, is_deleted)
SELECT 'ChucDanh', 'Chức danh', 3, false
WHERE NOT EXISTS (
    SELECT 1 FROM category_group cg WHERE cg.code = 'ChucDanh'
);

UPDATE categories c
SET group_code = 'ChucDanh',
    updated_date = NOW(),
    updated_by = 'system'
WHERE c.group_code = 'TITLE'
  AND NOT EXISTS (
      SELECT 1 FROM categories x
      WHERE x.group_code = 'ChucDanh' AND x.code = c.code
  );

-- Xóa bản TITLE trùng code đã có ở ChucDanh (tránh orphan duplicate)
DELETE FROM categories c
WHERE c.group_code = 'TITLE'
  AND EXISTS (
      SELECT 1 FROM categories x
      WHERE x.group_code = 'ChucDanh' AND x.code = c.code
  );

-- Xóa group TITLE mồ côi nếu không còn category nào trỏ tới
DELETE FROM category_group
WHERE code = 'TITLE'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE group_code = 'TITLE');

-- ============================================================
-- 0b) MIGRATE legacy UNIT / ĐonVi → DonVi (idempotent)
--     FE canonical: groupCode = 'DonVi' (category.schema.ts GROUP_CODE_OPTIONS).
-- ============================================================
INSERT INTO category_group (code, name, cat_group, is_deleted)
SELECT 'DonVi', 'Đơn vị tính', 6, false
WHERE NOT EXISTS (
    SELECT 1 FROM category_group cg WHERE cg.code = 'DonVi'
);

UPDATE categories c
SET group_code = 'DonVi',
    updated_date = NOW(),
    updated_by = 'system'
WHERE c.group_code IN ('UNIT', 'ĐonVi', 'DON_VI')
  AND NOT EXISTS (
      SELECT 1 FROM categories x
      WHERE x.group_code = 'DonVi' AND x.code = c.code
  );

DELETE FROM categories c
WHERE c.group_code IN ('UNIT', 'ĐonVi', 'DON_VI')
  AND EXISTS (
      SELECT 1 FROM categories x
      WHERE x.group_code = 'DonVi' AND x.code = c.code
  );

DELETE FROM category_group
WHERE code IN ('UNIT', 'ĐonVi', 'DON_VI')
  AND NOT EXISTS (
      SELECT 1 FROM categories WHERE group_code = category_group.code
  );

-- ============================================================
-- 1) CATEGORY GROUP (nhóm danh mục dùng chung)
--    catGroup: 1=Issuer, 2=Signer, 3=ChucDanh, 4=Location, 5=Industry, 6=DonVi
--    (LoaiTaiSan / DanhMucSP seed riêng bởi module initializer nếu có)
-- ============================================================
INSERT INTO category_group (code, name, cat_group, is_deleted)
SELECT v.code, v.name, v.cat_group, false
FROM (VALUES
    ('ISSUER',   'Cơ quan phát hành',    1),
    ('SIGNER',   'Người ký',              2),
    ('ChucDanh', 'Chức danh',             3),
    ('LOCATION', 'Địa bàn',               4),
    ('INDUSTRY', 'Ngành nghề',            5),
    ('DonVi',    'Đơn vị tính',           6),
    ('UX_POPUP', 'Popup UX thành công',   7)
) AS v(code, name, cat_group)
WHERE NOT EXISTS (
    SELECT 1 FROM category_group cg WHERE cg.code = v.code
);

-- ============================================================
-- 2) CATEGORY items (sample data cho từng group)
-- ============================================================
INSERT INTO categories (id, code, name, name_en, short_name, group_code, order_index, description, active, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, v.short_name, v.group_code, v.order_index, v.description, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    -- ---------- ISSUER (Cơ quan phát hành) ----------
    ('ISS_MOJ',   'Bộ Tư pháp',                        'Ministry of Justice',    'MOJ',   'ISSUER',   1, 'Cơ quan quản lý pháp luật'),
    ('ISS_MOH',   'Bộ Y tế',                           'Ministry of Health',     'MOH',   'ISSUER',   2, 'Cơ quan quản lý y tế'),
    ('ISS_MOF',   'Bộ Tài chính',                      'Ministry of Finance',    'MOF',   'ISSUER',   3, 'Cơ quan quản lý tài chính'),
    ('ISS_MOET',  'Bộ Giáo dục và Đào tạo',            'Ministry of Education',  'MOET',  'ISSUER',   4, 'Cơ quan quản lý giáo dục'),
    ('ISS_MOLISA','Bộ Lao động - Thương binh & XH',    'MOLISA',                 'MOLISA','ISSUER',   5, 'Cơ quan quản lý lao động'),
    ('ISS_UBND',  'Uỷ ban Nhân dân',                   'People''s Committee',    'UBND',  'ISSUER',   6, 'Cấp tỉnh/huyện/xã'),
    -- ---------- SIGNER (Người ký mẫu) ----------
    ('SGN_CEO',       'Tổng Giám đốc',           'CEO',                 'CEO',  'SIGNER', 1, 'Người ký cấp cao nhất'),
    ('SGN_COO',       'Giám đốc Điều hành',      'COO',                 'COO',  'SIGNER', 2, NULL),
    ('SGN_CFO',       'Giám đốc Tài chính',      'CFO',                 'CFO',  'SIGNER', 3, NULL),
    ('SGN_HR_DIR',    'Trưởng phòng Nhân sự',    'HR Director',         'HRD',  'SIGNER', 4, NULL),
    ('SGN_LEGAL',     'Trưởng phòng Pháp chế',   'Legal Director',      'LEG',  'SIGNER', 5, NULL),
    -- ---------- ChucDanh (Chức danh) — codes TTL_* giữ nguyên ----------
    ('TTL_INTERN',   'Thực tập sinh',            'Intern',              'INT',  'ChucDanh',  1, NULL),
    ('TTL_JUNIOR',   'Nhân viên (Junior)',       'Junior',              'JUN',  'ChucDanh',  2, NULL),
    ('TTL_STAFF',    'Nhân viên chính thức',     'Staff',               'STF',  'ChucDanh',  3, NULL),
    ('TTL_SENIOR',   'Nhân viên cấp cao',        'Senior',              'SEN',  'ChucDanh',  4, NULL),
    ('TTL_LEAD',     'Trưởng nhóm',              'Team Lead',           'LED',  'ChucDanh',  5, NULL),
    ('TTL_MANAGER',  'Quản lý',                  'Manager',             'MGR',  'ChucDanh',  6, NULL),
    ('TTL_DIRECTOR', 'Giám đốc',                 'Director',            'DIR',  'ChucDanh',  7, NULL),
    -- ---------- LOCATION (Địa bàn) ----------
    ('LOC_HN',   'Hà Nội',       'Hanoi',            'HN',  'LOCATION', 1, 'Thủ đô'),
    ('LOC_HCM',  'TP. Hồ Chí Minh','Ho Chi Minh City','HCM', 'LOCATION', 2, NULL),
    ('LOC_DN',   'Đà Nẵng',      'Da Nang',          'DN',  'LOCATION', 3, NULL),
    ('LOC_HP',   'Hải Phòng',    'Hai Phong',        'HP',  'LOCATION', 4, NULL),
    ('LOC_CT',   'Cần Thơ',      'Can Tho',          'CT',  'LOCATION', 5, NULL),
    ('LOC_BD',   'Bình Dương',   'Binh Duong',       'BD',  'LOCATION', 6, NULL),
    ('LOC_DNAI', 'Đồng Nai',     'Dong Nai',         'DNAI','LOCATION', 7, NULL),
    -- ---------- INDUSTRY (Ngành nghề) ----------
    ('IND_IT',       'Công nghệ thông tin',        'Information Technology', 'IT',    'INDUSTRY', 1, NULL),
    ('IND_FINANCE',  'Tài chính - Ngân hàng',      'Finance & Banking',      'FIN',   'INDUSTRY', 2, NULL),
    ('IND_EDU',      'Giáo dục - Đào tạo',         'Education',              'EDU',   'INDUSTRY', 3, NULL),
    ('IND_HEALTH',   'Y tế - Chăm sóc sức khoẻ',   'Healthcare',             'HLTH',  'INDUSTRY', 4, NULL),
    ('IND_RETAIL',   'Bán lẻ - Thương mại',        'Retail',                 'RTL',   'INDUSTRY', 5, NULL),
    ('IND_MFG',      'Sản xuất - Chế tạo',         'Manufacturing',          'MFG',   'INDUSTRY', 6, NULL),
    ('IND_LOGISTICS','Logistics - Kho vận',        'Logistics',              'LOG',   'INDUSTRY', 7, NULL),
    ('IND_REALESTATE','Bất động sản',              'Real Estate',            'RE',    'INDUSTRY', 8, NULL),
    -- ---------- DonVi (Đơn vị tính) — SME rau củ / thương mại ----------
    -- name = nhãn hiển thị VN (khớp unitName/product_units & default HĐ "cái")
    -- code = UOM_* (unique toàn bảng categories)
    ('UOM_CAI',   'cái',    'piece',      'cái',   'DonVi',  1,  'Đơn vị đếm'),
    ('UOM_KG',    'kg',     'kilogram',   'kg',    'DonVi',  2,  'Kilogram'),
    ('UOM_G',     'g',      'gram',       'g',     'DonVi',  3,  'Gram'),
    ('UOM_THUNG', 'thùng',  'carton',     'thùng', 'DonVi',  4,  'Thùng carton'),
    ('UOM_HOP',   'hộp',    'box',        'hộp',   'DonVi',  5,  'Hộp'),
    ('UOM_CHAI',  'chai',   'bottle',     'chai',  'DonVi',  6,  'Chai'),
    ('UOM_BO',    'bó',     'bunch',      'bó',    'DonVi',  7,  'Bó rau/lá'),
    ('UOM_BAO',   'bao',    'sack',       'bao',   'DonVi',  8,  'Bao/bao tải'),
    ('UOM_LIT',   'lít',    'liter',      'lít',   'DonVi',  9,  'Lít'),
    ('UOM_ML',    'ml',     'milliliter', 'ml',    'DonVi', 10,  'Mililít'),
    ('UOM_MET',   'mét',    'meter',      'mét',   'DonVi', 11,  'Mét'),
    ('UOM_GOI',   'gói',    'pack',       'gói',   'DonVi', 12,  'Gói'),
    ('UOM_RO',    'rổ',     'basket',     'rổ',    'DonVi', 13,  'Rổ (chợ/kho)'),
    ('UOM_QUA',   'quả',    'fruit',      'quả',   'DonVi', 14,  'Quả'),
    ('UOM_CAY',   'cây',    'stalk',      'cây',   'DonVi', 15,  'Cây'),
    ('UOM_CHUC',  'chục',   'dozen10',    'chục',  'DonVi', 16,  'Chục (10 cái)'),
    ('UOM_LON',   'lon',    'can',        'lon',   'DonVi', 17,  'Lon'),
    ('UOM_TAN',   'tấn',    'ton',        'tấn',   'DonVi', 18,  'Tấn')
) AS v(code, name, name_en, short_name, group_code, order_index, description)
WHERE NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE c.group_code = v.group_code AND c.code = v.code
);

-- ============================================================
-- 3) UX_POPUP templates — name=title, description=body (hoặc JSON
--    {"body":"...","imageUrl":"..."}). active=false để tắt popup.
--    Admin sửa tại /admin/category-management (group UX_POPUP).
-- ============================================================
INSERT INTO category_group (code, name, cat_group, is_deleted)
SELECT 'UX_POPUP', 'Popup UX thành công', 7, false
WHERE NOT EXISTS (
    SELECT 1 FROM category_group cg WHERE cg.code = 'UX_POPUP'
);

INSERT INTO categories (id, code, name, name_en, short_name, group_code, order_index, description, active, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, v.short_name, v.group_code, v.order_index, v.description, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    (
        'ATTENDANCE_FIRST_CHECKIN',
        'Chúc ngày làm việc hiệu quả!',
        'Have a productive day!',
        'CHECKIN',
        'UX_POPUP',
        1,
        'Bạn vừa check-in thành công. Chúc một ngày làm việc tràn đầy năng lượng và đạt nhiều kết quả tốt!'
    ),
    (
        'LOGIN_FIRST_OF_DAY',
        'Chào buổi sáng!',
        'Good morning!',
        'LOGIN',
        'UX_POPUP',
        2,
        'Chào mừng bạn trở lại Frezo. Chúc một ngày làm việc vui vẻ!'
    ),
    (
        'TASK_COMPLETED',
        'Hoàn thành công việc!',
        'Task completed!',
        'TASK',
        'UX_POPUP',
        3,
        'Tuyệt vời — bạn vừa hoàn thành một công việc. Tiếp tục phát huy nhé!'
    )
) AS v(code, name, name_en, short_name, group_code, order_index, description)
WHERE NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE c.group_code = v.group_code AND c.code = v.code
);
