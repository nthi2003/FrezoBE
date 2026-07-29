-- ============================================================
-- DEPRECATED — replaced by menu_tree_v3.sql (2026-07-21)
-- Reason: Acc/WH bị nhét dưới MENU_SALE; không khớp MODULE_CONSOLIDATION_PLAN.
-- DataInitializer loads menu_tree_v3.sql only. Keep for audit/history.
-- See: docs/BE_MENU_REGROUP_PLAN.md
-- ============================================================
-- SCRIPT: menu_tree_v2 — IA 5 module cha chính (legacy)
-- Parents (order 1–5): MENU_HRM, MENU_CRM, MENU_QTHT, MENU_TOOL, MENU_SALE
-- Soft-delete parents cũ: MENU_QLNS, MENU_ACCOUNTING, MENU_WAREHOUSE,
--   MENU_TASK, MENU_APPROVAL, MENU_EVENT, MENU_ACC, MENU_WH, MENU_SYS
-- Idempotent: INSERT WHERE NOT EXISTS + UPDATE parent_code / fe_url
-- Parent folders: fe_url IS NULL; icon = Lucide PascalCase
-- fe_url khớp FrezoFE packages/erp/src/app/router.tsx
-- Created: 2026-07-20 | Revised: 2026-07-20 (5-root IA)
-- ============================================================

-- ------------------------------------------------------------
-- 1) 5 PARENT GROUPS (root, parent_code = NULL, fe_url = NULL)
-- ------------------------------------------------------------
INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, 'QTHT', NULL, v.folder_path, NULL, v.order_index, 1, v.icon, true, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('MENU_HRM',  'Nhân sự',            'HRM',          'src/modules/qlns',       1, 'Users'),
    ('MENU_CRM',  'CRM',                'CRM',          'src/modules/crm',        2, 'UserCheck'),
    ('MENU_QTHT', 'Quản trị hệ thống',  'System Admin', 'src/modules/qtht',       3, 'Settings'),
    ('MENU_TOOL', 'Công cụ',            'Tools',        'src/modules/tools',      4, 'ListChecks'),
    ('MENU_SALE', 'Bán hàng',           'Sales',        'src/modules/sales',      5, 'ShoppingCart')
) AS v(code, name, name_en, folder_path, order_index, icon)
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = v.code
);

-- Đảm bảo 5 cha luôn là folder root active
UPDATE menu
SET fe_url = NULL,
    parent_code = NULL,
    is_deleted = false,
    status = true,
    updated_date = NOW(),
    updated_by = 'system'
WHERE app_code = 'QTHT'
  AND code IN ('MENU_HRM', 'MENU_CRM', 'MENU_QTHT', 'MENU_TOOL', 'MENU_SALE');

UPDATE menu SET name = 'Nhân sự',           name_en = 'HRM',          order_index = 1, icon = 'Users',        updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_HRM';
UPDATE menu SET name = 'CRM',               name_en = 'CRM',          order_index = 2, icon = 'UserCheck',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_CRM';
UPDATE menu SET name = 'Quản trị hệ thống', name_en = 'System Admin', order_index = 3, icon = 'Settings',     updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_QTHT';
UPDATE menu SET name = 'Công cụ',           name_en = 'Tools',        order_index = 4, icon = 'ListChecks',   updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_TOOL';
UPDATE menu SET name = 'Bán hàng',          name_en = 'Sales',        order_index = 5, icon = 'ShoppingCart', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_SALE';

-- ------------------------------------------------------------
-- 1b) SALE depth-3 subgroups (Kế toán / Kho) — optional folders
-- ------------------------------------------------------------
INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, 'QTHT', NULL, v.folder_path, v.parent_code, v.order_index, 1, v.icon, true, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('SALE_GRP_ACC', 'Kế toán', 'Accounting', 'src/modules/accounting', 'MENU_SALE', 10, 'DollarSign'),
    ('SALE_GRP_WH',  'Kho',     'Warehouse',  'src/modules/warehouse',  'MENU_SALE', 20, 'Warehouse')
) AS v(code, name, name_en, folder_path, parent_code, order_index, icon)
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = v.code
);

