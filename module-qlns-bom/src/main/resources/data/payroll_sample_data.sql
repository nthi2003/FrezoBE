-- ============================================================
-- SCRIPT: SAMPLE PAYROLL DATA (3 tháng lịch sử đầy đủ)
-- Purpose: Cung cấp dữ liệu bảng lương phong phú cho FrezoMobile —
--          nhân viên mở app thấy ngay 3 tháng lương gần nhất, chi tiết
--          thu nhập / khấu trừ / BHXH / TNCN đúng chuẩn Nghị định
--          145/2020 + Luật BHXH + Thông tư 111/2013.
--
-- PRECONDITIONS:
--   * demo_data.sql đã chạy trước (Persons, Contracts, Payroll_config,
--     Insurance_config, Tax_config).
--   * Bảng payroll_period + payroll_detail đã tồn tại (schema QLNS).
--
-- IDEMPOTENT: Mọi INSERT/UPDATE đều dùng WHERE NOT EXISTS trên
--             (person_id, month, year) hoặc business key.
-- Layout:
--   1) PAYROLL_PERIOD              (3 kỳ: M-3, M-2, M-1)
--   2) PAYROLL (10 NV × 3 kỳ = 30) — đủ 3 status: PAID / PAID / CONFIRMED
--   3) PAYROLL_DETAIL              (đầu mục thu nhập & khấu trừ)
--   4) LEAVE_REQUEST               (thêm 4 đơn nữa cho FE mobile demo)
--   5) ATTENDANCE                  (bổ sung 30 ngày gần nhất — cover
--                                    cả OT, LATE, HOLIDAY, HALF_DAY)
-- ============================================================

-- ============================================================
-- 1) PAYROLL_PERIOD — 3 kỳ M-3, M-2, M-1
-- ============================================================
INSERT INTO payroll_period (id, code, name, month, year, start_date, end_date,
                            cutoff_date, pay_date, status,
                            is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       'PP_' || v.year || LPAD(v.month::text, 2, '0'),
       'Kỳ lương ' || LPAD(v.month::text, 2, '0') || '/' || v.year,
       v.month, v.year,
       MAKE_DATE(v.year, v.month, 1),
       (MAKE_DATE(v.year, v.month, 1) + INTERVAL '1 month' - INTERVAL '1 day')::date,
       (MAKE_DATE(v.year, v.month, 1) + INTERVAL '1 month' + INTERVAL '2 days')::date,
       (MAKE_DATE(v.year, v.month, 1) + INTERVAL '1 month' + INTERVAL '5 days')::date,
       v.status,
       false, NOW(), 'system', NOW(), 'system'
FROM (
  SELECT EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '3 months'))::int AS month,
         EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '3 months'))::int AS year,
         'PAID'::text AS status
  UNION ALL
  SELECT EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int,
         EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '2 months'))::int,
         'PAID'
  UNION ALL
  SELECT EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month'))::int,
         EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '1 month'))::int,
         'CONFIRMED'
) AS v
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_period pp
    WHERE pp.month = v.month AND pp.year = v.year AND pp.is_deleted = false
);

