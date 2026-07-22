-- Idempotent: tạo HĐ ACTIVE cho EMP* chưa có hợp đồng đang hoạt động.
-- Không sửa/xóa contract ACTIVE hiện có; DRAFT/PENDING demo giữ nguyên.
--
-- Cách chạy (psql / DBeaver / IntelliJ DB console):
--   \i module-qlns-bom/src/main/resources/data/contract_active_backfill.sql
-- hoặc paste toàn bộ file vào SQL client trỏ DB Frezo.
--
-- Ước lượng: ~4 rows trên demo chuẩn (EMP004, EMP006, EMP009, EMP010).
-- Restart app cũng chạy cùng logic qua ContractDataInitializer (Order 80).

INSERT INTO contract (id, code, name, person_id, type_contract_id,
                      eff_from, eff_to, value, status, activated, html_contract,
                      employer_name, employer_address, employer_tax_code,
                      employee_id_number, employee_dob, job_position, work_location,
                      probation_days, allowance, ai_status,
                      is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       'HD_ACTIVE_' || p.code,
       'HĐLĐ đang hiệu lực — ' || COALESCE(p.name, p.code),
       p.id,
       'TYPE_ACTIVE_CHINH_THUC_' || p.code,
       DATE '2024-01-01',
       DATE '2099-12-31',
       18000000,
       'ACTIVE',
       true,
       '<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: HD_ACTIVE_' || p.code || '</p><p>Bên B: '
           || COALESCE(p.name, p.code) || '</p><p>Trạng thái: ACTIVE</p>',
       'Tổng Công ty Công nghệ FTECH',
       'Tầng 10, FTECH Tower, Cầu Giấy, Hà Nội',
       '0101234567',
       NULL,
       p.dob,
       COALESCE(p.job_title, 'Nhân viên'),
       'Hà Nội',
       0,
       NULL,
       'NONE',
       false,
       NOW(),
       'system',
       NOW(),
       'system'
FROM person p
WHERE COALESCE(p.is_deleted, false) = false
  AND p.code LIKE 'EMP%'
  AND NOT EXISTS (
      SELECT 1 FROM contract c
      WHERE c.person_id = p.id
        AND COALESCE(c.is_deleted, false) = false
        AND (c.activated = true OR c.status = 'ACTIVE')
  )
  AND NOT EXISTS (
      SELECT 1 FROM contract c WHERE c.code = 'HD_ACTIVE_' || p.code
  );
