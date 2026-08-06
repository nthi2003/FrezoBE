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
-- Updated: 2026-08-06 — Seed item cho 7 group HR (CapBac, TrinhDo, ChiNhanh,
--                       LoaiHopDong, GiaiDoan, KetQuaDanhGia, LyDoNghiViec).
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

-- HR Hạng mục — group codes for Module Hồ sơ nhân sự (2026-08-06)
INSERT INTO category_group (code, name, cat_group, is_deleted)
SELECT v.code, v.name, 3, false
FROM (VALUES
    ('TrinhDo', 'Trình độ'),
    ('CapBac', 'Cấp bậc'),
    ('ChiNhanh', 'Chi nhánh'),
    ('LoaiHopDong', 'Loại hợp đồng'),
    ('GiaiDoan', 'Giai đoạn'),
    ('KetQuaDanhGia', 'Kết quả đánh giá'),
    ('LyDoNghiViec', 'Lý do nghỉ việc')
) AS v(code, name)
WHERE NOT EXISTS (SELECT 1 FROM category_group cg WHERE cg.code = v.code);

-- HR Hạng mục items — không có item thì select Cấp bậc / Chức danh ở
-- /qlns/settings?tab=positions rỗng (ChucDanh dùng TTL_* seed ở phần 2).
-- name / name_en unique toàn bảng categories (CategoryServiceImpl.validateRequest)
-- → tên không được trùng ChucDanh / SalaryBand; name_en để NULL cho chắc.
INSERT INTO categories (id, code, name, name_en, short_name, group_code, order_index, description, active, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, NULL, v.short_name, v.group_code, v.order_index, NULL, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    -- ---------- CapBac (Cấp bậc) — dùng cho hr_job_position.rank_code ----------
    ('CB_INTERN',        'Thực tập',                        'TT',     'CapBac', 1),
    ('CB_STAFF',         'Nhân viên',                       'NV',     'CapBac', 2),
    ('CB_SPECIALIST',    'Chuyên viên',                     'CV',     'CapBac', 3),
    ('CB_SR_SPECIALIST', 'Chuyên viên chính',               'CVC',    'CapBac', 4),
    ('CB_LEAD',          'Tổ trưởng',                       'TOT',    'CapBac', 5),
    ('CB_MIDDLE_MGR',    'Quản lý cấp trung',               'QLCT',   'CapBac', 6),
    ('CB_SENIOR_MGR',    'Quản lý cấp cao',                 'QLCC',   'CapBac', 7),
    ('CB_EXECUTIVE',     'Ban giám đốc',                    'BGD',    'CapBac', 8),
    -- ---------- TrinhDo (Trình độ) ----------
    ('TD_THPT',      'Trung học phổ thông',                 'THPT',   'TrinhDo', 1),
    ('TD_TRUNGCAP',  'Trung cấp',                           'TC',     'TrinhDo', 2),
    ('TD_CAODANG',   'Cao đẳng',                            'CD',     'TrinhDo', 3),
    ('TD_DAIHOC',    'Đại học',                             'DH',     'TrinhDo', 4),
    ('TD_THACSI',    'Thạc sĩ',                             'ThS',    'TrinhDo', 5),
    ('TD_TIENSI',    'Tiến sĩ',                             'TS',     'TrinhDo', 6),
    ('TD_KHAC',      'Trình độ khác',                       'TDK',    'TrinhDo', 7),
    -- ---------- ChiNhanh (Chi nhánh) ----------
    ('CN_HQ',   'Trụ sở chính',                             'HQ',     'ChiNhanh', 1),
    ('CN_HN',   'Chi nhánh Hà Nội',                         'CN-HN',  'ChiNhanh', 2),
    ('CN_HCM',  'Chi nhánh TP. Hồ Chí Minh',                'CN-HCM', 'ChiNhanh', 3),
    ('CN_DN',   'Chi nhánh Đà Nẵng',                        'CN-DN',  'ChiNhanh', 4),
    -- ---------- LoaiHopDong (Loại hợp đồng) ----------
    ('LHD_THUVIEC',      'Hợp đồng thử việc',               'HDTV',   'LoaiHopDong', 1),
    ('LHD_XACDINH',      'Hợp đồng xác định thời hạn',      'HDXD',   'LoaiHopDong', 2),
    ('LHD_KHONGXACDINH', 'Hợp đồng không xác định thời hạn','HDKXD',  'LoaiHopDong', 3),
    ('LHD_THOIVU',       'Hợp đồng thời vụ',                'HDTVU',  'LoaiHopDong', 4),
    ('LHD_KHOANVIEC',    'Hợp đồng khoán việc',             'HDKV',   'LoaiHopDong', 5),
    ('LHD_THUCTAP',      'Hợp đồng thực tập',               'HDTT',   'LoaiHopDong', 6),
    -- ---------- GiaiDoan (Giai đoạn làm việc) ----------
    ('GD_NHANVIEC',   'Nhận việc',                          'ONB',    'GiaiDoan', 1),
    ('GD_THUVIEC',    'Thử việc',                           'TVIEC',  'GiaiDoan', 2),
    ('GD_CHINHTHUC',  'Chính thức',                         'CTHUC',  'GiaiDoan', 3),
    ('GD_TAMHOAN',    'Tạm hoãn hợp đồng',                  'THOAN',  'GiaiDoan', 4),
    ('GD_NGHIVIEC',   'Đã nghỉ việc',                       'NGHI',   'GiaiDoan', 5),
    -- ---------- KetQuaDanhGia (Kết quả đánh giá) ----------
    ('KQ_XUATSAC',      'Xuất sắc',                         'A+',     'KetQuaDanhGia', 1),
    ('KQ_TOT',          'Tốt',                              'A',      'KetQuaDanhGia', 2),
    ('KQ_DAT',          'Đạt',                              'B',      'KetQuaDanhGia', 3),
    ('KQ_CANCAITHIEN',  'Cần cải thiện',                    'C',      'KetQuaDanhGia', 4),
    ('KQ_KHONGDAT',     'Không đạt',                        'D',      'KetQuaDanhGia', 5),
    -- ---------- LyDoNghiViec (Lý do nghỉ việc) ----------
    ('LDN_CANHAN',     'Nghỉ theo nguyện vọng cá nhân',     'CANHAN', 'LyDoNghiViec', 1),
    ('LDN_HETHAN',     'Hết hạn hợp đồng',                  'HETHAN', 'LyDoNghiViec', 2),
    ('LDN_CHUYENCT',   'Chuyển công tác',                   'CHUYEN', 'LyDoNghiViec', 3),
    ('LDN_KHONGDATYC', 'Không đạt yêu cầu công việc',       'KDATYC', 'LyDoNghiViec', 4),
    ('LDN_TINHGIAN',   'Tinh giản nhân sự',                 'TGIAN',  'LyDoNghiViec', 5),
    ('LDN_KHAC',       'Lý do khác',                        'LDKHAC', 'LyDoNghiViec', 6)
) AS v(code, name, short_name, group_code, order_index)
WHERE NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE c.group_code = v.group_code AND c.code = v.code
);
