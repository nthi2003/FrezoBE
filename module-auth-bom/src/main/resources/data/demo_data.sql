-- ============================================================
-- SCRIPT: DEMO / SAMPLE DATA cho toàn hệ thống
-- Description: Seed data mẫu để mỗi trang FE khi mở đều có data hiển thị,
--              bao gồm cả các luồng nghiệp vụ (duyệt bài, duyệt hợp đồng,
--              duyệt đơn nghỉ, chấm công, tính lương, ...).
--              Chạy sau organization_data.sql / category_data.sql / RBAC seed.
-- IDEMPOTENT: mọi INSERT đều dùng WHERE NOT EXISTS trên business key (code).
--
-- Tables covered:
--   [Hệ thống]   Persons(10), Departments(6), Setting(1)
--   [Danh mục]   CategoryGroup 'LoaiSanPham' + 8 loại sản phẩm
--   [Nội dung]   Articles(8) — có author + manager (người duyệt)
--   [Nhân sự]    Contracts(10), LeaveRequests(6), Attendances(≈70), Payrolls(20)
--   [Kinh doanh] Customers(10), NCCs(5), Products(12)
--   [Task]       Tags(6), Tasks(10), Tickets(8)
--   [Email]      EmailTemplates(4)
--
-- Roles/luồng workflow (users tạo qua DataInitializer.seedDemoLoginUsers):
--   admin / superadmin  → SUPER ADMIN — Person.is_admin=true, full access
--   hungnv (EMP001)     → IT Manager    — MANAGER role, dept IT
--   maitt  (EMP002)     → HR Manager    — MANAGER role, dept HR    → duyệt LeaveRequest / Contract
--   tuanle (EMP003)     → Dev Senior    — STAFF role,   dept IT
--   hapt   (EMP004)     → HR Staff      — STAFF role,   dept HR
--   anhhd  (EMP005)     → Sales Manager — MANAGER role, dept SALES → duyệt Article PUBLIC
--   bichvn (EMP006)     → Content Writer— STAFF role,   dept MKT   → tạo Article DRAFT/WAITING
--   baodq  (EMP007)     → Backend Dev   — STAFF role,   dept IT
--   loanbt (EMP008)     → Finance Mgr   — MANAGER role, dept FIN   → duyệt Payroll
--   khangnx(EMP009)     → QA Engineer   — STAFF role,   dept IT
--   trangdt(EMP010)     → Admin Office  — STAFF role,   dept OPS
--   Password mặc định:   123456 (BCrypt encode qua PasswordEncoder Bean, không hardcode SQL).
-- ============================================================

-- ============================================================
-- 1) PERSONS — 10 nhân viên mẫu
--    Convention: code = 'EMP{001..010}', email = '<code>@frezo.com'
-- ============================================================
INSERT INTO person (id, code, name, short_name, activated, is_admin, is_deleted, gender, dob,
                    email, phone, job_title, address, description, avatar_url,
                    created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.short_name, true, false, false, v.gender,
       v.dob::date, v.email, v.phone, v.job_title, v.address, v.description, v.avatar,
       NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('EMP001', 'Nguyễn Văn Hùng',    'Hùng',   'MALE',   '1988-03-12', 'emp001@frezo.com', '0901000001', 'Trưởng phòng IT',        'Số 1 Cầu Giấy, Hà Nội',    'Trưởng phòng công nghệ, 10 năm kinh nghiệm.',       'https://i.pravatar.cc/150?img=11'),
    ('EMP002', 'Trần Thị Mai',       'Mai',    'FEMALE', '1992-07-08', 'emp002@frezo.com', '0901000002', 'Trưởng phòng Nhân sự',   'Số 2 Cầu Giấy, Hà Nội',    'Phụ trách tuyển dụng và đào tạo nhân sự.',          'https://i.pravatar.cc/150?img=32'),
    ('EMP003', 'Lê Minh Tuấn',       'Tuấn',   'MALE',   '1990-11-25', 'emp003@frezo.com', '0901000003', 'Kỹ sư phần mềm Senior',  'Số 3 Cầu Giấy, Hà Nội',    'Full-stack developer, chuyên React & Spring Boot.', 'https://i.pravatar.cc/150?img=13'),
    ('EMP004', 'Phạm Thu Hà',        'Hà',     'FEMALE', '1995-05-14', 'emp004@frezo.com', '0901000004', 'Chuyên viên tuyển dụng', 'Số 4 Cầu Giấy, Hà Nội',    'HR Recruiter, kinh nghiệm 5 năm ngành IT.',         'https://i.pravatar.cc/150?img=34'),
    ('EMP005', 'Hoàng Đức Anh',      'Anh',    'MALE',   '1993-09-30', 'emp005@frezo.com', '0901000005', 'Trưởng phòng Kinh doanh','Số 5 Cầu Giấy, Hà Nội',    'Sales manager phụ trách khu vực miền Bắc.',         'https://i.pravatar.cc/150?img=15'),
    ('EMP006', 'Vũ Ngọc Bích',       'Bích',   'FEMALE', '1994-02-18', 'emp006@frezo.com', '0901000006', 'Chuyên viên Marketing',  'Số 6 Cầu Giấy, Hà Nội',    'Digital marketing & content specialist.',           'https://i.pravatar.cc/150?img=36'),
    ('EMP007', 'Đặng Quốc Bảo',      'Bảo',    'MALE',   '1991-06-05', 'emp007@frezo.com', '0901000007', 'Kỹ sư phần mềm',         'Số 7 Cầu Giấy, Hà Nội',    'Backend engineer, hệ thống thanh toán.',            'https://i.pravatar.cc/150?img=17'),
    ('EMP008', 'Bùi Thanh Loan',     'Loan',   'FEMALE', '1996-12-01', 'emp008@frezo.com', '0901000008', 'Kế toán trưởng',         'Số 8 Cầu Giấy, Hà Nội',    'Chief accountant, tài chính doanh nghiệp.',         'https://i.pravatar.cc/150?img=38'),
    ('EMP009', 'Ngô Xuân Khang',     'Khang',  'MALE',   '1989-08-22', 'emp009@frezo.com', '0901000009', 'Kỹ sư QA',               'Số 9 Cầu Giấy, Hà Nội',    'Quality assurance engineer, automation testing.',   'https://i.pravatar.cc/150?img=19'),
    ('EMP010', 'Đỗ Thu Trang',       'Trang',  'FEMALE', '1997-04-10', 'emp010@frezo.com', '0901000010', 'Chuyên viên hành chính', 'Số 10 Cầu Giấy, Hà Nội',   'Administrative officer, phụ trách văn phòng.',      'https://i.pravatar.cc/150?img=40')
) AS v(code, name, short_name, gender, dob, email, phone, job_title, address, description, avatar)
WHERE NOT EXISTS (SELECT 1 FROM person p WHERE p.code = v.code);

-- ============================================================
-- 2) DEPARTMENTS — 6 phòng ban, link tới org FTECH_HN + manager là Person đã seed
-- ============================================================
INSERT INTO department (id, code, name, name_en, short_name, description, email, phone,
                        organization_id, parent_id, manager_id, level, order_index, path, status,
                        is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, v.short_name, v.description,
       v.email, v.phone,
       (SELECT id FROM organization WHERE code = 'FTECH_HN' LIMIT 1),
       NULL,
       (SELECT id FROM person WHERE code = v.manager_code LIMIT 1),
       1, v.order_index, '/' || v.code || '/', 'ACTIVE',
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('IT',    'Phòng Công Nghệ Thông Tin', 'IT Department',        'IT',    'Phát triển và vận hành hệ thống phần mềm.', 'it@frezo.com',        '024.3888.0001', 'EMP001', 1),
    ('HR',    'Phòng Nhân Sự',             'HR Department',        'HR',    'Tuyển dụng, đào tạo, chế độ nhân sự.',      'hr@frezo.com',        '024.3888.0002', 'EMP002', 2),
    ('SALES', 'Phòng Kinh Doanh',          'Sales Department',     'SALES', 'Tìm kiếm khách hàng, phát triển thị trường.', 'sales@frezo.com',    '024.3888.0003', 'EMP005', 3),
    ('MKT',   'Phòng Marketing',           'Marketing Department', 'MKT',   'Quảng bá thương hiệu, digital marketing.',  'marketing@frezo.com', '024.3888.0004', 'EMP006', 4),
    ('FIN',   'Phòng Tài Chính - Kế Toán', 'Finance Department',   'FIN',   'Quản lý tài chính, kế toán doanh nghiệp.',  'finance@frezo.com',   '024.3888.0005', 'EMP008', 5),
    ('OPS',   'Phòng Hành Chính',          'Operations Department','OPS',   'Vận hành văn phòng, hành chính tổng hợp.',  'ops@frezo.com',       '024.3888.0006', 'EMP010', 6)
) AS v(code, name, name_en, short_name, description, email, phone, manager_code, order_index)
WHERE NOT EXISTS (SELECT 1 FROM department d WHERE d.code = v.code);

