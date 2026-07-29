-- ============================================================
-- SCRIPT: menu_tree_v3 — Domain parents (MODULE_CONSOLIDATION_PLAN)
-- RUNTIME SSOT (LNK-07 / MENU-01). DataInitializer loads THIS file only.
-- menu_tree_v2.sql / menu_tree_restructure.sql = DEPRECATED — do not load.
-- Date: 2026-07-21
-- Replaces mega-IA v2 (MENU_SALE wrapping Acc/WH).
--
-- Root parents (folder, fe_url NULL):
--   MENU_HRM, MENU_CRM, MENU_PRODUCT, MENU_WAREHOUSE,
--   MENU_ACCOUNTING, MENU_APPROVAL, MENU_TASK, MENU_GROWTH, MENU_QTHT
-- Root leaves: HOME, DASHBOARD, PROFILE
--
-- Stable: leaf code / fe_url / permission map unchanged.
-- Idempotent: INSERT WHERE NOT EXISTS + UPDATE parent_code / order.
-- Icons: Lucide PascalCase (FE ICON_MAP lowercases keys).
-- ============================================================

-- ------------------------------------------------------------
-- 1) DOMAIN PARENT GROUPS (root folders)
-- ------------------------------------------------------------
INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, 'QTHT', NULL, v.folder_path, NULL, v.order_index, 1, v.icon, true, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('MENU_HRM',        'Nhân sự',              'HR / QLNS',           'src/modules/qlns',       10, 'Users'),
    ('MENU_CRM',        'CRM & Khách hàng',     'CRM & Customers',     'src/modules/crm',        20, 'UserCheck'),
    ('MENU_PRODUCT',    'Sản phẩm',             'Products',            'src/modules/products',   30, 'Package'),
    ('MENU_WAREHOUSE',  'Kho',                  'Warehouse',           'src/modules/warehouse',  40, 'Warehouse'),
    ('MENU_ACCOUNTING', 'Kế toán',              'Accounting',          'src/modules/accounting', 50, 'DollarSign'),
    ('MENU_APPROVAL',   'Phê duyệt',            'Approval',            'src/modules/approval',   60, 'ClipboardList'),
    ('MENU_TASK',       'Công việc',            'Tasks',               'src/modules/tasks',      70, 'ListChecks'),
    ('MENU_GROWTH',     'Kênh & Marketing',     'Channels & Growth',   'src/modules/growth',     80, 'Bot'),
    ('MENU_QTHT',       'Quản trị hệ thống',    'System Admin',        'src/modules/qtht',       90, 'Settings')
) AS v(code, name, name_en, folder_path, order_index, icon)
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = v.code
);

-- Force parents active as root folders
UPDATE menu
SET fe_url = NULL,
    parent_code = NULL,
    is_deleted = false,
    status = true,
    updated_date = NOW(),
    updated_by = 'system'
WHERE app_code = 'QTHT'
  AND code IN (
      'MENU_HRM', 'MENU_CRM', 'MENU_PRODUCT', 'MENU_WAREHOUSE',
      'MENU_ACCOUNTING', 'MENU_APPROVAL', 'MENU_TASK', 'MENU_GROWTH', 'MENU_QTHT'
  );

UPDATE menu SET name = 'Nhân sự',           name_en = 'HR / QLNS',         order_index = 10, icon = 'Users',         updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_HRM';
UPDATE menu SET name = 'CRM & Khách hàng',  name_en = 'CRM & Customers',   order_index = 20, icon = 'UserCheck',     updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_CRM';
UPDATE menu SET name = 'Sản phẩm',          name_en = 'Products',          order_index = 30, icon = 'Package',       updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_PRODUCT';
UPDATE menu SET name = 'Kho',               name_en = 'Warehouse',         order_index = 40, icon = 'Warehouse',     updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_WAREHOUSE';
UPDATE menu SET name = 'Kế toán',           name_en = 'Accounting',        order_index = 50, icon = 'DollarSign',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_ACCOUNTING';
UPDATE menu SET name = 'Phê duyệt',         name_en = 'Approval',          order_index = 60, icon = 'ClipboardList', updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_APPROVAL';
UPDATE menu SET name = 'Công việc',         name_en = 'Tasks',             order_index = 70, icon = 'ListChecks',    updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_TASK';
UPDATE menu SET name = 'Kênh & Marketing',  name_en = 'Channels & Growth', order_index = 80, icon = 'Bot',           updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_GROWTH';
UPDATE menu SET name = 'Quản trị hệ thống', name_en = 'System Admin',      order_index = 90, icon = 'Settings',      updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_QTHT';