-- ============================================================
-- 2) PAYROLL — 10 NV × 3 tháng M-3, M-2, M-1
--    Chiến lược data:
--      * M-3, M-2: đã PAID (status=2), paid_at trong quá khứ.
--      * M-1:      CONFIRMED (status=1), chưa paid_at.
--      * Bonus/OT thay đổi theo tháng để chart FE trông sinh động.
--      * baseSalary lấy từ contract.value, insuranceSalary = 80% base.
--      * Tính BHXH/BHYT/BHTN/TNCN đúng % (đơn giản hoá — engine sẽ
--        recalculate khi trigger, đây là snapshot demo).
-- ============================================================
WITH month_series AS (
    SELECT 1 AS ord,
           EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '3 months'))::int AS m,
           EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '3 months'))::int AS y,
           2 AS status, 2500000::numeric AS bonus, 8.0::numeric AS ot_normal, 4.0::numeric AS ot_weekend,
           (CURRENT_DATE - INTERVAL '2 months + 20 days')::date AS paid_at,
           'Lương tháng ' || EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '3 months'))::int || ' đã chi.' AS note
    UNION ALL
    SELECT 2,
           EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int,
           EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '2 months'))::int,
           2, 1800000, 6.0, 2.0,
           (CURRENT_DATE - INTERVAL '1 month + 20 days')::date,
           'Lương tháng ' || EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int || ' đã chi.'
    UNION ALL
    SELECT 3,
           EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month'))::int,
           EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '1 month'))::int,
           1, 3200000, 10.0, 3.0,
           NULL,
           'Bảng lương tháng trước — đã confirmed, chờ ngày trả.'
),
person_contract AS (
    SELECT p.id AS pid, c.id AS cid, c.value AS base
    FROM person p
    JOIN contract c ON c.person_id = p.id AND c.is_deleted = false
    WHERE p.code LIKE 'EMP%' AND c.status IN ('ACTIVE','PENDING_APPROVAL')
)
INSERT INTO payroll (id, person_id, contract_id, month, year, payroll_period_id,
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
       pc.pid, pc.cid, ms.m, ms.y,
       (SELECT id FROM payroll_period WHERE month = ms.m AND year = ms.y AND is_deleted = false LIMIT 1),
       pc.base,
       LEAST(pc.base * 0.8, 46800000)::numeric,          -- capped by BHXH ceiling
       22, 22, 22.0,
       1, 0, 25, (25 * 5000)::numeric,                    -- 25' late × 5.000đ = 125.000
       ms.ot_normal, ms.ot_weekend, 0,
       ((pc.base / 22 / 8) * (ms.ot_normal * 1.5 + ms.ot_weekend * 2.0))::numeric,
       2000000::numeric,                                  -- phụ cấp ăn trưa + đi lại
       ms.bonus,
       -- gross = base + OT pay + allowance + bonus - late penalty
       (pc.base
        + ((pc.base / 22 / 8) * (ms.ot_normal * 1.5 + ms.ot_weekend * 2.0))
        + 2000000 + ms.bonus - (25 * 5000))::numeric,
       (LEAST(pc.base * 0.8, 46800000) * 0.08)::numeric,   -- BHXH 8%
       (LEAST(pc.base * 0.8, 46800000) * 0.015)::numeric,  -- BHYT 1.5%
       (LEAST(pc.base * 0.8, 46800000) * 0.01)::numeric,   -- BHTN 1%
       (LEAST(pc.base * 0.8, 46800000) * 0.105)::numeric,  -- total insurance NLĐ 10.5%
       -- taxable = gross - insurance - personal deduction 11M
       GREATEST(0,
         (pc.base
          + ((pc.base / 22 / 8) * (ms.ot_normal * 1.5 + ms.ot_weekend * 2.0))
          + 2000000 + ms.bonus - (25 * 5000)
          - LEAST(pc.base * 0.8, 46800000) * 0.105
          - 11000000)
       )::numeric,
       -- Thuế TNCN (simplified — engine sẽ tính đúng bậc luỹ tiến)
       GREATEST(0,
         (pc.base
          + ((pc.base / 22 / 8) * (ms.ot_normal * 1.5 + ms.ot_weekend * 2.0))
          + 2000000 + ms.bonus - (25 * 5000)
          - LEAST(pc.base * 0.8, 46800000) * 0.105
          - 11000000) * 0.15 - 750000
       )::numeric,
       50000::numeric,                                      -- công đoàn phí
       0::numeric,
       CASE WHEN ms.ord = 1 THEN 500000 ELSE 0 END::numeric, -- 1 khấu trừ khác ở tháng đầu
       0::numeric,                                          -- tổng deductions (recompute below)
       0::numeric,                                          -- net (recompute below)
       ms.status, ms.paid_at, ms.note,
       false, NOW() - ((3 - ms.ord + 1) || ' months')::interval, 'system', NOW(), 'system'
FROM person_contract pc
CROSS JOIN month_series ms
WHERE NOT EXISTS (
    SELECT 1 FROM payroll pr
    WHERE pr.person_id = pc.pid AND pr.month = ms.m AND pr.year = ms.y
);

-- Recompute total_deductions & net_salary từ các field đã insert (idempotent)
UPDATE payroll
SET total_deductions = COALESCE(total_insurance,0) + COALESCE(tax_income,0)
                       + COALESCE(union_fee,0) + COALESCE(advance_deduction,0)
                       + COALESCE(other_deductions,0),
    net_salary       = COALESCE(gross_salary,0)
                       - (COALESCE(total_insurance,0) + COALESCE(tax_income,0)
                          + COALESCE(union_fee,0) + COALESCE(advance_deduction,0)
                          + COALESCE(other_deductions,0))
WHERE month IN (
    SELECT EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month'))::int UNION
    SELECT EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '2 months'))::int UNION
    SELECT EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '3 months'))::int
)
AND (net_salary = 0 OR net_salary IS NULL);