-- ============================================================
-- 3) Assign persons → departments (idempotent update)
-- ============================================================
UPDATE person SET department_id = (SELECT id FROM department WHERE code = 'IT'    LIMIT 1),
                  org_id = (SELECT id FROM organization WHERE code = 'FTECH_HN' LIMIT 1)
WHERE code IN ('EMP001','EMP003','EMP007','EMP009') AND (department_id IS NULL OR org_id IS NULL);

UPDATE person SET department_id = (SELECT id FROM department WHERE code = 'HR'    LIMIT 1),
                  org_id = (SELECT id FROM organization WHERE code = 'FTECH_HN' LIMIT 1)
WHERE code IN ('EMP002','EMP004') AND (department_id IS NULL OR org_id IS NULL);

UPDATE person SET department_id = (SELECT id FROM department WHERE code = 'SALES' LIMIT 1),
                  org_id = (SELECT id FROM organization WHERE code = 'FTECH_HN' LIMIT 1)
WHERE code = 'EMP005' AND (department_id IS NULL OR org_id IS NULL);

UPDATE person SET department_id = (SELECT id FROM department WHERE code = 'MKT'   LIMIT 1),
                  org_id = (SELECT id FROM organization WHERE code = 'FTECH_HN' LIMIT 1)
WHERE code = 'EMP006' AND (department_id IS NULL OR org_id IS NULL);

UPDATE person SET department_id = (SELECT id FROM department WHERE code = 'FIN'   LIMIT 1),
                  org_id = (SELECT id FROM organization WHERE code = 'FTECH_HN' LIMIT 1)
WHERE code = 'EMP008' AND (department_id IS NULL OR org_id IS NULL);

UPDATE person SET department_id = (SELECT id FROM department WHERE code = 'OPS'   LIMIT 1),
                  org_id = (SELECT id FROM organization WHERE code = 'FTECH_HN' LIMIT 1)
WHERE code = 'EMP010' AND (department_id IS NULL OR org_id IS NULL);

-- ============================================================
-- 4) DEMO USERS — xem DataInitializer.seedDemoLoginUsers()
--    (Phải encode BCrypt qua PasswordEncoder Bean, KHÔNG hardcode hash trong SQL.)
-- ============================================================

-- ============================================================
-- 5) SETTING — 1 record cho org root (bật chấm công + email)
-- ============================================================
INSERT INTO setting (id, org_id, is_email, is_swap, is_color, is_attendance, details,
                     morning_start, morning_end, afternoon_start, afternoon_end,
                     max_members, max_posts, require_avatar, require_cv, require_health_cert,
                     auto_approve_article, article_approver, require_manager, allow_late,
                     is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM organization WHERE code = 'FTECH_HO' LIMIT 1),
       true, true, true, true, '{}',
       '08:00', '12:00', '13:30', '17:30',
       100, 50, true, false, false,
       false, 'admin', true, true,
       false, NOW(), 'system', NOW(), 'system'
WHERE EXISTS (SELECT 1 FROM organization WHERE code = 'FTECH_HO')
  AND NOT EXISTS (
      SELECT 1 FROM setting s WHERE s.org_id = (SELECT id FROM organization WHERE code = 'FTECH_HO' LIMIT 1)
  );

-- ============================================================
-- 6) TAGS — nhãn công việc VN (slug không dấu, category khớp FE TagsPage)
--    Migration: cập nhật seed EN cũ → VI nếu DB đã chạy demo trước đó.
-- ============================================================
UPDATE tags SET code = 'bug', name = 'Bug', category = 'other', color = '#dc2626',
           updated_date = NOW(), updated_by = 'system'
WHERE code = 'BUG' AND COALESCE(is_deleted, false) = false
  AND NOT EXISTS (SELECT 1 FROM tags t2 WHERE t2.code = 'bug' AND t2.id <> tags.id);

UPDATE tags SET code = 'tinh-nang', name = 'Tính năng', category = 'other', color = '#3b82f6',
           updated_date = NOW(), updated_by = 'system'
WHERE code = 'FEATURE' AND COALESCE(is_deleted, false) = false
  AND NOT EXISTS (SELECT 1 FROM tags t2 WHERE t2.code = 'tinh-nang' AND t2.id <> tags.id);

UPDATE tags SET code = 'cai-tien', name = 'Cải tiến', category = 'other', color = '#22c55e',
           updated_date = NOW(), updated_by = 'system'
WHERE code = 'IMPROVE' AND COALESCE(is_deleted, false) = false
  AND NOT EXISTS (SELECT 1 FROM tags t2 WHERE t2.code = 'cai-tien' AND t2.id <> tags.id);

UPDATE tags SET code = 'gap', name = 'Gấp', category = 'priority', color = '#f97316',
           updated_date = NOW(), updated_by = 'system'
WHERE code = 'URGENT' AND COALESCE(is_deleted, false) = false
  AND NOT EXISTS (SELECT 1 FROM tags t2 WHERE t2.code = 'gap' AND t2.id <> tags.id);

UPDATE tags SET code = 'tai-lieu', name = 'Tài liệu', category = 'other', color = '#a855f7',
           updated_date = NOW(), updated_by = 'system'
WHERE code = 'DOC' AND COALESCE(is_deleted, false) = false
  AND NOT EXISTS (SELECT 1 FROM tags t2 WHERE t2.code = 'tai-lieu' AND t2.id <> tags.id);

UPDATE tags SET code = 'theo-doi', name = 'Theo dõi', category = 'status', color = '#eab308',
           updated_date = NOW(), updated_by = 'system'
WHERE code IN ('REVIEW', 'FOLLOW_UP', 'FOLLOW-UP') AND COALESCE(is_deleted, false) = false
  AND NOT EXISTS (SELECT 1 FROM tags t2 WHERE t2.code = 'theo-doi' AND t2.id <> tags.id);

-- Soft-delete orphan EN seed nếu đã có bản VI song song (tránh trùng hiển thị)
UPDATE tags SET is_deleted = true, updated_date = NOW(), updated_by = 'system'
WHERE code IN ('BUG', 'FEATURE', 'IMPROVE', 'URGENT', 'DOC', 'REVIEW', 'FOLLOW_UP', 'FOLLOW-UP')
  AND COALESCE(is_deleted, false) = false
  AND EXISTS (
      SELECT 1 FROM tags t2
      WHERE t2.code IN ('bug', 'tinh-nang', 'cai-tien', 'gap', 'tai-lieu', 'theo-doi')
        AND t2.is_deleted = false
  );

INSERT INTO tags (id, code, name, category, color, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.category, v.color, false,
       NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('gap',        'Gấp',         'priority', '#f97316'),
    ('quan-trong', 'Quan trọng',  'priority', '#ef4444'),
    ('bug',        'Bug',         'other',    '#dc2626'),
    ('tinh-nang',  'Tính năng',   'other',    '#3b82f6'),
    ('hop',        'Họp',         'other',    '#06b6d4'),
    ('theo-doi',   'Theo dõi',    'status',   '#eab308'),
    ('cai-tien',   'Cải tiến',    'other',    '#22c55e'),
    ('tai-lieu',   'Tài liệu',    'other',    '#a855f7')
) AS v(code, name, category, color)
WHERE NOT EXISTS (SELECT 1 FROM tags t WHERE t.code = v.code AND COALESCE(t.is_deleted, false) = false);

