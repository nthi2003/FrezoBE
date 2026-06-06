-- ============================================================
-- SCRIPT: Thêm Dữ Liệu Tổ Chức (Organization)
-- Description: Tạo cây tổ chức đầy đủ các trường
-- ============================================================

-- 1. ROOT: FTECH Corp (Headquarters)
INSERT INTO organization (
    id, code, name, name_en, short_name, description, 
    tax_code, logo_url, website, email, phone, fax, address, province_code, ward_code,
    organization_type, scale, business_sector, established_date, status, 
    legal_representative_id, contact_person, contact_position, contact_email, contact_phone,
    level, path, order_index, is_delete, created_date, created_by, updated_date, updated_by
)
SELECT 
    gen_random_uuid(), 
    'FTECH_HO', 
    'Tổng Công ty Công nghệ FTECH', 
    'FTECH Technology Corporation', 
    'FTECH CORP', 
    'Trụ sở chính tập đoàn FTECH, chuyên cung cấp giải pháp phần mềm toàn diện.',
    '0101234567', 
    '/assets/images/logo-ftech.png', 
    'https://ftech.com.vn', 
    'info@ftech.com.vn', 
    '024.3768.9999', 
    '024.3768.8888', 
    'Tầng 10, Tòa nhà FTECH Tower, Số 1 Phạm Văn Bạch, Cầu Giấy, Hà Nội', 
    '01', -- Mã Hà Nội 
    '001', -- Mã phường
    'COMPANY', 
    'ENTERPRISE', 
    'Information Technology', 
    '2010-01-01 00:00:00', 
    'ACTIVE', 
    (SELECT id FROM person WHERE code = 'ADMIN' LIMIT 1), -- Link to Admin
    'Nguyễn Văn A', 
    'Giám đốc điều hành', 
    'ceo@ftech.com.vn', 
    '0901234567',
    0, 
    '/FTECH_HO/', 
    1,
    false, 
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE code = 'FTECH_HO');

-- 2. Chi nhánh Hà Nội
INSERT INTO organization (
    id, code, name, name_en, short_name, description, 
    tax_code, logo_url, website, email, phone, fax, address, province_code, ward_code,
    organization_type, scale, business_sector, established_date, status, 
    legal_representative_id, contact_person, contact_position, contact_email, contact_phone,
    level, parent_id, path, order_index, is_delete, created_date, created_by, updated_date, updated_by
)
SELECT 
    gen_random_uuid(), 
    'FTECH_HN', 
    'Chi nhánh Hà Nội', 
    'Hanoi Branch', 
    'FTECH HN', 
    'Chi nhánh phụ trách kinh doanh và kỹ thuật khu vực miền Bắc.',
    '0101234567-001', 
    '/assets/images/logo-ftech-hn.png', 
    'https://hn.ftech.com.vn', 
    'hanoi@ftech.com.vn', 
    '024.3888.6666', 
    '024.3888.5555', 
    'Tầng 5, Tòa nhà Technosoft, Cầu Giấy, Hà Nội', 
    '01', 
    '002',
    'BRANCH', 
    'LARGE', 
    'Software Development & Sales', 
    '2012-06-15 00:00:00', 
    'ACTIVE', 
    (SELECT id FROM person WHERE code = 'ADMIN' LIMIT 1),
    'Trần Thị B', 
    'Giám đốc chi nhánh', 
    'director.hn@ftech.com.vn', 
    '0912345678',
    1, 
    (SELECT id FROM organization WHERE code = 'FTECH_HO'), 
    '/FTECH_HO/FTECH_HN/', 
    2,
    false, 
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE code = 'FTECH_HN');

-- 3. Chi nhánh TP.HCM
INSERT INTO organization (
    id, code, name, name_en, short_name, description, 
    tax_code, logo_url, website, email, phone, fax, address, province_code, ward_code,
    organization_type, scale, business_sector, established_date, status, 
    legal_representative_id, contact_person, contact_position, contact_email, contact_phone,
    level, parent_id, path, order_index, is_delete, created_date, created_by, updated_date, updated_by
)
SELECT 
    gen_random_uuid(), 
    'FTECH_HCM', 
    'Chi nhánh TP.HCM', 
    'HCM Branch', 
    'FTECH HCM', 
    'Chi nhánh phụ trách khu vực miền Nam.',
    '0101234567-002', 
    '/assets/images/logo-ftech-hcm.png', 
    'https://hcm.ftech.com.vn', 
    'hcm@ftech.com.vn', 
    '028.3999.7777', 
    '028.3999.6666', 
    'Tòa nhà Ree Tower, Quận 4, TP.HCM', 
    '79', -- Mã TP.HCM
    '003',
    'BRANCH', 
    'MEDIUM', 
    'Sales & Support', 
    '2015-09-01 00:00:00', 
    'ACTIVE', 
    (SELECT id FROM person WHERE code = 'ADMIN' LIMIT 1),
    'Lê Văn C', 
    'Giám đốc chi nhánh', 
    'director.hcm@ftech.com.vn', 
    '0987654321',
    1, 
    (SELECT id FROM organization WHERE code = 'FTECH_HO'), 
    '/FTECH_HO/FTECH_HCM/', 
    3,
    false, 
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE code = 'FTECH_HCM');