UPDATE menu
SET fe_url = NULL,
    parent_code = 'MENU_SALE',
    is_deleted = false,
    status = true,
    name = 'Kế toán',
    name_en = 'Accounting',
    order_index = 10,
    icon = 'DollarSign',
    updated_date = NOW(),
    updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'SALE_GRP_ACC';

UPDATE menu
SET fe_url = NULL,
    parent_code = 'MENU_SALE',
    is_deleted = false,
    status = true,
    name = 'Kho',
    name_en = 'Warehouse',
    order_index = 20,
    icon = 'Warehouse',
    updated_date = NOW(),
    updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'SALE_GRP_WH';

-- ------------------------------------------------------------
-- 2) NEW leaf menus (FE routes) — idempotent
-- ------------------------------------------------------------
INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, 'QTHT', v.fe_url, v.folder_path, v.parent_code, v.order_index, 1, v.icon, true, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    -- HRM extras
    ('QLNS_OKR',           'OKR',                 'OKRs',                 '/qlns/okrs',                       'src/modules/qlns',       'MENU_HRM',     7,  'Target'),
    ('QLNS_PERF',          'Đánh giá hiệu suất',  'Performance Reviews',  '/qlns/performance-reviews',        'src/modules/qlns',       'MENU_HRM',     8,  'BarChart3'),
    ('QLNS_ONBOARD',       'Onboarding',          'Onboarding',           '/qlns/onboarding',                 'src/modules/qlns',       'MENU_HRM',     9,  'UserPlus'),
    ('QLNS_RECRUIT',       'Yêu cầu tuyển dụng',  'Requisitions',         '/qlns/recruitment/requisitions',   'src/modules/qlns',       'MENU_HRM',     10, 'Briefcase'),
    ('QLNS_RECRUIT_BOARD', 'Kanban tuyển dụng',   'Recruitment Board',    '/qlns/recruitment/board',          'src/modules/qlns',       'MENU_HRM',     11, 'LayoutGrid'),
    -- SALE · Accounting
    ('ACC_ACCOUNTS',       'Hệ thống tài khoản',  'Chart of Accounts',    '/accounting/accounts',             'src/modules/accounting', 'SALE_GRP_ACC', 1,  'Book'),
    ('ACC_JOURNALS',       'Chứng từ kế toán',    'Journals',             '/accounting/journals',             'src/modules/accounting', 'SALE_GRP_ACC', 2,  'FileText'),
    ('ACC_LEDGER',         'Sổ cái',              'General Ledger',       '/accounting/ledger',               'src/modules/accounting', 'SALE_GRP_ACC', 3,  'BookOpen'),
    ('ACC_TB',             'Cân đối phát sinh',   'Trial Balance',        '/accounting/trial-balance',        'src/modules/accounting', 'SALE_GRP_ACC', 4,  'BarChart2'),
    ('ACC_FS',             'Báo cáo tài chính',   'Financial Statements', '/accounting/financial-statements', 'src/modules/accounting', 'SALE_GRP_ACC', 5,  'PieChart'),
    ('ACC_BANK',           'Đối soát ngân hàng',  'Bank Reconciliation',  '/accounting/bank-reconciliation',  'src/modules/accounting', 'SALE_GRP_ACC', 6,  'Landmark'),
    ('ACC_SETTINGS',       'Cài đặt kế toán',     'Accounting Settings',  '/accounting/settings',             'src/modules/accounting', 'SALE_GRP_ACC', 7,  'Settings'),
    -- SALE · Warehouse
    ('WH_REORDER',         'Quy tắc tồn kho',     'Reorder Rules',        '/warehouse/reorder-rules',         'src/modules/warehouse',  'SALE_GRP_WH',  1,  'RefreshCw'),
    ('WH_ALERTS',          'Cảnh báo tồn kho',    'Stock Alerts',         '/warehouse/stock-alerts',          'src/modules/warehouse',  'SALE_GRP_WH',  2,  'Bell'),
    ('WH_STOCKTAKE',       'Kiểm kê',             'Stock Take',           '/warehouse/stock-takes',           'src/modules/warehouse',  'SALE_GRP_WH',  3,  'ClipboardCheck'),
    ('WH_PR',              'Yêu cầu mua hàng',    'Purchase Requests',    '/warehouse/purchase-requests',     'src/modules/warehouse',  'SALE_GRP_WH',  4,  'ShoppingCart'),
    ('WH_PO',              'Đơn mua hàng',        'Purchase Orders',      '/warehouse/purchase-orders',       'src/modules/warehouse',  'SALE_GRP_WH',  5,  'FileSpreadsheet'),
    -- CRM
    ('CRM_LEADS',          'Leads',               'Leads',                '/crm/leads',                       'src/modules/crm',        'MENU_CRM',     2,  'UserPlus'),
    ('CRM_DEALS',          'Cơ hội bán',          'Deals',                '/crm/deals',                       'src/modules/crm',        'MENU_CRM',     3,  'Briefcase'),
    ('CRM_MEETINGS',       'Cuộc họp',            'Meetings',             '/crm/meetings',                    'src/modules/crm',        'MENU_CRM',     4,  'CalendarCheck'),
    ('CRM_EMAIL_SEQ',      'Email sequence',      'Email Sequences',      '/crm/email-sequences',             'src/modules/crm',        'MENU_CRM',     5,  'Mail'),
    -- SALE · quotes / invoices (bán hàng thuần)
    ('CRM_QUOTES',         'Báo giá',             'Quotes',               '/crm/quotes',                      'src/modules/crm',        'MENU_SALE',    3,  'FileCheck'),
    ('CRM_INVOICES',       'Hoá đơn',             'Invoices',             '/crm/invoices',                    'src/modules/crm',        'MENU_SALE',    4,  'Receipt'),
    -- TOOL · phê duyệt inbox
    ('APPR_INBOX',         'Hộp thư duyệt',       'Approval Inbox',       '/approval/inbox',                  'src/modules/approval',   'MENU_TOOL',    6,  'Inbox'),
    -- QTHT · cấu hình luồng duyệt + khấu hao
    ('APPR_FLOWS',         'Cấu hình luồng duyệt','Approval Flows',       '/approval/flows',                  'src/modules/approval',   'MENU_QTHT',    11, 'GitBranch'),
    ('SYS_ASSET_DEP',      'Khấu hao TSCĐ',       'Depreciation',         '/assets/depreciation',             'src/modules/assets',     'MENU_QTHT',    9,  'TrendingDown'),
    -- TOOL · docs
    ('MENU_DOCS',          'Tài liệu',            'Docs',                 '/docs',                            'src/docs',               'MENU_TOOL',    4,  'BookOpen')
) AS v(code, name, name_en, fe_url, folder_path, parent_code, order_index, icon)
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = v.code
);