-- ------------------------------------------------------------
-- 2) NEW / MISSING LEAVES (idempotent — same codes as v2)
-- ------------------------------------------------------------
INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, 'QTHT', v.fe_url, v.folder_path, v.parent_code, v.order_index, 1, v.icon, true, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    -- HR
    ('QLNS_OKR',           'OKR',                  'OKRs',                 '/qlns/okrs',                       'src/modules/qlns',       'MENU_HRM',        7,  'Target'),
    ('QLNS_PERF',          'Đánh giá hiệu suất',   'Performance Reviews',  '/qlns/performance-reviews',        'src/modules/qlns',       'MENU_HRM',        8,  'BarChart3'),
    ('QLNS_ONBOARD',       'Onboarding',           'Onboarding',           '/qlns/onboarding',                 'src/modules/qlns',       'MENU_HRM',        9,  'UserPlus'),
    ('QLNS_RECRUIT',       'Yêu cầu tuyển dụng',   'Requisitions',         '/qlns/recruitment/requisitions',   'src/modules/qlns',       'MENU_HRM',        10, 'Briefcase'),
    ('QLNS_RECRUIT_BOARD', 'Kanban tuyển dụng',    'Recruitment Board',    '/qlns/recruitment/board',          'src/modules/qlns',       'MENU_HRM',        11, 'LayoutGrid'),
    -- Accounting
    ('ACC_ACCOUNTS',       'Hệ thống tài khoản',   'Chart of Accounts',    '/accounting/accounts',             'src/modules/accounting', 'MENU_ACCOUNTING', 1,  'Book'),
    ('ACC_JOURNALS',       'Chứng từ kế toán',     'Journals',             '/accounting/journals',             'src/modules/accounting', 'MENU_ACCOUNTING', 2,  'FileText'),
    ('ACC_LEDGER',         'Sổ cái',               'General Ledger',       '/accounting/ledger',               'src/modules/accounting', 'MENU_ACCOUNTING', 3,  'BookOpen'),
    ('ACC_TB',             'Cân đối phát sinh',    'Trial Balance',        '/accounting/trial-balance',        'src/modules/accounting', 'MENU_ACCOUNTING', 4,  'BarChart2'),
    ('ACC_FS',             'Báo cáo tài chính',    'Financial Statements', '/accounting/financial-statements', 'src/modules/accounting', 'MENU_ACCOUNTING', 5,  'PieChart'),
    ('ACC_BANK',           'Đối soát ngân hàng',   'Bank Reconciliation',  '/accounting/bank-reconciliation',  'src/modules/accounting', 'MENU_ACCOUNTING', 6,  'Landmark'),
    ('ACC_PERIODS',        'Kỳ kế toán',           'Fiscal Periods',       '/accounting/periods',              'src/modules/accounting', 'MENU_ACCOUNTING', 7,  'CalendarRange'),
    ('ACC_SETTINGS',       'Cài đặt kế toán',      'Accounting Settings',  '/accounting/settings',             'src/modules/accounting', 'MENU_ACCOUNTING', 8,  'Settings'),
    -- Warehouse
    ('WH_REORDER',         'Quy tắc tồn kho',      'Reorder Rules',        '/warehouse/reorder-rules',         'src/modules/warehouse',  'MENU_WAREHOUSE',  1,  'RefreshCw'),
    ('WH_ALERTS',          'Cảnh báo tồn kho',     'Stock Alerts',         '/warehouse/stock-alerts',          'src/modules/warehouse',  'MENU_WAREHOUSE',  2,  'Bell'),
    ('WH_STOCKTAKE',       'Kiểm kê',              'Stock Take',           '/warehouse/stock-takes',           'src/modules/warehouse',  'MENU_WAREHOUSE',  3,  'ClipboardCheck'),
    ('WH_PR',              'Yêu cầu mua hàng',     'Purchase Requests',    '/warehouse/purchase-requests',     'src/modules/warehouse',  'MENU_WAREHOUSE',  4,  'ShoppingCart'),
    ('WH_PO',              'Đơn mua hàng',         'Purchase Orders',      '/warehouse/purchase-orders',       'src/modules/warehouse',  'MENU_WAREHOUSE',  5,  'FileSpreadsheet'),
    ('WH_GRN',             'Phiếu nhập kho',       'Goods Receipt Notes',  '/warehouse/grn',                  'src/modules/warehouse',  'MENU_WAREHOUSE',  6,  'PackagePlus'),
    ('WH_GIN',             'Phiếu xuất kho',       'Goods Issue Notes',    '/warehouse/gin',                  'src/modules/warehouse',  'MENU_WAREHOUSE',  7,  'PackageMinus'),
    -- CRM
    ('CRM_LEADS',          'Leads',                'Leads',                '/crm/leads',                       'src/modules/crm',        'MENU_CRM',        2,  'UserPlus'),
    ('CRM_DEALS',          'Cơ hội bán',           'Deals',                '/crm/deals',                       'src/modules/crm',        'MENU_CRM',        3,  'Briefcase'),
    ('CRM_MEETINGS',       'Cuộc họp',             'Meetings',             '/crm/meetings',                    'src/modules/crm',        'MENU_CRM',        4,  'CalendarCheck'),
    ('CRM_EMAIL_SEQ',      'Email sequence',       'Email Sequences',      '/crm/email-sequences',             'src/modules/crm',        'MENU_CRM',        5,  'Mail'),
    ('CRM_QUOTES',         'Báo giá',              'Quotes',               '/crm/quotes',                      'src/modules/crm',        'MENU_CRM',        6,  'FileCheck'),
    ('CRM_INVOICES',       'Hoá đơn',              'Invoices',             '/crm/invoices',                    'src/modules/crm',        'MENU_CRM',        7,  'Receipt'),
    -- Approval (ops inbox)
    ('APPR_INBOX',         'Hộp thư duyệt',        'Approval Inbox',       '/approval/inbox',                  'src/modules/approval',   'MENU_APPROVAL',   1,  'Inbox'),
    -- QTHT config
    ('APPR_FLOWS',         'Cấu hình luồng duyệt', 'Approval Flows',       '/approval/flows',                  'src/modules/approval',   'MENU_QTHT',       11, 'GitBranch'),
    ('SYS_ASSET_DEP',      'Khấu hao TSCĐ',        'Depreciation',         '/assets/depreciation',             'src/modules/assets',     'MENU_QTHT',       9,  'TrendingDown'),
    -- Docs
    ('MENU_DOCS',          'Tài liệu',             'Docs',                 '/docs',                            'src/docs',               'MENU_TASK',       5,  'BookOpen'),
    -- Ticket category master (FR-TASK-CAT)
    ('QLHT_TICKET_CAT',    'Danh mục Ticket',      'Ticket Categories',    '/task/categories',                 'src/modules/tasks',      'MENU_TASK',       4,  'FolderTree'),
    -- Guide CMS (FR-DOC-03/04)
    ('QLHT_GUIDE',         'Quản lý hướng dẫn',    'Guide CMS',            '/admin/guides',                    'src/modules/docs',       'MENU_QTHT',       18, 'BookOpen')
) AS v(code, name, name_en, fe_url, folder_path, parent_code, order_index, icon)
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = v.code
);