-- ============================================================
-- 7) TASKS — 10 công việc mẫu (mix status/priority, có assignee, deadline)
-- ============================================================
INSERT INTO tasks (id, title, description, priority, status, assignee_id, deadline,
                   is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.title, v.description, v.priority, v.status,
       (SELECT id FROM person WHERE code = v.assignee_code LIMIT 1),
       (CURRENT_DATE + (v.deadline_offset_days || ' days')::interval)::timestamp,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('Migrate JWT token TTL sang refresh flow',        'Nghiên cứu refresh token rotation + revoke list.',                    'HIGH',   'IN_PROGRESS', 'EMP001', 3),
    ('Viết docs onboarding cho intern Q3',              'Tổng hợp checklist onboarding + tài liệu training.',                  'MEDIUM', 'OPEN',        'EMP002', 7),
    ('Fix bug 404 ở trang /admin/events',               'Route missing, cần thêm placeholder trong router.',                   'HIGH',   'DONE',        'EMP003', -2),
    ('Redesign trang Articles theo enterprise pattern', 'Header + PageGuide + full-page editor.',                              'HIGH',   'DONE',        'EMP003', -5),
    ('Chuẩn bị deck sales Q4',                          'Số liệu doanh thu 9 tháng đầu năm + roadmap.',                        'MEDIUM', 'IN_PROGRESS', 'EMP005', 5),
    ('Setup Google Ads campaign',                       'Chạy campaign quảng bá sản phẩm mới, budget 20M/tháng.',              'MEDIUM', 'OPEN',        'EMP006', 10),
    ('Đóng sổ kế toán tháng',                           'Đối chiếu công nợ, xuất báo cáo BCTC tháng.',                         'HIGH',   'OPEN',        'EMP008', 2),
    ('Test regression module Payroll',                  'Chạy full suite, đảm bảo bảng lương tính đúng.',                      'MEDIUM', 'IN_PROGRESS', 'EMP009', 4),
    ('Kiểm kê VPP + văn phòng',                         'Đếm số lượng, đối chiếu với danh sách tài sản.',                      'LOW',    'OPEN',        'EMP010', 14),
    ('Backup DB production',                            'Định kỳ hàng tuần, upload S3 + verify checksum.',                     'HIGH',   'DONE',        'EMP007', -1)
) AS v(title, description, priority, status, assignee_code, deadline_offset_days)
WHERE NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = v.title);

-- ============================================================
-- 8) TAGS trên task — gán nhãn VI cho vài task demo
-- ============================================================
INSERT INTO task_tags (task_id, tag_id)
SELECT
    (SELECT id FROM tasks WHERE title = v.task_title LIMIT 1),
    (SELECT id FROM tags  WHERE code  = v.tag_code AND COALESCE(is_deleted, false) = false LIMIT 1)
FROM (VALUES
    ('Fix bug 404 ở trang /admin/events',               'bug'),
    ('Fix bug 404 ở trang /admin/events',               'gap'),
    ('Redesign trang Articles theo enterprise pattern', 'tinh-nang'),
    ('Redesign trang Articles theo enterprise pattern', 'theo-doi'),
    ('Backup DB production',                            'gap'),
    ('Backup DB production',                            'quan-trong'),
    ('Viết docs onboarding cho intern Q3',              'tai-lieu'),
    ('Chuẩn bị deck sales Q4',                          'hop'),
    ('Chuẩn bị deck sales Q4',                          'theo-doi'),
    ('Test regression module Payroll',                  'cai-tien')
) AS v(task_title, tag_code)
WHERE EXISTS (SELECT 1 FROM tasks WHERE title = v.task_title)
  AND EXISTS (SELECT 1 FROM tags WHERE code = v.tag_code AND COALESCE(is_deleted, false) = false)
  AND NOT EXISTS (
      SELECT 1 FROM task_tags tt
      WHERE tt.task_id = (SELECT id FROM tasks WHERE title = v.task_title LIMIT 1)
        AND tt.tag_id  = (SELECT id FROM tags  WHERE code  = v.tag_code AND COALESCE(is_deleted, false) = false LIMIT 1)
  );

-- ============================================================
-- 9) CUSTOMERS — 10 khách hàng (mix COMPANY / INDIVIDUAL)
-- ============================================================
INSERT INTO customers (id, code, name, phone, phone_last4, email, address, tax_code, type, status,
                       note, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.phone, RIGHT(v.phone, 4), v.email, v.address,
       v.tax_code, v.type, 'ACTIVE', v.note, false,
       NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('KH001', 'Công ty TNHH ABC Việt Nam',       '0912345001', 'contact@abc.vn',         'Tầng 5, Toà Keangnam, Hà Nội',        '0100223344', 'COMPANY',    'Khách hàng lâu năm, hợp đồng năm.'),
    ('KH002', 'Công ty CP Xây Dựng Sao Vàng',    '0912345002', 'info@saovang.com.vn',    '123 Nguyễn Trãi, Thanh Xuân, HN',      '0100223345', 'COMPANY',    'Đối tác chiến lược từ 2020.'),
    ('KH003', 'Nhà hàng Phố Cổ',                 '0912345003', 'phoco@gmail.com',        '15 Hàng Bạc, Hoàn Kiếm, HN',           NULL,          'COMPANY',    'Cung cấp thực phẩm hàng tuần.'),
    ('KH004', 'Cửa hàng Tạp Hoá Minh Anh',       '0912345004', NULL,                     '45 Lê Duẩn, Đống Đa, HN',              NULL,          'INDIVIDUAL', 'Chị Anh - chủ shop.'),
    ('KH005', 'Công ty TNHH FPT Software',       '0912345005', 'contact@fsoft.com.vn',   'Toà FPT, Duy Tân, Cầu Giấy, HN',       '0101248109',  'COMPANY',    'Đối tác công nghệ.'),
    ('KH006', 'Anh Trần Văn Hải',                '0912345006', 'hai.tran@yahoo.com',     '78 Xuân Thủy, Cầu Giấy, HN',           NULL,          'INDIVIDUAL', 'Khách lẻ, mua định kỳ.'),
    ('KH007', 'Chuỗi Cafe Highland',              '0912345007', 'partner@highland.vn',   '234 Nguyễn Thị Minh Khai, HCM',         '0300223346',  'COMPANY',    'Cung cấp cafe hạt.'),
    ('KH008', 'Siêu Thị BigC Thăng Long',        '0912345008', 'purchase@bigc.vn',      '222 Trần Duy Hưng, Cầu Giấy, HN',      '0200223347',  'COMPANY',    'Đơn hàng lớn hàng tháng.'),
    ('KH009', 'Chị Nguyễn Thị Lan',              '0912345009', NULL,                     '99 Kim Mã, Ba Đình, HN',               NULL,          'INDIVIDUAL', 'Khách quen 3 năm.'),
    ('KH010', 'Công ty CP May Việt Tiến',         '0912345010', 'sales@viettien.com.vn', 'Số 7 Lê Minh Xuân, Bình Tân, HCM',     '0300223348',  'COMPANY',    'Đồng phục cho nhân viên.')
) AS v(code, name, phone, email, address, tax_code, type, note)
WHERE NOT EXISTS (SELECT 1 FROM customers c WHERE c.code = v.code);

-- ============================================================
-- 10) NCC (Nhà cung cấp) — 5
-- ============================================================
INSERT INTO nccs (id, code, name, representative, phone, phone_last4, address,
                  classification_code, growing_area, max_capacity, strengths,
                  is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.rep, v.phone, RIGHT(v.phone, 4),
       v.address, v.classification, v.area, v.capacity, v.strengths,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('NCC001', 'HTX Rau Sạch Đà Lạt',      'Nguyễn Văn Xanh', '0987000001', 'Phường 8, Đà Lạt, Lâm Đồng',        'HANG_A', 15.5, 800.0,  'Rau xanh, dâu tây, atisô hữu cơ.'),
    ('NCC002', 'Trang Trại Ba Vì',          'Trần Thị Nga',    '0987000002', 'Xã Vân Hoà, Ba Vì, Hà Nội',         'HANG_A', 8.0,  500.0,  'Bò sữa, gà thả vườn, rau củ sạch.'),
    ('NCC003', 'Công ty Thuỷ Sản Cà Mau',   'Lê Văn Đông',     '0987000003', 'TP Cà Mau, Cà Mau',                 'HANG_B', 25.0, 2000.0, 'Tôm sú, cua biển, cá tra.'),
    ('NCC004', 'HTX Trái Cây Bến Tre',      'Phạm Thị Dừa',    '0987000004', 'Huyện Châu Thành, Bến Tre',         'HANG_A', 12.0, 1000.0, 'Dừa xiêm, chôm chôm, sầu riêng.'),
    ('NCC005', 'Công ty Gạo Long An',       'Hoàng Minh Lúa',  '0987000005', 'Huyện Đức Hòa, Long An',            'HANG_B', 200.0, 5000.0, 'Gạo ST25, gạo tẻ, gạo lứt.')
) AS v(code, name, rep, phone, address, classification, area, capacity, strengths)
WHERE NOT EXISTS (SELECT 1 FROM nccs n WHERE n.code = v.code);