-- ------------------------------------------------------------
-- 3) Nest existing menus under 5 parents + sync fe_url
-- ------------------------------------------------------------

-- === HRM (MENU_HRM) ===
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 1,  fe_url = '/qlns/persons',      updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_STAFF';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 2,  fe_url = '/qlns/contract',      updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_CONTRACT';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 3,  fe_url = '/qlns/payrolls',      updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_PAYROLL';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 4,  fe_url = '/qlns/salary-bands',  updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_SALARY_BAND';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 5,  fe_url = '/qlns/leaves',        updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_LEAVE';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 6,  fe_url = '/admin/attendance',  updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_ATTENDANCE';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 7,  fe_url = '/qlns/okrs',                     updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_OKR';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 8,  fe_url = '/qlns/performance-reviews',      updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_PERF';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 9,  fe_url = '/qlns/onboarding',               updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_ONBOARD';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 10, fe_url = '/qlns/recruitment/requisitions', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_RECRUIT';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 11, fe_url = '/qlns/recruitment/board',        updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_RECRUIT_BOARD';

-- === CRM (MENU_CRM) — pipeline / khách hàng (quotes+invoices → SALE) ===
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 1, fe_url = '/customer',            updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_CUSTOMER';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 2, fe_url = '/crm/leads',           updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_LEADS';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 3, fe_url = '/crm/deals', name = 'Cơ hội bán', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_DEALS';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 4, fe_url = '/crm/meetings',        updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_MEETINGS';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 5, fe_url = '/crm/email-sequences', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_EMAIL_SEQ';