-- ------------------------------------------------------------
-- 3) Nest leaves under domain parents + sync fe_url
-- ------------------------------------------------------------

-- === HR / QLNS (C2) ===
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 1,  fe_url = '/qlns/persons',                 is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_STAFF';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 2,  fe_url = '/qlns/contract',                 is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_CONTRACT';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 3,  fe_url = '/qlns/payrolls',                 is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_PAYROLL';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 4,  fe_url = '/qlns/salary-bands',             is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_SALARY_BAND';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 5,  fe_url = '/qlns/leaves',                   is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_LEAVE';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 6,  fe_url = '/admin/attendance',              is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ATTENDANCE';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 7,  fe_url = '/qlns/okrs',                     is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_OKR';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 8,  fe_url = '/qlns/performance-reviews',      is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_PERF';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 9,  fe_url = '/qlns/onboarding',               is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_ONBOARD';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 10, fe_url = '/qlns/recruitment/requisitions', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_RECRUIT';
UPDATE menu SET parent_code = 'MENU_HRM', order_index = 11, fe_url = '/qlns/recruitment/board',        is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLNS_RECRUIT_BOARD';

-- === CRM & Customer (C5) ===
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 1, fe_url = '/customer',            is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_CUSTOMER';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 2, fe_url = '/crm/leads',           is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_LEADS';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 3, fe_url = '/crm/deals', name = 'Cơ hội bán', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_DEALS';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 4, fe_url = '/crm/meetings',        is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_MEETINGS';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 5, fe_url = '/crm/email-sequences', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_EMAIL_SEQ';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 6, fe_url = '/crm/quotes',          is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_QUOTES';
UPDATE menu SET parent_code = 'MENU_CRM', order_index = 7, fe_url = '/crm/invoices',        is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'CRM_INVOICES';

