-- ============================================================
-- SCRIPT: Seed Contract Templates (loại HĐ mẫu cho FE)
-- Idempotent theo (contract_type) / template_name
-- Contracts mẫu: xem demo_data.sql §15 + ContractDataInitializer
-- ============================================================

INSERT INTO contract_templates (id, template_name, contract_type, file_url, file_object_name,
                                is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.name, v.type, v.html, v.obj,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('Mẫu HĐ thử việc',
     'THU_VIEC',
     '<h2 style="text-align:center"><strong>HỢP ĐỒNG THỬ VIỆC</strong></h2><p>Bên A: {{employerName}} — Bên B: {{personName}}</p><p>Vị trí: {{jobPosition}}</p>',
     'seed/thu_viec.html'),
    ('Mẫu HĐ chính thức (không xác định TH)',
     'CHINH_THUC',
     '<h2 style="text-align:center"><strong>HỢP ĐỒNG LAO ĐỘNG</strong></h2><p>(Không xác định thời hạn)</p><p>Bên A: {{employerName}} — Bên B: {{personName}}</p>',
     'seed/chinh_thuc.html'),
    ('Mẫu HĐ thời vụ / xác định TH',
     'THOI_VU',
     '<h2 style="text-align:center"><strong>HỢP ĐỒNG LAO ĐỘNG XÁC ĐỊNH THỜI HẠN</strong></h2><p>Từ {{startDate}} đến {{endDate}}</p><p>Bên B: {{personName}} · {{jobPosition}}</p>',
     'seed/thoi_vu.html')
) AS v(name, type, html, obj)
WHERE NOT EXISTS (
    SELECT 1 FROM contract_templates t
    WHERE t.is_deleted = false AND (t.contract_type = v.type OR t.template_name = v.name)
);