-- === QTHT — admin / org / RBAC / assets / workflows config ===
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 1,  fe_url = '/qtht/organizations',        updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ORG';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 2,  fe_url = '/qtht/departments',          updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_DEPARTMENT';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 3,  fe_url = '/qtht/users',                updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ACCOUNT';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 4,  fe_url = '/qtht/roles',                updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ROLE';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 5,  fe_url = '/qtht/menus',                updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FEATURE';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 6,  fe_url = '/qtht/permissions',          updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_PERMISSION';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 7,  fe_url = '/admin/category-management', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_CATEGORY';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 8,  fe_url = '/admin/qlts',                updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ASSET';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 9,  fe_url = '/assets/depreciation',       updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'SYS_ASSET_DEP';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 10, fe_url = '/qtht/workflows',            updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_WORKFLOW';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 11, fe_url = '/approval/flows',            updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'APPR_FLOWS';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 12, fe_url = '/qtht/security',             updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_SECURITY';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 13, fe_url = '/qtht/apilogs',              updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_APILOG';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 14, fe_url = '/qtht/website',              updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_WEBSITE';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 15, fe_url = '/qtht/tin-tuc',              updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_NEWS';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 16, fe_url = '/qtht/settings',             updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_SETTING';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 17, fe_url = '/admin/article-management',  updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ARTICLE';

-- === TOOL — tasks / docs / events / approval inbox / email / fb / mkt ===
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 1, fe_url = '/task',         updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_CV';
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 2, fe_url = '/task/tickets', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_TICKET';
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 3, fe_url = '/task/tags',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_TAG';
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 4, fe_url = '/docs',         updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_DOCS';
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 5, fe_url = '/admin/events', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EVENT';
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 6, fe_url = '/approval/inbox', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'APPR_INBOX';

-- Email / FB / MKT: giữ folder depth-3 dưới TOOL (fe_url NULL)
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 10, fe_url = NULL, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL';
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 20, fe_url = NULL, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_FB';
UPDATE menu SET parent_code = 'MENU_TOOL', order_index = 30, fe_url = NULL, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_MKT';

-- Email children (giữ parent QLHT_EMAIL)
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 1, fe_url = '/email/inbox',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_INBOX';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 2, fe_url = '/email/compose',  updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_COMPOSE';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 3, fe_url = '/email/template', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_TEMPLATE';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 4, fe_url = '/email/group',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_GROUP';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 5, fe_url = '/email/config',   updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_CONFIG';

-- FB children
UPDATE menu SET parent_code = 'QLHT_FB', order_index = 1, fe_url = '/fb/accounts',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_ACCOUNT';
UPDATE menu SET parent_code = 'QLHT_FB', order_index = 2, fe_url = '/fb/scan-groups', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_SCAN';
UPDATE menu SET parent_code = 'QLHT_FB', order_index = 3, fe_url = '/fb/groups',      updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_GROUP';
UPDATE menu SET parent_code = 'QLHT_FB', order_index = 4, fe_url = '/fb/leads',       updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_LEAD';

-- MKT children
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 1,  fe_url = '/mkt/inbox',        updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_INBOX';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 2,  fe_url = '/mkt/leads/import',  updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_LEAD_IMPORT';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 3,  fe_url = '/mkt/content',       updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_CONTENT';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 4,  fe_url = '/mkt/affiliate',     updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_AFFILIATE';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 5,  fe_url = '/mkt/insights',      updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_INSIGHTS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 6,  fe_url = '/mkt/ads',           updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_ADS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 7,  fe_url = '/mkt/comments',      updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_COMMENTS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 8,  fe_url = '/mkt/reviews',       updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_REVIEWS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 9,  fe_url = '/mkt/live',          updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_LIVE';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 10, fe_url = '/mkt/zalo',          updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_ZALO';