-- === Product (C4 catalog) ===
UPDATE menu SET parent_code = 'MENU_PRODUCT', order_index = 1, fe_url = '/product',       is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_PRODUCT';
UPDATE menu SET parent_code = 'MENU_PRODUCT', order_index = 2, fe_url = '/loai-san-pham', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_PRODUCT_CATE';
-- QLHT_BGHD: soft-delete (PlaceholderPage dead-end) — LNK-08 / Plan R23

-- === Warehouse (C4 inventory) ===
UPDATE menu SET parent_code = 'MENU_WAREHOUSE', order_index = 1, fe_url = '/warehouse/reorder-rules',     is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_REORDER';
UPDATE menu SET parent_code = 'MENU_WAREHOUSE', order_index = 2, fe_url = '/warehouse/stock-alerts',      is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_ALERTS';
UPDATE menu SET parent_code = 'MENU_WAREHOUSE', order_index = 3, fe_url = '/warehouse/stock-takes',       is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_STOCKTAKE';
UPDATE menu SET parent_code = 'MENU_WAREHOUSE', order_index = 4, fe_url = '/warehouse/purchase-requests', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_PR';
UPDATE menu SET parent_code = 'MENU_WAREHOUSE', order_index = 5, fe_url = '/warehouse/purchase-orders',   is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_PO';
UPDATE menu SET parent_code = 'MENU_WAREHOUSE', order_index = 6, fe_url = '/warehouse/grn',               is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_GRN';
UPDATE menu SET parent_code = 'MENU_WAREHOUSE', order_index = 7, fe_url = '/warehouse/gin',               is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'WH_GIN';

-- === Accounting (C3) ===
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 1, fe_url = '/accounting/accounts',             is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_ACCOUNTS';
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 2, fe_url = '/accounting/journals',             is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_JOURNALS';
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 3, fe_url = '/accounting/ledger',               is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_LEDGER';
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 4, fe_url = '/accounting/trial-balance',        is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_TB';
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 5, fe_url = '/accounting/financial-statements', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_FS';
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 6, fe_url = '/accounting/bank-reconciliation',  is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_BANK';
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 7, fe_url = '/accounting/periods',              is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_PERIODS';
UPDATE menu SET parent_code = 'MENU_ACCOUNTING', order_index = 8, fe_url = '/accounting/settings',             is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'ACC_SETTINGS';

-- === Approval ops (C1) ===
UPDATE menu SET parent_code = 'MENU_APPROVAL', order_index = 1, fe_url = '/approval/inbox', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'APPR_INBOX';

-- === Tasks / Ops (C8) ===
UPDATE menu SET parent_code = 'MENU_TASK', order_index = 1, fe_url = '/task',            is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_CV';
UPDATE menu SET parent_code = 'MENU_TASK', order_index = 2, fe_url = '/task/tickets',    is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_TICKET';
UPDATE menu SET parent_code = 'MENU_TASK', order_index = 3, fe_url = '/task/tags',       is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_TAG';
UPDATE menu SET parent_code = 'MENU_TASK', order_index = 4, name = 'Danh mục Ticket', name_en = 'Ticket Categories', fe_url = '/task/categories', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_TICKET_CAT';
UPDATE menu SET parent_code = 'MENU_TASK', order_index = 5, fe_url = '/docs',            is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'MENU_DOCS';

-- === Growth / Channels (C7) ===
UPDATE menu SET parent_code = 'MENU_GROWTH', order_index = 1,  fe_url = '/admin/events', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EVENT';
UPDATE menu SET parent_code = 'MENU_GROWTH', order_index = 10, fe_url = NULL,           is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL';
UPDATE menu SET parent_code = 'MENU_GROWTH', order_index = 20, fe_url = NULL,           is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB';
UPDATE menu SET parent_code = 'MENU_GROWTH', order_index = 30, fe_url = NULL,           is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT';

UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 1, fe_url = '/email/inbox',    is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_INBOX';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 2, fe_url = '/email/compose',  is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_COMPOSE';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 3, fe_url = '/email/template', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_TEMPLATE';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 4, fe_url = '/email/group',    is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_GROUP';
UPDATE menu SET parent_code = 'QLHT_EMAIL', order_index = 5, fe_url = '/email/config',   is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_EMAIL_CONFIG';

UPDATE menu SET parent_code = 'QLHT_FB', order_index = 1, fe_url = '/fb/accounts',    is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_ACCOUNT';
UPDATE menu SET parent_code = 'QLHT_FB', order_index = 2, fe_url = '/fb/scan-groups', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_SCAN';
UPDATE menu SET parent_code = 'QLHT_FB', order_index = 3, fe_url = '/fb/groups',      is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_GROUP';
UPDATE menu SET parent_code = 'QLHT_FB', order_index = 4, fe_url = '/fb/leads',       is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FB_LEAD';

UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 1,  fe_url = '/mkt/inbox',       is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_INBOX';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 2,  fe_url = '/mkt/leads/import', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_LEAD_IMPORT';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 3,  fe_url = '/mkt/content',      is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_CONTENT';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 4,  fe_url = '/mkt/affiliate',    is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_AFFILIATE';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 5,  fe_url = '/mkt/insights',     is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_INSIGHTS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 6,  fe_url = '/mkt/ads',          is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_ADS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 7,  fe_url = '/mkt/comments',     is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_COMMENTS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 8,  fe_url = '/mkt/reviews',      is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_REVIEWS';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 9,  fe_url = '/mkt/live',         is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_LIVE';
UPDATE menu SET parent_code = 'QLHT_MKT', order_index = 10, fe_url = '/mkt/zalo',         is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_MKT_ZALO';

-- === QTHT / Platform admin (C1 + C6 assets + content admin) ===
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 1,  fe_url = '/qtht/organizations',        is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ORG';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 2,  fe_url = '/qtht/departments',          is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_DEPARTMENT';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 3,  fe_url = '/qtht/users',                is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ACCOUNT';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 4,  fe_url = '/qtht/roles',                is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ROLE';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 5,  fe_url = '/qtht/menus',                is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_FEATURE';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 6,  fe_url = '/qtht/permissions',          is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_PERMISSION';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 7,  fe_url = '/admin/category-management', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_CATEGORY';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 8,  fe_url = '/admin/qlts',                is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ASSET';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 9,  fe_url = '/assets/depreciation',       is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'SYS_ASSET_DEP';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 10, fe_url = '/qtht/workflows',            is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_WORKFLOW';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 11, fe_url = '/approval/flows',            is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'APPR_FLOWS';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 12, fe_url = '/qtht/security',             is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_SECURITY';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 13, fe_url = '/qtht/apilogs',              is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_APILOG';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 14, fe_url = '/qtht/website',              is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_WEBSITE';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 15, fe_url = '/qtht/tin-tuc',              is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_NEWS';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 16, fe_url = '/qtht/settings',             is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_SETTING';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 17, fe_url = '/admin/article-management',  is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_ARTICLE';
UPDATE menu SET parent_code = 'MENU_QTHT', order_index = 18, fe_url = '/admin/guides',               is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system' WHERE app_code = 'QTHT' AND code = 'QLHT_GUIDE';

-- ------------------------------------------------------------
-- 4) Soft-delete deprecated mega-parents / legacy folders
-- ------------------------------------------------------------
UPDATE menu
SET status = false, is_deleted = true, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT'
  AND code IN (
      -- v2 mega-IA
      'MENU_SALE', 'SALE_GRP_ACC', 'SALE_GRP_WH', 'MENU_TOOL',
      -- older aliases / unused parents
      'MENU_QLNS', 'MENU_EVENT', 'MENU_ACC', 'MENU_WH', 'MENU_SYS',
      -- attendance settings removed from FE
      'QLHT_ATTEND_SETTING', 'QLHT_ATTENDANCE_SETTING', 'ATTENDANCE_SETTINGS',
      -- category sub-leaves (parent QLHT_CATEGORY is enough)
      'QLDM_ISSUER', 'QLDM_SIGNER', 'QLDM_TITLE', 'QLDM_LOCATION', 'QLDM_INDUSTRY',
      -- SALE BGHD placeholder (Plan R23 / LNK-08)
      'QLHT_BGHD'
  )
  AND (is_deleted IS DISTINCT FROM true OR status IS DISTINCT FROM false);

-- ------------------------------------------------------------
-- 5) Root leaves — HOME (portal, mọi user) vs DASHBOARD (KPI, Admin/level cao)
-- ------------------------------------------------------------
INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), 'HOME', 'Trang chủ', 'Home', 'QTHT', '/', 'src/modules/dashboard', NULL, 0, 1, 'Home', true, true, false, NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = 'HOME'
);

UPDATE menu SET parent_code = NULL, order_index = 0,  fe_url = '/',           name = 'Trang chủ', name_en = 'Home', icon = 'Home',
    is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'HOME';

UPDATE menu SET parent_code = NULL, order_index = 1,  fe_url = '/dashboard', name = 'Tổng quan', name_en = 'Dashboard', icon = 'dashboard',
    is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'DASHBOARD';

UPDATE menu SET parent_code = NULL, order_index = 97, fe_url = '/profile', is_deleted = false, status = true, updated_date = NOW(), updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'PROFILE';