-- ============================================================
-- 11) PRODUCTS — 12 sản phẩm mẫu
-- ============================================================
INSERT INTO products (id, code, name, origin, season, image_url, description, price, rating,
                      warning_threshold, expiry_alert_days, is_active, is_new, is_deleted,
                      created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.origin, v.season, v.image_url, v.description,
       v.price, v.rating, v.threshold, v.expiry_days, true, v.is_new, false,
       NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('SP001', 'Rau Cải Xanh Đà Lạt',        'Đà Lạt',    'Đông Xuân', 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=400', 'Rau cải xanh hữu cơ, không thuốc trừ sâu.', 25000.0,  4.8, 10.0,  3,  true),
    ('SP002', 'Dâu Tây Đà Lạt Hộp 500g',    'Đà Lạt',    'Đông',      'https://images.unsplash.com/photo-1518635017498-87f514b751ba?w=400', 'Dâu tây tươi thu hoạch trong ngày.',        180000.0, 4.9, 5.0,   2,  true),
    ('SP003', 'Cà Chua Bi Ba Vì',            'Ba Vì',     'Quanh năm', 'https://images.unsplash.com/photo-1592841200221-a6898f307baa?w=400', 'Cà chua bi trồng theo tiêu chuẩn VietGAP.', 35000.0,  4.5, 15.0,  5,  false),
    ('SP004', 'Bơ Sáp Đắk Lắk',              'Đắk Lắk',   'Hè',        'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=400', 'Bơ sáp thơm béo, size lớn.',                75000.0,  4.7, 8.0,   4,  false),
    ('SP005', 'Cam Sành Hà Giang',            'Hà Giang',  'Đông Xuân', 'https://images.unsplash.com/photo-1547514701-42782101795e?w=400',  'Cam ngọt tự nhiên, mọng nước.',             55000.0,  4.6, 20.0,  7,  false),
    ('SP006', 'Xoài Cát Hoà Lộc',             'Tiền Giang','Hè',        'https://images.unsplash.com/photo-1553279768-865429fa0078?w=400',  'Xoài cát chín cây, ngọt đậm đà.',           95000.0,  4.9, 10.0,  4,  true),
    ('SP007', 'Tôm Sú Cà Mau Size Lớn',      'Cà Mau',    'Quanh năm', 'https://images.unsplash.com/photo-1565680018434-b513d5573b03?w=400', 'Tôm sú tươi sống, size 20-25 con/kg.',      450000.0, 4.8, 5.0,   1,  false),
    ('SP008', 'Cá Tra Fillet',                'An Giang',  'Quanh năm', 'https://images.unsplash.com/photo-1535596784-9d2fe1c8be3c?w=400',  'Cá tra fillet cấp đông, đóng gói 1kg.',     120000.0, 4.4, 15.0,  30, false),
    ('SP009', 'Gạo ST25 Sóc Trăng 5kg',      'Sóc Trăng', 'Quanh năm', 'https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400', 'Gạo ST25 chính hãng, thơm dẻo.',            185000.0, 5.0, 20.0,  90, true),
    ('SP010', 'Dừa Xiêm Bến Tre',            'Bến Tre',   'Quanh năm', 'https://images.unsplash.com/photo-1580984969071-a8da5656c2fb?w=400', 'Dừa xiêm ngọt lịm, trái to.',               25000.0,  4.5, 30.0,  7,  false),
    ('SP011', 'Sầu Riêng Ri6 Cái Mơn',        'Bến Tre',   'Hè',        'https://images.unsplash.com/photo-1553279768-865429fa0078?w=400',  'Sầu riêng Ri6 hàng tuyển, thơm béo.',       320000.0, 4.9, 5.0,   3,  true),
    ('SP012', 'Trứng Gà Ta Ba Vì Vỉ 30',     'Ba Vì',     'Quanh năm', 'https://images.unsplash.com/photo-1587486913049-53fc88980cfc?w=400', 'Trứng gà thả vườn, size to đều.',           85000.0,  4.7, 10.0,  14, false)
) AS v(code, name, origin, season, image_url, description, price, rating, threshold, expiry_days, is_new)
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.code = v.code);

-- ============================================================
-- 12) ARTICLES — 8 bài viết mẫu, đầy đủ luồng duyệt (author → manager)
--     WORKFLOW: DRAFT → WAITING_APPROVAL → APPROVED → PUBLISHED
--     - author_id  = người viết  (EMPxxx)
--     - manager_id = người duyệt (EMP002 HR Mgr / EMP005 Sales Mgr / EMP001 CTO)
--     - published_at chỉ set khi status = PUBLISHED
-- ============================================================
INSERT INTO articles (id, code, title, content, author_id, manager_id, organization_id, status, publish_scope,
                      is_active, is_public, published_at,
                      is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.title, v.content,
       (SELECT id FROM person WHERE code = v.author_code  LIMIT 1),
       (SELECT id FROM person WHERE code = v.manager_code LIMIT 1),
       (SELECT id FROM organization WHERE code = 'FTECH_HO' LIMIT 1),
       v.status, v.scope, v.is_active, v.is_public,
       (CASE WHEN v.status = 'PUBLISHED' THEN NOW() - (v.days_ago || ' days')::interval ELSE NULL END),
       false, NOW() - ((v.days_ago + 2) || ' days')::interval, 'system', NOW(), 'system'
FROM (VALUES
    -- code            | title                                    | content ...                                                                                                                                                                                                                                                                                        | author  | manager | status            | scope    | active | public | days_ago
    ('ART_20260701',   'Thông báo lịch nghỉ lễ 2/9 năm 2026',      '<p>Kính gửi toàn thể cán bộ nhân viên FTECH,</p><p>Công ty xin thông báo lịch nghỉ lễ Quốc khánh 2/9 năm 2026 như sau:</p><ul><li>Thời gian: từ 01/09/2026 đến hết 03/09/2026 (3 ngày)</li><li>Ngày làm việc trở lại: Thứ Sáu 04/09/2026</li></ul><p>Trân trọng thông báo.</p>',                             'EMP004', 'EMP002', 'PUBLISHED',        'INTERNAL', true,  false, 15),
    ('ART_20260702',   'Chào mừng nhân viên mới tháng 7',          '<p>Trong tháng 7, chúng ta chào đón <strong>5 nhân viên mới</strong> gia nhập đại gia đình FTECH.</p><p>Chúc mừng và mong các bạn có nhiều đóng góp cho công ty.</p>',                                                                                                                                          'EMP004', 'EMP002', 'PUBLISHED',        'INTERNAL', true,  false,  8),
    ('ART_20260703',   'Sản phẩm mới: Rau hữu cơ Đà Lạt',          '<p>FTECH tự hào giới thiệu dòng sản phẩm <strong>rau hữu cơ Đà Lạt</strong> mới - đạt tiêu chuẩn VietGAP.</p><p>Đặt hàng ngay hôm nay để nhận ưu đãi 20%.</p>',                                                                                                                                                  'EMP006', 'EMP005', 'PUBLISHED',        'PUBLIC',   true,  true,   5),
    ('ART_20260704',   'Chính sách bảo hiểm y tế 2026',            '<p>Từ 01/01/2026, chính sách BHYT của công ty được cập nhật:</p><ul><li>Mức đóng: 4.5% lương cơ bản</li><li>Bảo lãnh viện phí trực tiếp tại 50+ bệnh viện</li></ul>',                                                                                                                                            'EMP004', 'EMP002', 'APPROVED',         'INTERNAL', true,  false,  3),
    ('ART_20260705',   'Roadmap sản phẩm Q4 2026',                 '<p>Bản nháp roadmap Q4 - đang chờ review từ CTO.</p><p>Các milestone chính: <em>module thanh toán, tích hợp AI, mobile app v2.</em></p>',                                                                                                                                                                        'EMP003', NULL,      'DRAFT',           'INTERNAL', false, false,  2),
    ('ART_20260706',   'Đối tác chiến lược với ABC Corp',           '<p>FTECH ký kết hợp tác chiến lược với <strong>ABC Corporation</strong> - mở rộng thị trường ĐNÁ.</p>',                                                                                                                                                                                                          'EMP006', 'EMP005', 'PUBLISHED',        'PUBLIC',   true,  true,  10),
    ('ART_20260707',   'Hướng dẫn sử dụng hệ thống ERP mới',       '<p>Tài liệu hướng dẫn nhân viên sử dụng các module chính của ERP:</p><ol><li>Chấm công</li><li>Đơn nghỉ phép</li><li>Quản lý task</li></ol>',                                                                                                                                                                    'EMP003', 'EMP001', 'WAITING_APPROVAL', 'INTERNAL', false, false,  1),
    ('ART_20260708',   'Tuyển dụng Kỹ sư Backend Q3',              '<p>FTECH mở tuyển <strong>3 vị trí Kỹ sư Backend Senior</strong> - làm việc tại HN & HCM.</p><p>Yêu cầu: 3+ năm Spring Boot, PostgreSQL, K8s.</p><p>Mức lương: <em>25-40M</em>.</p>',                                                                                                                              'EMP004', 'EMP002', 'PUBLISHED',        'PUBLIC',   true,  true,   6)
) AS v(code, title, content, author_code, manager_code, status, scope, is_active, is_public, days_ago)
WHERE NOT EXISTS (SELECT 1 FROM articles a WHERE a.code = v.code);

-- ============================================================
-- 13) EMAIL TEMPLATES — 4 mẫu email
-- ============================================================
INSERT INTO email_templates (id, code, name, subject, content, description,
                             is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.subject, v.content, v.description,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('WELCOME',      'Chào mừng nhân viên mới',   'Chào mừng bạn đến với FTECH!',           '<h2>Xin chào {{name}}!</h2><p>Chào mừng bạn gia nhập đại gia đình FTECH. Chúng tôi rất vui khi bạn là thành viên mới.</p><p>Trân trọng,<br/>Team HR</p>',                              'Gửi cho nhân viên onboarding.'),
    ('LEAVE_APPROVE','Duyệt đơn nghỉ phép',        'Đơn nghỉ phép của bạn đã được duyệt',    '<p>Xin chào {{name}},</p><p>Đơn xin nghỉ phép từ <strong>{{start}}</strong> đến <strong>{{end}}</strong> của bạn đã được phê duyệt.</p><p>Chúc bạn có kỳ nghỉ vui vẻ!</p>',            'Gửi khi manager duyệt đơn.'),
    ('LEAVE_REJECT', 'Từ chối đơn nghỉ phép',      'Đơn nghỉ phép của bạn bị từ chối',       '<p>Xin chào {{name}},</p><p>Rất tiếc, đơn nghỉ phép của bạn từ chối với lý do: <em>{{reason}}</em></p>',                                                                              'Gửi khi manager từ chối.'),
    ('CUSTOMER_WELCOME','Cảm ơn khách hàng mới', 'Cảm ơn bạn đã trở thành khách hàng của FTECH','<h2>Xin chào {{name}}!</h2><p>Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của FTECH. Chúng tôi cam kết mang đến trải nghiệm tốt nhất.</p><p>Trân trọng,<br/>Sales Team</p>',              'Gửi cho khách hàng mới đăng ký.')
) AS v(code, name, subject, content, description)
WHERE NOT EXISTS (SELECT 1 FROM email_templates e WHERE e.code = v.code);