-- ============================================================
-- 3) PAYROLL_DETAIL — chi tiết các khoản của bảng lương M-1 (mới nhất)
--    Mỗi payroll có 5 detail: 2 earning + 3 deduction
-- ============================================================
INSERT INTO payroll_detail (id, payroll_id, item_type, item_code, item_name,
                            amount, note,
                            is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), pr.id, v.item_type, v.item_code, v.item_name,
       CASE
         WHEN v.item_code = 'ALLOWANCE_LUNCH'     THEN 1200000::numeric
         WHEN v.item_code = 'ALLOWANCE_TRANSPORT' THEN 800000::numeric
         WHEN v.item_code = 'BHXH_EMP'            THEN pr.social_insurance
         WHEN v.item_code = 'BHYT_EMP'            THEN pr.health_insurance
         WHEN v.item_code = 'BHTN_EMP'            THEN pr.unemployment_insurance
         ELSE 0::numeric
       END,
       v.note,
       false, NOW(), 'system', NOW(), 'system'
FROM payroll pr
CROSS JOIN (VALUES
    ('EARNING',   'ALLOWANCE_LUNCH',     'Phụ cấp ăn trưa',              'Phụ cấp cố định 1.200.000/tháng'),
    ('EARNING',   'ALLOWANCE_TRANSPORT', 'Phụ cấp đi lại',               'Phụ cấp xăng xe 800.000/tháng'),
    ('DEDUCTION', 'BHXH_EMP',            'BHXH (8% lương BH)',           'Người lao động đóng 8% theo Luật BHXH'),
    ('DEDUCTION', 'BHYT_EMP',            'BHYT (1.5% lương BH)',         'Bảo hiểm y tế'),
    ('DEDUCTION', 'BHTN_EMP',            'BHTN (1% lương BH)',           'Bảo hiểm thất nghiệp')
) AS v(item_type, item_code, item_name, note)
WHERE pr.month = EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month'))::int
  AND pr.year  = EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '1 month'))::int
  AND NOT EXISTS (
      SELECT 1 FROM payroll_detail pd
      WHERE pd.payroll_id = pr.id AND pd.item_code = v.item_code
  );

-- ============================================================
-- 4) LEAVE_REQUEST — bổ sung 4 đơn cho FE mobile demo
--    (đã có 6 đơn từ demo_data.sql — thêm APPROVED + trending)
-- ============================================================
INSERT INTO leave_request (id, contract_id, person_id, leave_type, start_date, end_date,
                           duration_days, reason, status,
                           manager_username, manager_approved_by, manager_approved_at,
                           hr_approved_by, hr_approved_at,
                           is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM contract WHERE code = v.contract_code LIMIT 1),
       (SELECT id FROM person   WHERE code = v.person_code   LIMIT 1),
       v.leave_type, v.start_date::date, v.end_date::date, v.duration_days, v.reason, v.status,
       v.manager_username, v.manager_approved_by, v.manager_approved_at::date,
       v.hr_approved_by, v.hr_approved_at::date,
       false, NOW() - (v.created_days_ago || ' days')::interval, v.person_code, NOW(), 'system'
