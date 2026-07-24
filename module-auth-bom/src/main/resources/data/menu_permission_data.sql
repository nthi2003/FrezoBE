-- ============================================================
-- SCRIPT: Menu ↔ Permission mapping
-- Description: Mỗi menu link với TẤT CẢ permission có api_path prefix trùng
--              với module của menu (VD menu QLHT_DEPARTMENT link với tất cả
--              permission có api_path = 'qtht/department').
--              Giúp FE hiện đúng nút thao tác (VIEW/CREATE/UPDATE/DELETE) tuỳ role.
-- Created: 2026-07-16 (Batch I4)
-- IDEMPOTENT: NOT EXISTS trên (menu_id, permission_id)
-- ============================================================

WITH menu_api_map(menu_code, api_path) AS (
    VALUES
        -- QTHT
        ('QLHT_ACCOUNT',        'qtht/user'),
        ('QLHT_ROLE',           'qtht/role'),
        ('QLHT_PERMISSION',     'qtht/permission'),
        ('QLHT_FEATURE',        'qtht/menu'),
        ('QLHT_DEPARTMENT',     'qtht/department'),
        ('QLHT_ORG',            'qtht/organization'),
        ('QLHT_CATEGORY',       'qtht/category'),
        ('QLHT_SETTING',        'qtht/setting'),
        ('QLHT_SECURITY',       'qtht/ip-blacklist'),
        ('QLHT_SECURITY',       'qtht/ip-whitelist'),
        ('QLHT_SECURITY',       'qtht/ip-trust'),
        ('QLHT_APILOG',         'qtht/api-log'),
        ('DASHBOARD',           'qtht/dashboard'),
        -- QLNS
        ('QLHT_STAFF',          'qlns/person'),
        ('QLHT_CONTRACT',       'qlns/contract'),
        ('QLHT_PAYROLL',        'qlns/payroll'),
        ('QLHT_LEAVE',          'qlns/leave'),
        ('QLHT_LEAVE',          'qlns/leave-request'),
        ('QLHT_ATTENDANCE',     'qlns/attendance'),
        -- Customer / Product
        ('QLHT_CUSTOMER',       'customer/customer'),
        ('QLHT_PRODUCT',        'product/product'),
        ('QLHT_PRODUCT_CATE',   'product/product-category'),
        -- Tasks
        ('QLHT_CV',             'task/task'),
        ('QLHT_TICKET',         'task/ticket'),
        ('QLHT_TAG',            'task/tag'),
        ('QLHT_TICKET_CAT',     'task/ticket-category'),
        -- Email
        ('QLHT_EMAIL_TEMPLATE', 'email/template'),
        ('QLHT_EMAIL_GROUP',    'email/group'),
        ('QLHT_EMAIL_CONFIG',   'email/config'),
        ('QLHT_EMAIL_INBOX',    'email/inbox'),
        ('QLHT_EMAIL_COMPOSE',  'email/send'),
        -- Facebook
        ('QLHT_FB_ACCOUNT',     'fb/account'),
        ('QLHT_FB_GROUP',       'fb/group'),
        ('QLHT_FB_LEAD',        'fb/lead'),
        ('QLHT_FB_SCAN',        'fb/automation'),
        -- QTBV / CMS (SA-ART-002: api_path khớp @CheckPermission — leading slash + plural)
        ('QLHT_ARTICLE',        '/qtbv/articles'),
        ('QLHT_ARTICLE',        '/qtbv/articles/filter'),
        ('QLHT_ARTICLE',        '/qtbv/articles/submit'),
        ('QLHT_ARTICLE',        '/qtbv/articles/publish'),
        ('QLHT_ARTICLE',        '/qtbv/articles/review'),
        ('QLHT_ARTICLE',        '/qtbv/articles/my-drafts'),
        ('QLHT_ARTICLE',        '/qtbv/articles/pending-approval'),
        ('QLHT_ARTICLE',        '/qtbv/articles/published'),
        ('QLHT_ARTICLE',        '/qtbv/managers'),
        ('QLHT_ARTICLE',        '/qtbv/organizations'),
        ('QLHT_EVENT',          'qtbv/event'),
        ('QLHT_WEBSITE',        'qtbv/landing-config'),
        ('QLHT_WEBSITE',        'qtbv/banner'),
        -- Contracts (BGHD)
        ('QLHT_BGHD',           'qlns/contract'),
        -- Assets
        ('QLHT_ASSET',          'qlts/asset'),
        ('SYS_ASSET_DEP',       '/asset/depreciation'),
        -- DMDC sub menus
        ('QLDM_ISSUER',         'dmdc/issuer'),
        ('QLDM_SIGNER',         'dmdc/signer'),
        ('QLDM_TITLE',          'dmdc/title'),
        ('QLDM_LOCATION',       'dmdc/location'),
        ('QLDM_INDUSTRY',       'dmdc/industry')
)
INSERT INTO menu_permission (id, menu_id, permission_id, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    m.id,
    p.id,
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM menu_api_map map
JOIN menu m       ON m.code = map.menu_code AND m.app_code = 'QTHT'
JOIN permission p ON p.api_path = map.api_path AND p.app_code = 'QTHT'
WHERE p.is_deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM menu_permission mp
      WHERE mp.menu_id = m.id AND mp.permission_id = p.id
  );