-- ============================================================
-- 13b) EMAIL CONFIG — MailHog local (dev) — activated=true
--     Host/port placeholder; không hardcode SMTP password thật.
--     Cần MailHog (hoặc SMTP tương đương) listen localhost:1025 để gửi được.
--     Bulk send lấy config qua findByActivatedTrue() — thiếu row activated → 400
--     error.email.config.not.found.
-- ============================================================
INSERT INTO email_configs (id, code, name, api_key, smtp, port, name_email, org_id, activated,
                           is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.api_key, v.smtp, v.port, v.name_email, NULL, true,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('MAILHOG_LOCAL', 'MailHog Local (dev)', 'unused', 'localhost', 1025::smallint, 'noreply@frezo.local')
) AS v(code, name, api_key, smtp, port, name_email)
WHERE NOT EXISTS (SELECT 1 FROM email_configs e WHERE e.code = v.code);

-- LNK-09: upsert — nếu row đã có (activated=false / sai host) → ép activated + MailHog local
UPDATE email_configs
SET activated = true,
    smtp = 'localhost',
    port = 1025,
    name_email = COALESCE(NULLIF(name_email, ''), 'noreply@frezo.local'),
    is_deleted = false,
    updated_date = NOW(),
    updated_by = 'system'
WHERE code = 'MAILHOG_LOCAL';

-- ============================================================
-- 14) CATEGORY GROUP 'LoaiSanPham' + 8 loại sản phẩm cho trang /loai-san-pham
-- ============================================================
INSERT INTO category_group (code, name, cat_group, is_deleted)
SELECT 'LoaiSanPham', 'Loại sản phẩm', 10, false
WHERE NOT EXISTS (SELECT 1 FROM category_group WHERE code = 'LoaiSanPham');

INSERT INTO categories (id, code, name, name_en, short_name, group_code, order_index, description, active, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, v.short_name, 'LoaiSanPham', v.order_index, v.description, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('LSP_VEG',   'Rau củ',           'Vegetables',   'RAU',   1, 'Rau xanh, củ quả tươi.'),
    ('LSP_FRUIT', 'Trái cây',         'Fruits',       'HOA',   2, 'Trái cây tươi trong nước & nhập khẩu.'),
    ('LSP_MEAT',  'Thịt tươi',        'Fresh Meat',   'THIT',  3, 'Thịt bò, heo, gà tươi trong ngày.'),
    ('LSP_FISH',  'Hải sản',          'Seafood',      'HS',    4, 'Tôm, cá, cua biển các loại.'),
    ('LSP_RICE',  'Gạo & Ngũ cốc',    'Rice & Grains','GAO',   5, 'Gạo, đậu, ngũ cốc.'),
    ('LSP_DAIRY', 'Sữa & Trứng',      'Dairy & Eggs', 'SUA',   6, 'Sữa tươi, trứng gà, sữa chua.'),
    ('LSP_BEV',   'Đồ uống',          'Beverages',    'UONG',  7, 'Nước ép, cafe, trà.'),
    ('LSP_SPICE', 'Gia vị & Đồ khô',  'Spices',       'GV',    8, 'Muối, đường, tiêu, đồ khô.')
) AS v(code, name, name_en, short_name, order_index, description)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.group_code = 'LoaiSanPham' AND c.code = v.code);

-- Gán category_id cho products đã seed (idempotent — chỉ update khi NULL)
UPDATE products SET category_id = (SELECT id FROM categories WHERE code = 'LSP_VEG'   AND group_code = 'LoaiSanPham' LIMIT 1) WHERE code IN ('SP001','SP003') AND category_id IS NULL;
UPDATE products SET category_id = (SELECT id FROM categories WHERE code = 'LSP_FRUIT' AND group_code = 'LoaiSanPham' LIMIT 1) WHERE code IN ('SP002','SP004','SP005','SP006','SP010','SP011') AND category_id IS NULL;
UPDATE products SET category_id = (SELECT id FROM categories WHERE code = 'LSP_FISH'  AND group_code = 'LoaiSanPham' LIMIT 1) WHERE code IN ('SP007','SP008') AND category_id IS NULL;
UPDATE products SET category_id = (SELECT id FROM categories WHERE code = 'LSP_RICE'  AND group_code = 'LoaiSanPham' LIMIT 1) WHERE code = 'SP009' AND category_id IS NULL;
UPDATE products SET category_id = (SELECT id FROM categories WHERE code = 'LSP_DAIRY' AND group_code = 'LoaiSanPham' LIMIT 1) WHERE code = 'SP012' AND category_id IS NULL;