FROM (VALUES
    -- Đã duyệt bởi cả manager & HR — hiển thị trạng thái APPROVED xanh
    ('HD_2026_003', 'EMP003', 'annual', TO_CHAR(CURRENT_DATE - INTERVAL '25 days', 'YYYY-MM-DD'),
                                        TO_CHAR(CURRENT_DATE - INTERVAL '24 days', 'YYYY-MM-DD'),
                                        2.0, 'Nghỉ phép sinh nhật vợ.', 'APPROVED',
                                        'hungnv', 'hungnv', TO_CHAR(CURRENT_DATE - INTERVAL '27 days', 'YYYY-MM-DD'),
                                        'maitt',  TO_CHAR(CURRENT_DATE - INTERVAL '26 days', 'YYYY-MM-DD'), 30),
    ('HD_2026_007', 'EMP007', 'annual', TO_CHAR(CURRENT_DATE - INTERVAL '10 days', 'YYYY-MM-DD'),
                                        TO_CHAR(CURRENT_DATE - INTERVAL '10 days', 'YYYY-MM-DD'),
                                        1.0, 'Đi khám sức khoẻ định kỳ.', 'APPROVED',
                                        'hungnv', 'hungnv', TO_CHAR(CURRENT_DATE - INTERVAL '12 days', 'YYYY-MM-DD'),
                                        'maitt',  TO_CHAR(CURRENT_DATE - INTERVAL '11 days', 'YYYY-MM-DD'), 15),
    -- Đang chờ manager duyệt (từ nhân viên EMP003 gửi hôm nay)
    ('HD_2026_003', 'EMP003', 'annual', TO_CHAR(CURRENT_DATE + INTERVAL '14 days', 'YYYY-MM-DD'),
                                        TO_CHAR(CURRENT_DATE + INTERVAL '16 days', 'YYYY-MM-DD'),
                                        3.0, 'Nghỉ phép du lịch Đà Nẵng cùng gia đình.', 'PENDING_MANAGER',
                                        'hungnv', NULL, NULL, NULL, NULL, 1),
    -- Đơn ốm mới, manager đã duyệt, chờ HR
    ('HD_2026_007', 'EMP007', 'sick',   TO_CHAR(CURRENT_DATE - INTERVAL '2 days', 'YYYY-MM-DD'),
                                        TO_CHAR(CURRENT_DATE - INTERVAL '1 day',  'YYYY-MM-DD'),
                                        2.0, 'Bị cảm cúm, có giấy khám của BV Bạch Mai.', 'PENDING_HR',
                                        'hungnv', 'hungnv', TO_CHAR(CURRENT_DATE - INTERVAL '2 days', 'YYYY-MM-DD'),
                                        NULL, NULL, 3)
) AS v(contract_code, person_code, leave_type, start_date, end_date, duration_days, reason, status,
       manager_username, manager_approved_by, manager_approved_at, hr_approved_by, hr_approved_at, created_days_ago)
WHERE EXISTS (SELECT 1 FROM contract WHERE code = v.contract_code)
  AND NOT EXISTS (
      SELECT 1 FROM leave_request lr
      WHERE lr.person_id = (SELECT id FROM person WHERE code = v.person_code LIMIT 1)
        AND lr.start_date = v.start_date::date
        AND lr.end_date   = v.end_date::date
        AND lr.reason     = v.reason
  );