-- === SALE — products / quotes / invoices / BGHD + subgroups Acc/WH ===
UPDATE menu SET parent_code = 'MENU_SALE', order_index = 1, fe_url = '/product',       updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_PRODUCT';
UPDATE menu SET parent_code = 'MENU_SALE', order_index = 2, fe_url = '/loai-san-pham', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_PRODUCT_CATE';
UPDATE menu SET parent_code = 'MENU_SALE', order_index = 3, fe_url = '/crm/quotes',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_QUOTES';
UPDATE menu SET parent_code = 'MENU_SALE', order_index = 4, fe_url = '/crm/invoices',  updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_INVOICES';
UPDATE menu SET parent_code = 'MENU_SALE', order_index = 5, fe_url = '/admin/qlbghd',  updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_BGHD';

UPDATE menu SET parent_code = 'SALE_GRP_ACC', order_index = 1, fe_url = '/accounting/accounts',             updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_ACCOUNTS';
UPDATE menu SET parent_code = 'SALE_GRP_ACC', order_index = 2, fe_url = '/accounting/journals',             updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_JOURNALS';
UPDATE menu SET parent_code = 'SALE_GRP_ACC', order_index = 3, fe_url = '/accounting/ledger',               updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_LEDGER';
UPDATE menu SET parent_code = 'SALE_GRP_ACC', order_index = 4, fe_url = '/accounting/trial-balance',        updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_TB';
UPDATE menu SET parent_code = 'SALE_GRP_ACC', order_index = 5, fe_url = '/accounting/financial-statements', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_FS';
UPDATE menu SET parent_code = 'SALE_GRP_ACC', order_index = 6, fe_url = '/accounting/bank-reconciliation',  updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_BANK';
UPDATE menu SET parent_code = 'SALE_GRP_ACC', order_index = 7, fe_url = '/accounting/settings',             updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_SETTINGS';

UPDATE menu SET parent_code = 'SALE_GRP_WH', order_index = 1, fe_url = '/warehouse/reorder-rules',     updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_REORDER';
UPDATE menu SET parent_code = 'SALE_GRP_WH', order_index = 2, fe_url = '/warehouse/stock-alerts',      updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_ALERTS';
UPDATE menu SET parent_code = 'SALE_GRP_WH', order_index = 3, fe_url = '/warehouse/stock-takes',       updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_STOCKTAKE';
UPDATE menu SET parent_code = 'SALE_GRP_WH', order_index = 4, fe_url = '/warehouse/purchase-requests', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_PR';
UPDATE menu SET parent_code = 'SALE_GRP_WH', order_index = 5, fe_url = '/warehouse/purchase-orders',   updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_PO';

-- ------------------------------------------------------------
-- 4) Soft-delete / deprecate old parents & legacy
-- ------------------------------------------------------------

-- Parents cũ (đã migrate children) → soft-delete
UPDATE menu
SET status = false, is_deleted = true, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT'
  AND code IN (
      'MENU_QLNS', 'MENU_ACCOUNTING', 'MENU_WAREHOUSE',
      'MENU_TASK', 'MENU_APPROVAL', 'MENU_EVENT',
      'MENU_ACC', 'MENU_WH', 'MENU_SYS'
  )
  AND (is_deleted IS DISTINCT FROM true OR status IS DISTINCT FROM false);

-- Cài đặt chấm công đã gỡ khỏi FE
UPDATE menu
SET status = false, is_deleted = true, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT'
  AND code IN ('QLHT_ATTEND_SETTING', 'QLHT_ATTENDANCE_SETTING', 'ATTENDANCE_SETTINGS')
  AND (is_deleted IS DISTINCT FROM true OR status IS DISTINCT FROM false);

-- Sub-danh mục dưới QLHT_CATEGORY → ẩn (QLHT_CATEGORY đủ dùng)
UPDATE menu
SET status = false, is_deleted = true, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT'
  AND code IN ('QLDM_ISSUER', 'QLDM_SIGNER', 'QLDM_TITLE', 'QLDM_LOCATION', 'QLDM_INDUSTRY')
  AND (is_deleted IS DISTINCT FROM true);

-- ------------------------------------------------------------
-- 5) Root leaves ngoài 5 module: Dashboard / Profile
-- ------------------------------------------------------------
UPDATE menu SET parent_code = NULL, order_index = 0,  fe_url = '/',        updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'DASHBOARD';

UPDATE menu SET parent_code = NULL, order_index = 97, fe_url = '/profile', updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'PROFILE';