-- ============================================================
-- 15) CONTRACTS — 10 hợp đồng lao động (mỗi nhân viên EMP001..EMP010 có 1 HĐ)
--     Workflow: DRAFT → PENDING_APPROVAL → ACTIVE
--     - person_id       : nhân viên (Bên B)
--     - employer_name   : FTECH (Bên A)
--     - status          : 6 EMP đầu = ACTIVE, 2 kế = PENDING_APPROVAL, 2 cuối = DRAFT
--     - type_contract_id: UUID unique (constraint entity yêu cầu unique)
-- ============================================================
INSERT INTO contract (id, code, name, person_id, type_contract_id,
                      eff_from, eff_to, value, status, activated, html_contract,
                      employer_name, employer_address, employer_tax_code,
                      employee_id_number, employee_dob, job_position, work_location,
                      probation_days, allowance, ai_status,
                      is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name,
       (SELECT id FROM person WHERE code = v.person_code LIMIT 1),
       gen_random_uuid()::text,
       v.eff_from::date, v.eff_to::date, v.value, v.status, v.activated, v.html_contract,
       'Tổng Công ty Công nghệ FTECH',
       'Tầng 10, Tòa nhà FTECH Tower, Số 1 Phạm Văn Bạch, Cầu Giấy, Hà Nội',
       '0101234567',
       v.id_number, v.dob::date, v.job_position, v.work_location,
       v.probation_days, v.allowance, 'NONE',
       false, NOW() - '30 days'::interval, 'system', NOW(), 'system'
FROM (VALUES
    ('HD_2026_001', 'HĐLĐ không xác định thời hạn - Nguyễn Văn Hùng',   'EMP001', '2020-01-01', '2099-12-31', 45000000, 'ACTIVE',           true,  '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_001</p><p>Bên A: Tổng Công ty Công nghệ FTECH</p><p>Bên B: Nguyễn Văn Hùng</p><p>Vị trí: Trưởng phòng IT.</p>',                    '001088012345', '1988-03-12', 'Trưởng phòng IT',        'Hà Nội', 0,  '5.000.000 phụ cấp trách nhiệm'),
    ('HD_2026_002', 'HĐLĐ không xác định thời hạn - Trần Thị Mai',      'EMP002', '2019-06-01', '2099-12-31', 40000000, 'ACTIVE',           true,  '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_002</p><p>Bên B: Trần Thị Mai — Trưởng phòng Nhân sự.</p>',                                                                        '001192067890', '1992-07-08', 'Trưởng phòng Nhân sự',   'Hà Nội', 0,  '4.000.000 phụ cấp trách nhiệm'),
    ('HD_2026_003', 'HĐLĐ có xác định thời hạn 3 năm - Lê Minh Tuấn',    'EMP003', '2024-01-01', '2027-01-01', 30000000, 'ACTIVE',           true,  '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_003</p><p>Bên B: Lê Minh Tuấn — Kỹ sư phần mềm Senior.</p>',                                                                      '001190025431', '1990-11-25', 'Kỹ sư phần mềm Senior',  'Hà Nội', 60, '2.000.000 phụ cấp ăn trưa'),
    ('HD_2026_004', 'HĐLĐ có xác định thời hạn 2 năm - Phạm Thu Hà',     'EMP004', '2025-05-01', '2027-05-01', 18000000, 'PENDING_APPROVAL', false, '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_004</p><p>Bên B: Phạm Thu Hà — Chuyên viên tuyển dụng.</p>',                                                                       '001195051401', '1995-05-14', 'Chuyên viên tuyển dụng', 'Hà Nội', 60, '1.500.000 phụ cấp'),
    ('HD_2026_005', 'HĐLĐ không xác định thời hạn - Hoàng Đức Anh',      'EMP005', '2021-09-01', '2099-12-31', 42000000, 'ACTIVE',           true,  '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_005</p><p>Bên B: Hoàng Đức Anh — Trưởng phòng Kinh doanh.</p>',                                                                    '001193093015', '1993-09-30', 'Trưởng phòng Kinh doanh','Hà Nội', 0,  '5.000.000 phụ cấp trách nhiệm + hoa hồng'),
    ('HD_2026_006', 'HĐLĐ có xác định thời hạn 3 năm - Vũ Ngọc Bích',    'EMP006', '2024-03-01', '2027-03-01', 15000000, 'PENDING_APPROVAL', false, '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_006</p><p>Bên B: Vũ Ngọc Bích — Chuyên viên Marketing.</p>',                                                                     '001194021802', '1994-02-18', 'Chuyên viên Marketing',  'Hà Nội', 30, '1.500.000 phụ cấp'),
    ('HD_2026_007', 'HĐLĐ có xác định thời hạn 3 năm - Đặng Quốc Bảo',   'EMP007', '2023-07-01', '2026-07-01', 28000000, 'ACTIVE',           true,  '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_007</p><p>Bên B: Đặng Quốc Bảo — Kỹ sư phần mềm.</p>',                                                                             '001191060501', '1991-06-05', 'Kỹ sư phần mềm',         'Hà Nội', 60, '2.000.000 phụ cấp ăn trưa'),
    ('HD_2026_008', 'HĐLĐ không xác định thời hạn - Bùi Thanh Loan',     'EMP008', '2018-04-01', '2099-12-31', 38000000, 'ACTIVE',           true,  '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_008</p><p>Bên B: Bùi Thanh Loan — Kế toán trưởng.</p>',                                                                            '001196120103', '1996-12-01', 'Kế toán trưởng',         'Hà Nội', 0,  '4.000.000 phụ cấp trách nhiệm'),
    ('HD_2026_009', 'HĐLĐ thời vụ 6 tháng - Ngô Xuân Khang',              'EMP009', '2026-04-01', '2026-10-01', 22000000, 'DRAFT',            false, '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_009</p><p>Bên B: Ngô Xuân Khang — Kỹ sư QA.</p><p><em>Bản nháp — chưa duyệt.</em></p>',                                             '001189082201', '1989-08-22', 'Kỹ sư QA',               'Hà Nội', 30, NULL),
    ('HD_2026_010', 'HĐLĐ thử việc 2 tháng - Đỗ Thu Trang',               'EMP010', '2026-07-01', '2026-09-01', 12000000, 'DRAFT',            false, '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_2026_010</p><p>Bên B: Đỗ Thu Trang — Chuyên viên hành chính.</p><p><em>Bản nháp — chờ HR chốt.</em></p>',                              '001197041004', '1997-04-10', 'Chuyên viên hành chính', 'Hà Nội', 60, NULL)
) AS v(code, name, person_code, eff_from, eff_to, value, status, activated, html_contract, id_number, dob, job_position, work_location, probation_days, allowance)
WHERE NOT EXISTS (SELECT 1 FROM contract c WHERE c.code = v.code);

-- ============================================================
-- 15b) ACTIVE backfill — EMP chưa có HĐ đang hoạt động (EMP004/006/009/010)
--      Giữ nguyên HD_2026_* DRAFT/PENDING cho demo workflow.
--      Auth/Mobile resolve contract qua activated=true.
-- ============================================================
INSERT INTO contract (id, code, name, person_id, type_contract_id,
                      eff_from, eff_to, value, status, activated, html_contract,
                      employer_name, employer_address, employer_tax_code,
                      employee_id_number, employee_dob, job_position, work_location,
                      probation_days, allowance, ai_status,
                      is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name,
       (SELECT id FROM person WHERE code = v.person_code LIMIT 1),
       v.type_contract_id,
       v.eff_from::date, v.eff_to::date, v.value, 'ACTIVE', true, v.html_contract,
       'Tổng Công ty Công nghệ FTECH',
       'Tầng 10, Tòa nhà FTECH Tower, Số 1 Phạm Văn Bạch, Cầu Giấy, Hà Nội',
       '0101234567',
       v.id_number, v.dob::date, v.job_position, v.work_location,
       v.probation_days, v.allowance, 'NONE',
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('HD_ACTIVE_EMP004', 'HĐLĐ đang hiệu lực - Phạm Thu Hà',     'EMP004', 'TYPE_ACTIVE_CHINH_THUC_EMP004', '2025-05-01', '2099-12-31', 18000000,
     '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_ACTIVE_EMP004</p><p>Bên B: Phạm Thu Hà</p>', '001195051401', '1995-05-14', 'Chuyên viên tuyển dụng', 'Hà Nội', 0,  '1.500.000 phụ cấp'),
    ('HD_ACTIVE_EMP006', 'HĐLĐ đang hiệu lực - Vũ Ngọc Bích',    'EMP006', 'TYPE_ACTIVE_CHINH_THUC_EMP006', '2024-03-01', '2099-12-31', 15000000,
     '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_ACTIVE_EMP006</p><p>Bên B: Vũ Ngọc Bích</p>', '001194021802', '1994-02-18', 'Chuyên viên Marketing',  'Hà Nội', 0,  '1.500.000 phụ cấp'),
    ('HD_ACTIVE_EMP009', 'HĐLĐ đang hiệu lực - Ngô Xuân Khang',   'EMP009', 'TYPE_ACTIVE_CHINH_THUC_EMP009', '2026-04-01', '2099-12-31', 22000000,
     '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_ACTIVE_EMP009</p><p>Bên B: Ngô Xuân Khang</p>', '001189082201', '1989-08-22', 'Kỹ sư QA',               'Hà Nội', 0,  NULL),
    ('HD_ACTIVE_EMP010', 'HĐLĐ đang hiệu lực - Đỗ Thu Trang',    'EMP010', 'TYPE_ACTIVE_CHINH_THUC_EMP010', '2026-07-01', '2099-12-31', 12000000,
     '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_ACTIVE_EMP010</p><p>Bên B: Đỗ Thu Trang</p>', '001197041004', '1997-04-10', 'Chuyên viên hành chính', 'Hà Nội', 0,  NULL)
) AS v(code, name, person_code, type_contract_id, eff_from, eff_to, value, html_contract, id_number, dob, job_position, work_location, probation_days, allowance)
WHERE NOT EXISTS (SELECT 1 FROM contract c WHERE c.code = v.code)
  AND EXISTS (SELECT 1 FROM person p WHERE p.code = v.person_code)
  AND NOT EXISTS (
      SELECT 1 FROM contract c
      JOIN person p ON p.id = c.person_id
      WHERE p.code = v.person_code
        AND COALESCE(c.is_deleted, false) = false
        AND (c.activated = true OR c.status = 'ACTIVE')
  );