-- ============================================================
-- 5) ATTENDANCE — bổ sung dữ liệu cover status đa dạng
--    Chỉ thêm cho EMP003 (Tuấn — Dev Senior) để demo mobile chi tiết.
--    30 ngày gần nhất, mix LATE / OT / HALF_DAY / HOLIDAY.
-- ============================================================
INSERT INTO attendance (id, contract_id, person_id, attendance_date,
                        check_in_time, check_in_latitude, check_in_longitude, check_in_wifi_ssid,
                        check_out_time, check_out_latitude, check_out_longitude, check_out_wifi_ssid,
                        work_minutes, late_minutes, overtime_minutes,
                        shift_type, status, note,
                        is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(),
       (SELECT id FROM contract WHERE code = 'HD_2026_003' LIMIT 1),
       (SELECT id FROM person   WHERE code = 'EMP003'      LIMIT 1),
       d::date,
       -- check_in giờ khác nhau tuỳ ngày: 07:55 - 09:15
       CASE (abs(hashtext('EMP003' || d::text)) % 5)
         WHEN 0 THEN TIME '08:15'
         WHEN 1 THEN TIME '08:35'
         WHEN 2 THEN TIME '07:55'
         WHEN 3 THEN TIME '09:10'
         ELSE        TIME '08:05'
       END,
       21.0285, 105.8542, 'FTECH-Office-5G',
       CASE (abs(hashtext('EMP003' || d::text)) % 4)
         WHEN 0 THEN TIME '17:30'
         WHEN 1 THEN TIME '18:45'
         WHEN 2 THEN TIME '20:30'   -- OT
         ELSE        TIME '17:50'
       END,
       21.0285, 105.8542, 'FTECH-Office-5G',
       480 + (abs(hashtext('EMP003' || d::text)) % 180),
       GREATEST(0, (abs(hashtext('EMP003' || d::text)) % 40) - 10),
       CASE WHEN (abs(hashtext('EMP003' || d::text)) % 4) = 2 THEN 180 ELSE 0 END,
       'FULL',
       CASE
         WHEN EXTRACT(DOW FROM d) IN (0,6) THEN 'HOLIDAY'
         WHEN (abs(hashtext('EMP003' || d::text)) % 15) = 0 THEN 'HALF_DAY'
         WHEN (abs(hashtext('EMP003' || d::text)) % 40) > 25 THEN 'LATE'
         ELSE 'PRESENT'
       END,
       CASE
         WHEN (abs(hashtext('EMP003' || d::text)) % 40) > 25 THEN 'Kẹt xe cầu Nhật Tân, xin lỗi anh chị.'
         WHEN (abs(hashtext('EMP003' || d::text)) % 4)  = 2  THEN 'OT release feature v1.2.'
         ELSE NULL
       END,
       false, NOW(), 'system', NOW(), 'system'
FROM generate_series(CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE - INTERVAL '1 day', INTERVAL '1 day') AS d
WHERE EXISTS (SELECT 1 FROM person WHERE code = 'EMP003')
  AND EXISTS (SELECT 1 FROM contract WHERE code = 'HD_2026_003')
  AND NOT EXISTS (
      SELECT 1 FROM attendance a
      WHERE a.person_id = (SELECT id FROM person WHERE code = 'EMP003' LIMIT 1)
        AND a.attendance_date = d::date
  );

-- ============================================================
-- SUMMARY — quick check sau khi seed
-- ============================================================
SELECT 'payroll_period'  AS tbl, COUNT(*) AS cnt FROM payroll_period WHERE is_deleted = false
UNION ALL SELECT 'payroll',      COUNT(*) FROM payroll         WHERE is_deleted = false
UNION ALL SELECT 'payroll_detail', COUNT(*) FROM payroll_detail WHERE is_deleted = false
UNION ALL SELECT 'leave_request', COUNT(*) FROM leave_request  WHERE is_deleted = false
UNION ALL SELECT 'attendance',   COUNT(*) FROM attendance     WHERE is_deleted = false;