-- 4. Phòng Ban HN - Kỹ Thuật
INSERT INTO organization (
    id, code, name, name_en, short_name, description, 
    tax_code, logo_url, website, email, phone, fax, address, province_code, ward_code,
    organization_type, scale, business_sector, established_date, status, 
    legal_representative_id, contact_person, contact_position, contact_email, contact_phone,
    level, parent_id, path, order_index, is_delete, created_date, created_by, updated_date, updated_by
)
SELECT 
    gen_random_uuid(), 
    'DEPT_TECH_HN', 
    'Phòng Kỹ Thuật', 
    'Technology Department', 
    'TECH HN', 
    'Chịu trách nhiệm nghiên cứu và phát triển sản phẩm.',
    NULL, NULL, NULL, 
    'tech.hn@ftech.com.vn', 
    '024.3888.1111', 
    NULL, 
    'Tầng 5, Tòa nhà Technosoft', 
    '01', '002',
    'DEPARTMENT', 
    'MEDIUM', 
    'R&D', 
    '2012-07-01 00:00:00', 
    'ACTIVE', 
    (SELECT id FROM person WHERE code = 'ADMIN' LIMIT 1),
    'Phạm Kỹ T', 
    'Trưởng phòng', 
    'tech.lead@ftech.com.vn', 
    '0999888777',
    2, 
    (SELECT id FROM organization WHERE code = 'FTECH_HN'), 
    '/FTECH_HO/FTECH_HN/DEPT_TECH_HN/', 
    1,
    false, 
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DEPT_TECH_HN');

-- 5. Phòng Ban HN - Nhân sự
INSERT INTO organization (
    id, code, name, name_en, short_name, description, 
    tax_code, logo_url, website, email, phone, fax, address, province_code, ward_code,
    organization_type, scale, business_sector, established_date, status, 
    legal_representative_id, contact_person, contact_position, contact_email, contact_phone,
    level, parent_id, path, order_index, is_delete, created_date, created_by, updated_date, updated_by
)
SELECT 
    gen_random_uuid(), 
    'DEPT_HR_HN', 
    'Phòng Hành Chính Nhân Sự', 
    'Human Resources Department', 
    'HR HN', 
    'Quản lý nhân sự, tuyển dụng và hành chính.',
    NULL, NULL, NULL, 
    'hr.hn@ftech.com.vn', 
    '024.3888.2222', 
    NULL, 
    'Tầng 5, Tòa nhà Technosoft', 
    '01', '002',
    'DEPARTMENT', 
    'SMALL', 
    'Human Resources', 
    '2012-07-01 00:00:00', 
    'ACTIVE', 
    (SELECT id FROM person WHERE code = 'ADMIN' LIMIT 1),
    'Đỗ Thu H', 
    'Trưởng phòng', 
    'hr.lead@ftech.com.vn', 
    '0966555444',
    2, 
    (SELECT id FROM organization WHERE code = 'FTECH_HN'), 
    '/FTECH_HO/FTECH_HN/DEPT_HR_HN/', 
    2,
    false, 
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DEPT_HR_HN');

-- 6. Phòng Ban HN - Kinh Doanh
INSERT INTO organization (
    id, code, name, name_en, short_name, description, 
    tax_code, logo_url, website, email, phone, fax, address, province_code, ward_code,
    organization_type, scale, business_sector, established_date, status, 
    legal_representative_id, contact_person, contact_position, contact_email, contact_phone,
    level, parent_id, path, order_index, is_delete, created_date, created_by, updated_date, updated_by
)
SELECT 
    gen_random_uuid(), 
    'DEPT_SALE_HN', 
    'Phòng Kinh Doanh', 
    'Sales Department', 
    'SALE HN', 
    'Tìm kiếm khách hàng và phát triển thị trường.',
    NULL, NULL, NULL, 
    'sales.hn@ftech.com.vn', 
    '024.3888.3333', 
    NULL, 
    'Tầng 5, Tòa nhà Technosoft', 
    '01', '002',
    'DEPARTMENT', 
    'MEDIUM', 
    'Sales', 
    '2012-07-01 00:00:00', 
    'ACTIVE', 
    (SELECT id FROM person WHERE code = 'ADMIN' LIMIT 1),
    'Ngô Mạnh S', 
    'Trưởng phòng', 
    'sales.lead@ftech.com.vn', 
    '0933222111',
    2, 
    (SELECT id FROM organization WHERE code = 'FTECH_HN'), 
    '/FTECH_HO/FTECH_HN/DEPT_SALE_HN/', 
    3,
    false, 
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DEPT_SALE_HN');

-- 7. Phòng Ban HCM - Kỹ Thuật
INSERT INTO organization (
    id, code, name, name_en, short_name, description, 
    tax_code, logo_url, website, email, phone, fax, address, province_code, ward_code,
    organization_type, scale, business_sector, established_date, status, 
    legal_representative_id, contact_person, contact_position, contact_email, contact_phone,
    level, parent_id, path, order_index, is_delete, created_date, created_by, updated_date, updated_by
)
SELECT 
    gen_random_uuid(), 
    'DEPT_TECH_HCM', 
    'Phòng Kỹ Thuật HCM', 
    'Technology Department HCM', 
    'TECH HCM', 
    'Hỗ trợ kỹ thuật và triển khai dự án phía Nam.',
    NULL, NULL, NULL, 
    'tech.hcm@ftech.com.vn', 
    '028.3999.1111', 
    NULL, 
    'Tòa nhà Ree Tower', 
    '79', '003',
    'DEPARTMENT', 
    'SMALL', 
    'Technical Support', 
    '2015-10-01 00:00:00', 
    'ACTIVE', 
    (SELECT id FROM person WHERE code = 'ADMIN' LIMIT 1),
    'Huỳnh Tấn K', 
    'Trưởng nhóm', 
    'tech.hcm@ftech.com.vn', 
    '0944333222',
    2, 
    (SELECT id FROM organization WHERE code = 'FTECH_HCM'), 
    '/FTECH_HO/FTECH_HCM/DEPT_TECH_HCM/', 
    1,
    false, 
    NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DEPT_TECH_HCM');