-- ============================================================
-- 16) LEAVE REQUESTS — 6 đơn nghỉ phép (mix PENDING/APPROVED/REJECTED)
--     Duyệt bởi maitt (HR Manager)
-- ============================================================
INSERT INTO leave_request (id, contract_id, person_id, leave_type, start_date, end_date,
                           duration_days, reason, status,
                           approved_by, approved_at, rejected_by, reject_reason,
                           is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM contract WHERE code = v.contract_code LIMIT 1),
       (SELECT id FROM person   WHERE code = v.person_code   LIMIT 1),
       v.leave_type, v.start_date::date, v.end_date::date, v.duration_days, v.reason, v.status,
       v.approved_by, v.approved_at::date, v.rejected_by, v.reject_reason,
       false, NOW() - (v.created_days_ago || ' days')::interval, COALESCE(v.person_code, 'system'), NOW(), 'system'
FROM (VALUES
    ('HD_2026_003', 'EMP003', 'annual', '2026-07-20', '2026-07-22', 3.0,  'Về quê thăm gia đình.',                     'APPROVED', 'maitt', '2026-07-14', NULL,     NULL,                                     5),
    ('HD_2026_007', 'EMP007', 'sick',   '2026-07-15', '2026-07-15', 1.0,  'Nghỉ ốm - đau đầu, sốt nhẹ.',               'APPROVED', 'maitt', '2026-07-15', NULL,     NULL,                                     2),
    ('HD_2026_004', 'EMP004', 'annual', '2026-08-01', '2026-08-05', 5.0,  'Du lịch cùng gia đình.',                    'PENDING',  NULL,     NULL,        NULL,     NULL,                                     1),
    ('HD_2026_006', 'EMP006', 'annual', '2026-07-25', '2026-07-26', 2.0,  'Việc gia đình.',                            'PENDING',  NULL,     NULL,        NULL,     NULL,                                     3),
    ('HD_2026_009', 'EMP009', 'unpaid', '2026-07-30', '2026-08-02', 4.0,  'Xin nghỉ không lương lo việc riêng.',       'REJECTED', NULL,     NULL,        'maitt',  'Trùng deadline release, nghỉ sau ngày 5/8.', 4),
    ('HD_2026_010', 'EMP010', 'annual', '2026-08-10', '2026-08-11', 2.0,  'Về giỗ ông nội.',                            'PENDING',  NULL,     NULL,        NULL,     NULL,                                     0)
) AS v(contract_code, person_code, leave_type, start_date, end_date, duration_days, reason, status, approved_by, approved_at, rejected_by, reject_reason, created_days_ago)
WHERE EXISTS (SELECT 1 FROM contract WHERE code = v.contract_code)
  AND NOT EXISTS (
      SELECT 1 FROM leave_request lr
      WHERE lr.person_id = (SELECT id FROM person WHERE code = v.person_code LIMIT 1)
        AND lr.start_date = v.start_date::date
        AND lr.end_date   = v.end_date::date
  );

-- ============================================================
-- 17) ATTENDANCE — 10 nhân viên x 7 ngày gần nhất (không tính CN)
--     Idempotent: unique(person_id, attendance_date)
-- ============================================================
INSERT INTO attendance (id, contract_id, person_id, attendance_date,
                        check_in_time, check_out_time, work_minutes, late_minutes, overtime_minutes,
                        shift_type, status, note,
                        is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM contract WHERE person_id = p.id ORDER BY created_date DESC LIMIT 1),
       p.id,
       d::date,
       -- Random-ish check-in 07:55..08:35 dựa trên hash (person + date) để dữ liệu ổn định giữa các lần seed
       (TIME '08:00' + ((abs(hashtext(p.code || d::text)) % 40 - 5) || ' minutes')::interval)::time,
       (TIME '17:30' + ((abs(hashtext(p.code || d::text)) % 60)     || ' minutes')::interval)::time,
       480 + (abs(hashtext(p.code || d::text)) % 60),
       GREATEST(0, (abs(hashtext(p.code || d::text)) % 40) - 5),
       (abs(hashtext(p.code || d::text)) % 60),
       'FULL',
       CASE WHEN (abs(hashtext(p.code || d::text)) % 40) > 30 THEN 'LATE'
            WHEN (abs(hashtext(p.code || d::text)) % 20) = 0  THEN 'PRESENT'
            ELSE 'PRESENT' END,
       CASE WHEN (abs(hashtext(p.code || d::text)) % 40) > 30 THEN 'Đi muộn (kẹt xe).' ELSE NULL END,
       false, NOW(), 'system', NOW(), 'system'
FROM person p
CROSS JOIN generate_series(CURRENT_DATE - INTERVAL '9 days', CURRENT_DATE - INTERVAL '1 day', INTERVAL '1 day') AS d
WHERE p.code LIKE 'EMP%'
  AND EXTRACT(DOW FROM d) NOT IN (0, 6)  -- bỏ Chủ nhật(0) & Thứ 7(6)
  AND EXISTS (SELECT 1 FROM contract c WHERE c.person_id = p.id)
  AND NOT EXISTS (
      SELECT 1 FROM attendance a WHERE a.person_id = p.id AND a.attendance_date = d::date
  );

-- ============================================================
-- 17b) INSURANCE / TAX / PAYROLL CONFIG — Chuẩn Việt Nam
--      Bắt buộc phải có mới tính lương được (nếu thiếu, engine sẽ trả về
--      lương gross+net nhưng deductions = 0). Các con số bên dưới lấy theo
--      Nghị định 145/2020, Luật BHXH & Thông tư 111/2013 (áp dụng 2024-2026).
-- ============================================================

-- Insurance config 2024, 2025, 2026 (đề phòng calculate ở nhiều năm)
INSERT INTO insurance_config (id, year, social_insurance_rate, health_insurance_rate, unemployment_insurance_rate,
                              employer_social_rate, employer_health_rate, employer_unemployment_rate, employer_accident_rate,
                              max_insurance_salary, base_minimum_wage, regional_minimum_wage, region,
                              applies_from, applies_to, is_active,
                              is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.year,
       0.08, 0.015, 0.01,      -- Employee: BHXH 8% · BHYT 1.5% · BHTN 1%
       0.175, 0.03, 0.01, 0.005, -- Employer: BHXH 17.5% · BHYT 3% · BHTN 1% · BHTNLĐ-BNN 0.5%
       46800000, 2340000, 4960000, 'I', -- Max = 20 × lương cơ sở 2.34M; min vùng I = 4.96M
       1, 12, true,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES (2024), (2025), (2026)) AS v(year)
WHERE NOT EXISTS (
    SELECT 1 FROM insurance_config ic WHERE ic.year = v.year AND ic.is_deleted = false
);

-- Tax config (TNCN) 2024, 2025, 2026 — 7 bậc lũy tiến, giảm trừ bản thân 11M, phụ thuộc 4.4M
INSERT INTO tax_config (id, year, bracket_order, from_amount, to_amount, rate, deduct_amount,
                        personal_deduction, dependent_deduction, is_active,
                        is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.year, v.bracket, v.from_amt, v.to_amt, v.rate, v.deduct,
       11000000, 4400000, true,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    (1, 0,        5000000,  0.05, 0),
    (2, 5000000,  10000000, 0.10, 250000),
    (3, 10000000, 18000000, 0.15, 750000),
    (4, 18000000, 32000000, 0.20, 1650000),
    (5, 32000000, 52000000, 0.25, 3250000),
    (6, 52000000, 80000000, 0.30, 5850000),
    (7, 80000000, NULL,     0.35, 9850000)
) AS v(bracket, from_amt, to_amt, rate, deduct)
CROSS JOIN (VALUES (2024), (2025), (2026)) AS y(year)
WHERE NOT EXISTS (
    SELECT 1 FROM tax_config tc
    WHERE tc.year = y.year AND tc.bracket_order = v.bracket AND tc.is_deleted = false
);

