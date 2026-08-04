-- ============================================================
-- SCRIPT: Menu ↔ Permission mapping
-- Description: Mỗi leaf menu gắn permission cùng resource (legacy entity-level
--              + per-endpoint api_path sau 2026-08-04).
--              Join = PREFIX trên LTRIM(api_path,'/') — giống role_permission_data.
--              Dùng cho catalog admin (menu.permissionIds) + tài liệu “full
--              access menu X = các permission này”. KHÔNG tự filter sidebar
--              (sidebar = role_menu) và KHÔNG tự gate API (API = role_permission).
-- Created: 2026-07-16 (Batch I4)
-- Updated: 2026-08-04 — prefix match + CRM/WH/ACC/QLNS hubs
-- IDEMPOTENT: NOT EXISTS trên (menu_id, permission_id)
-- ============================================================

-- api_prefix: không leading slash; khớp exact HOẶC prefix + '/'
WITH menu_api_map(menu_code, api_prefix) AS (
    VALUES
        -- ========================= QTHT =========================
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
        ('QLHT_USAGE',          'qtht/usage'),
        ('QLHT_GUIDE',          'qtht/guides'),
        ('DASHBOARD',           'qtht/dashboard'),
        ('SYS_ASSET_DEP',       'asset/depreciation'),

        -- ========================= QLNS / HRM =========================
        ('QLHT_STAFF',          'qlns/person'),
        ('QLHT_CONTRACT',       'qlns/contract'),
        ('QLHT_PAYROLL',        'qlns/payroll'),
        ('QLHT_SALARY_BAND',    'qlns/salary-bands'),
        ('QLHT_LEAVE',          'qlns/leave'),
        ('QLHT_LEAVE',          'qlns/leave-request'),
        ('QLHT_ATTENDANCE',     'qlns/attendance'),
        ('QLNS_OKR',            'qlns/okrs'),
        ('QLNS_PERF',           'qlns/performance-reviews'),
        ('QLNS_RECOGNITION',    'qlns/recognition'),
        ('QLNS_ONBOARD',        'qlns/onboarding'),
        ('QLNS_RECRUIT',        'qlns/recruitment'),
        ('QLNS_RECRUIT_BOARD',  'qlns/recruitment'),

        -- ========================= CRM =========================
        -- Legacy entity path + per-endpoint /customer
        ('QLHT_CUSTOMER',       'customer/customer'),
        ('QLHT_CUSTOMER',       'customer'),
        ('CRM_LEADS',           'crm/leads'),
        ('CRM_DEALS',           'crm/deals'),
        ('CRM_MEETINGS',        'crm/meetings'),
        ('CRM_EMAIL_SEQ',       'crm/email-sequences'),
        ('CRM_QUOTES',          'crm/quotes'),
        ('CRM_INVOICES',        'crm/invoices'),

        -- ========================= Product =========================
        ('QLHT_PRODUCT',        'product/product'),
        ('QLHT_PRODUCT',        'product'),
        ('QLHT_PRODUCT_CATE',   'product/product-category'),
        ('QLHT_PRODUCT_CATE',   'product/category'),

        -- ========================= Warehouse =========================
        ('WH_REORDER',          'warehouse/reorder-rules'),
        ('WH_ALERTS',           'warehouse/stock-alerts'),
        ('WH_STOCKTAKE',        'warehouse/stock-takes'),
        ('WH_PR',               'warehouse/purchase-requests'),
        ('WH_PO',               'warehouse/purchase-orders'),
        ('WH_GRN',              'warehouse/grn'),
        ('WH_GIN',              'warehouse/gin'),
        ('WH_BATCHES',          'warehouse/batches'),
        ('WH_SHRINKAGE',        'warehouse/shrinkage'),

        -- ========================= Accounting =========================
        ('ACC_ACCOUNTS',        'accounting/accounts'),
        ('ACC_JOURNALS',        'accounting/journals'),
        ('ACC_LEDGER',          'accounting/gl/ledger'),
        ('ACC_LEDGER',          'accounting/ledger'),
        ('ACC_TB',              'accounting/gl/trial-balance'),
        ('ACC_TB',              'accounting/trial-balance'),
        ('ACC_FS',              'accounting/reports'),
        ('ACC_FS',              'accounting/financial-statements'),
        ('ACC_BANK',            'accounting/bank-statements'),
        ('ACC_BANK',            'accounting/bank-reconciliation'),
        ('ACC_PERIODS',         'accounting/periods'),
        ('ACC_SETTINGS',        'accounting/setting'),
        ('ACC_SETTINGS',        'accounting/settings'),

        -- ========================= Approval =========================
        ('APPR_INBOX',          'approvals'),
        ('APPR_FLOWS',          'approval-flows'),

        -- ========================= Tasks =========================
        ('QLHT_CV',             'task/task'),
        ('QLHT_TICKET',         'task/ticket'),
        ('QLHT_TAG',            'task/tag'),
        ('QLHT_TICKET_CAT',     'task/ticket-category'),

        -- ========================= Email =========================
        ('QLHT_EMAIL_TEMPLATE', 'email/template'),
        ('QLHT_EMAIL_GROUP',    'email/group'),
        ('QLHT_EMAIL_CONFIG',   'email/config'),
        ('QLHT_EMAIL_INBOX',    'email/inbox'),
        ('QLHT_EMAIL_COMPOSE',  'email/send'),

        -- ========================= Facebook / Growth =========================
        ('QLHT_FB_ACCOUNT',     'fb/account'),
        ('QLHT_FB_GROUP',       'fb/group'),
        ('QLHT_FB_LEAD',        'fb/lead'),
        ('QLHT_FB_SCAN',        'fb/automation'),

        -- ========================= MKT Suite =========================
        ('QLHT_MKT_INBOX',      'fb/leads'),
        ('QLHT_MKT_LEAD_IMPORT','mkt/leads/import'),
        ('QLHT_MKT_CONTENT',    'mkt/posts'),
        ('QLHT_MKT_AFFILIATE',  'mkt/affiliate'),
        ('QLHT_MKT_INSIGHTS',   'mkt/insights'),
        ('QLHT_MKT_ADS',        'mkt/ads'),
        ('QLHT_MKT_COMMENTS',   'mkt/comments'),
        ('QLHT_MKT_REVIEWS',    'mkt/reviews'),
        ('QLHT_MKT_LIVE',       'mkt/live'),

        -- ========================= QTBV / CMS =========================
        ('QLHT_ARTICLE',        'qtbv/articles'),
        ('QLHT_ARTICLE',        'qtbv/managers'),
        ('QLHT_ARTICLE',        'qtbv/organizations'),
        ('QLHT_EVENT',          'qtbv/event'),
        ('QLHT_EVENT',          'events'),
        ('QLHT_WEBSITE',        'qtbv/landing-config'),
        ('QLHT_WEBSITE',        'qtbv/banner'),

        -- ========================= Contracts / Assets / DMDC =========================
        ('QLHT_BGHD',           'qlns/contract'),
        ('QLHT_ASSET',          'qlts/asset'),
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
JOIN menu m
  ON m.code = map.menu_code
 AND m.app_code = 'QTHT'
 AND (m.is_deleted = false OR m.is_deleted IS NULL)
JOIN permission p
  ON p.app_code = 'QTHT'
 AND (p.is_deleted = false OR p.is_deleted IS NULL)
 AND (
        LTRIM(p.api_path, '/') = map.api_prefix
     OR LTRIM(p.api_path, '/') LIKE map.api_prefix || '/%'
 )
WHERE NOT EXISTS (
      SELECT 1 FROM menu_permission mp
      WHERE mp.menu_id = m.id AND mp.permission_id = p.id
  );