-- Payroll config (default org=NULL — áp dụng toàn hệ thống nếu org chưa có riêng)
INSERT INTO payroll_config (id, org_id, year, standard_working_days, standard_hours_per_day,
                            overtime_normal_rate, overtime_weekend_rate, overtime_holiday_rate, overtime_night_rate,
                            late_penalty_per_minute, union_due_rate, max_union_due, is_active,
                            is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), NULL, v.year,
       22, 8,
       1.50, 2.00, 3.00, 1.30,     -- OT theo Luật LĐ điều 98: thường 150%, cuối tuần 200%, lễ 300%, đêm +30%
       5000, 0.01, NULL, true,     -- Phạt đi muộn 5.000đ/phút · Công đoàn phí 1% gross
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES (2024), (2025), (2026)) AS v(year)
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_config pc
    WHERE pc.year = v.year AND pc.org_id IS NULL AND pc.is_deleted = false
);

-- ============================================================
-- 18) PAYROLL — 10 nhân viên x 2 tháng gần nhất (tháng trước = Paid, tháng này = Draft)
-- ============================================================
INSERT INTO payroll (id, person_id, contract_id, month, year,
                     basic_salary, insurance_salary,
                     standard_days, working_days, actual_working_days,
                     leaves_paid, leaves_unpaid, total_late_minutes, late_penalty,
                     overtime_hours_normal, overtime_hours_weekend, overtime_hours_holiday, overtime_pay,
                     allowance, bonus,
                     gross_salary, social_insurance, health_insurance, unemployment_insurance, total_insurance,
                     taxable_income, tax_income, union_fee, advance_deduction, other_deductions, total_deductions,
                     net_salary, status, paid_at, note,
                     is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       p.id,
       c.id,
       v.month, v.year,
       (c.value)::numeric, (c.value * 0.8)::numeric,
       22, 22, 22.0,
       0, 0, 30, 100000::numeric,
       4.0, 2.0, 0.0, 800000::numeric,
       2000000::numeric, v.bonus::numeric,
       (c.value + 2000000 + v.bonus + 800000)::numeric,
       (c.value * 0.08)::numeric, (c.value * 0.015)::numeric, (c.value * 0.01)::numeric, (c.value * 0.105)::numeric,
       (c.value * 0.895 + 800000)::numeric, (c.value * 0.0)::numeric, 50000::numeric, 0::numeric, 100000::numeric, (c.value * 0.105 + 250000)::numeric,
       (c.value * 0.895 + 2000000 + v.bonus + 800000 - 250000)::numeric,
       v.status, v.paid_at::date, v.note,
       false, NOW() - '30 days'::interval, 'system', NOW(), 'system'
FROM person p
JOIN contract c ON c.person_id = p.id AND c.is_deleted = false
CROSS JOIN (VALUES
    -- month | year | bonus     | status | paid_at        | note
    (EXTRACT(MONTH FROM CURRENT_DATE - INTERVAL '1 month')::int, EXTRACT(YEAR FROM CURRENT_DATE - INTERVAL '1 month')::int, 1500000, 3, TO_CHAR(CURRENT_DATE - INTERVAL '15 days', 'YYYY-MM-DD'), 'Đã trả lương tháng trước.'),
    (EXTRACT(MONTH FROM CURRENT_DATE)::int,                       EXTRACT(YEAR FROM CURRENT_DATE)::int,                       0,       1, NULL,                                                    'Bảng lương tháng hiện tại - đang draft.')
) AS v(month, year, bonus, status, paid_at, note)
WHERE p.code LIKE 'EMP%'
  AND c.status IN ('ACTIVE', 'PENDING_APPROVAL')
  AND NOT EXISTS (
      SELECT 1 FROM payroll pr
      WHERE pr.person_id = p.id AND pr.month = v.month AND pr.year = v.year
  );

-- ============================================================
-- 18b) TICKET CATEGORIES — master danh mục VI (FR-TASK-CAT)
--      code giữ enum cũ để tickets.category không cần rewrite
-- ============================================================
INSERT INTO ticket_categories (id, code, name, sort_order, active, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.sort_order, true, false,
       NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('BUG',             'Lỗi',       1),
    ('FEATURE_REQUEST', 'Tính năng', 2),
    ('SUPPORT',         'Hỗ trợ',    3),
    ('OTHER',           'Khác',      4)
) AS v(code, name, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_categories t
    WHERE t.code = v.code AND COALESCE(t.is_deleted, false) = false
);

-- Đồng bộ tên VI nếu đã seed trước đó bằng tên EN
UPDATE ticket_categories SET name = 'Lỗi',       updated_date = NOW(), updated_by = 'system'
WHERE code = 'BUG' AND COALESCE(is_deleted, false) = false AND name IS DISTINCT FROM 'Lỗi';
UPDATE ticket_categories SET name = 'Tính năng', updated_date = NOW(), updated_by = 'system'
WHERE code = 'FEATURE_REQUEST' AND COALESCE(is_deleted, false) = false AND name IS DISTINCT FROM 'Tính năng';
UPDATE ticket_categories SET name = 'Hỗ trợ',    updated_date = NOW(), updated_by = 'system'
WHERE code = 'SUPPORT' AND COALESCE(is_deleted, false) = false AND name IS DISTINCT FROM 'Hỗ trợ';
UPDATE ticket_categories SET name = 'Khác',      updated_date = NOW(), updated_by = 'system'
WHERE code = 'OTHER' AND COALESCE(is_deleted, false) = false AND name IS DISTINCT FROM 'Khác';

-- ============================================================
-- 19) TICKETS — 8 ticket support/bug/feature
-- ============================================================
INSERT INTO tickets (id, code, title, description, status, priority, category,
                     reporter_id, assignee_id, due_date, resolved_at, resolution_note,
                     is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.title, v.description, v.status, v.priority, v.category,
       (SELECT id FROM person WHERE code = v.reporter_code LIMIT 1),
       (SELECT id FROM person WHERE code = v.assignee_code LIMIT 1),
       (CURRENT_DATE + (v.due_offset || ' days')::interval)::timestamp,
       (CASE WHEN v.status IN ('RESOLVED','CLOSED') THEN NOW() - (v.resolved_ago || ' days')::interval ELSE NULL END),
       v.resolution_note,
       false, NOW() - ((v.due_offset + 5) || ' days')::interval, 'system', NOW(), 'system'
FROM (VALUES
    ('TICKET-0001', 'Không đăng nhập được sau khi đổi mật khẩu',   'User báo lỗi 401 sau khi đổi password. Cần check JWT expiry.',                                    'RESOLVED',    'HIGH',   'BUG',             'EMP004', 'EMP003', -5, 3,  'Rehash password lại trong DB, đã xử lý.'),
    ('TICKET-0002', 'Đề xuất thêm dark mode cho ERP',                'Nhiều bạn phàn nàn giao diện chói mắt khi làm việc buổi tối.',                                     'IN_PROGRESS', 'MEDIUM', 'FEATURE_REQUEST', 'EMP006', 'EMP003',  7, 0, NULL),
    ('TICKET-0003', 'Export Excel bảng lương bị lỗi font',           'Font tiếng Việt bị vỡ khi export.',                                                                'OPEN',        'MEDIUM', 'BUG',             'EMP008', 'EMP007',  3, 0, NULL),
    ('TICKET-0004', 'Cần thêm filter theo phòng ban ở trang nhân sự','UX request: filter nhanh theo dept.',                                                               'IN_PROGRESS', 'LOW',    'FEATURE_REQUEST', 'EMP002', 'EMP003', 10, 0, NULL),
    ('TICKET-0005', 'Chấm công không nhận được GPS ở tầng 5',        'Kiểm tra lại radius wifi/GPS validation.',                                                          'OPEN',        'HIGH',   'BUG',             'EMP010', 'EMP009',  2, 0, NULL),
    ('TICKET-0006', 'Hỗ trợ tạo báo cáo doanh thu Q3',               'Cần template báo cáo mới cho ban giám đốc.',                                                        'OPEN',        'HIGH',   'SUPPORT',         'EMP005', 'EMP008',  5, 0, NULL),
    ('TICKET-0007', 'Cải thiện tốc độ load trang Articles',          'Load 570 records mất >3s. Cần pagination hoặc virtual scroll.',                                     'RESOLVED',    'MEDIUM', 'BUG',             'EMP006', 'EMP003', -3, 1,  'Đã convert sang server-side pagination.'),
    ('TICKET-0008', 'Yêu cầu tích hợp SSO Google Workspace',         'Đang có yêu cầu từ khách hàng ABC Corp.',                                                            'OPEN',        'URGENT', 'FEATURE_REQUEST', 'EMP005', 'EMP001', 14, 0, NULL)
) AS v(code, title, description, status, priority, category, reporter_code, assignee_code, due_offset, resolved_ago, resolution_note)
WHERE NOT EXISTS (SELECT 1 FROM tickets t WHERE t.code = v.code);

