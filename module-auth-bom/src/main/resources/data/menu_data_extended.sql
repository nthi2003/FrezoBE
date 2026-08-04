-- ============================================================
-- SCRIPT: Menu bổ sung cho các module business
-- Description: Thêm menu cho Customer, Product, QLNS full, Email,
--              Facebook Automation, Security, API Log, ... để
--              FE sidebar hiển thị đầy đủ page hiện có trong router.
-- Created: 2026-07-16 (Batch I1)
-- IDEMPOTENT: dùng WHERE NOT EXISTS trên (app_code, code)
-- ============================================================

-- ============================================================
-- 1) ROOT MENUS BỔ SUNG
-- ============================================================

INSERT INTO menu (id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index, menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.code, v.name, v.name_en, 'QTHT', v.fe_url, v.folder_path, v.parent_code, v.order_index, 1, v.icon, true, true, false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    -- Sales & CRM
    ('QLHT_CUSTOMER',       'Quản Lý Khách Hàng',       'Customer Management',       '/customer',                    'src/modules/customers', NULL, 20, 'fa-solid fa-user-tie'),
    -- Products
    ('QLHT_PRODUCT',        'Quản Lý Sản Phẩm',         'Product Management',        '/product',                     'src/modules/products',  NULL, 21, 'fa-solid fa-box-open'),
    ('QLHT_PRODUCT_CATE',   'Loại Sản Phẩm',            'Product Category',          '/loai-san-pham',               'src/modules/products',  NULL, 22, 'fa-solid fa-tags'),
    -- QLNS full
    ('QLHT_CONTRACT',       'Hợp Đồng Lao Động',        'Employment Contract',       '/qlns/contract',               'src/modules/contracts', NULL, 30, 'fa-solid fa-file-signature'),
    ('QLHT_PAYROLL',        'Bảng Lương',               'Payroll',                   '/qlns/payrolls',               'src/modules/qlns',      NULL, 31, 'fa-solid fa-money-check-dollar'),
    ('QLHT_SALARY_BAND',    'Bậc Lương',                'Salary Bands',              '/qlns/salary-bands',           'src/modules/qlns',      NULL, 31, 'fa-solid fa-ranking-star'),
    ('QLHT_LEAVE',          'Đơn Nghỉ Phép',            'Leave Requests',            '/qlns/leaves',                 'src/modules/qlns',      NULL, 32, 'fa-solid fa-plane-departure'),
    -- Tasks
    ('QLHT_TAG',            'Nhãn Công Việc',           'Task Tags',                 '/task/tags',                   'src/modules/tasks',     'QLHT_CV', 3, 'fa-solid fa-tag'),
    ('QLHT_TICKET_CAT',     'Danh mục Ticket',          'Ticket Categories',         '/task/categories',             'src/modules/tasks',     'QLHT_CV', 4, 'fa-solid fa-folder-tree'),
    -- Email root + subs
    ('QLHT_EMAIL',          'Email Marketing',          'Email Marketing',           '/email/inbox',                 'src/modules/email',     NULL, 40, 'fa-solid fa-envelope-open-text'),
    ('QLHT_EMAIL_INBOX',    'Hộp Thư Đến',              'Inbox',                     '/email/inbox',                 'src/modules/email',     'QLHT_EMAIL', 1, 'fa-solid fa-inbox'),
    ('QLHT_EMAIL_COMPOSE',  'Soạn Email',               'Compose',                   '/email/compose',               'src/modules/email',     'QLHT_EMAIL', 2, 'fa-solid fa-pen-to-square'),
    ('QLHT_EMAIL_TEMPLATE', 'Mẫu Email',                'Email Templates',           '/email/template',              'src/modules/email',     'QLHT_EMAIL', 3, 'fa-solid fa-file-lines'),
    ('QLHT_EMAIL_GROUP',    'Nhóm Nhận',                'Email Groups',              '/email/group',                 'src/modules/email',     'QLHT_EMAIL', 4, 'fa-solid fa-users-line'),
    ('QLHT_EMAIL_CONFIG',   'Cấu Hình Email',           'Email Configs',             '/email/config',                'src/modules/email',     'QLHT_EMAIL', 5, 'fa-solid fa-gear'),
    -- Facebook Automation
    ('QLHT_FB',             'Facebook Automation',      'Facebook Automation',       '/fb',                          'src/modules/fbautomation', NULL, 50, 'fa-brands fa-facebook'),
    ('QLHT_FB_ACCOUNT',     'Tài Khoản FB',             'FB Accounts',               '/fb/accounts',                 'src/modules/fbautomation', 'QLHT_FB', 1, 'fa-solid fa-user-group'),
    ('QLHT_FB_SCAN',        'Quét Nhóm',                'Scan Groups',               '/fb/scan-groups',              'src/modules/fbautomation', 'QLHT_FB', 2, 'fa-solid fa-magnifying-glass'),
    ('QLHT_FB_GROUP',       'Nhóm Đã Lưu',              'Saved Groups',              '/fb/groups',                   'src/modules/fbautomation', 'QLHT_FB', 3, 'fa-solid fa-layer-group'),
    ('QLHT_FB_LEAD',        'Lead Thu Được',            'Leads',                     '/fb/leads',                    'src/modules/fbautomation', 'QLHT_FB', 4, 'fa-solid fa-bullhorn'),
    -- MKT Suite (marketing/CSKH tools) — nhóm mới song song với FB Automation
    ('QLHT_MKT',            'MKT & CSKH',               'Marketing & Support',       '/mkt/inbox',                   'src/modules/fbautomation', NULL, 51, 'fa-solid fa-bullhorn'),
    ('QLHT_MKT_INBOX',      'Inbox khách hàng',         'Customer Inbox',            '/mkt/inbox',                   'src/modules/fbautomation', 'QLHT_MKT',  1, 'fa-solid fa-inbox'),
    ('QLHT_MKT_LEAD_IMPORT','Nhập lead hàng loạt',      'Lead Bulk Import',          '/mkt/leads/import',            'src/modules/fbautomation', 'QLHT_MKT',  2, 'fa-solid fa-file-import'),
    ('QLHT_MKT_CONTENT',    'Lên lịch nội dung',        'Content Scheduler',         '/mkt/content',                 'src/modules/fbautomation', 'QLHT_MKT',  3, 'fa-solid fa-calendar-days'),
    ('QLHT_MKT_AFFILIATE',  'Affiliate / KOL',          'Affiliate & KOL',           '/mkt/affiliate',               'src/modules/fbautomation', 'QLHT_MKT',  4, 'fa-solid fa-link'),
    ('QLHT_MKT_INSIGHTS',   'Insights Fanpage',         'Page Insights',             '/mkt/insights',                'src/modules/fbautomation', 'QLHT_MKT',  5, 'fa-solid fa-chart-line'),
    ('QLHT_MKT_ADS',        'Báo cáo Ads',              'Ads Report',                '/mkt/ads',                     'src/modules/fbautomation', 'QLHT_MKT',  6, 'fa-solid fa-bullseye'),
    ('QLHT_MKT_COMMENTS',   'Kiểm duyệt Comment',       'Comment Moderator',         '/mkt/comments',                'src/modules/fbautomation', 'QLHT_MKT',  7, 'fa-solid fa-shield-halved'),
    ('QLHT_MKT_REVIEWS',    'Theo dõi đánh giá',        'Review Monitor',            '/mkt/reviews',                 'src/modules/fbautomation', 'QLHT_MKT',  8, 'fa-solid fa-star'),
    ('QLHT_MKT_LIVE',       'Livestream Reminder',      'Livestream Reminder',       '/mkt/live',                    'src/modules/fbautomation', 'QLHT_MKT',  9, 'fa-solid fa-video'),
    ('QLHT_MKT_ZALO',       'Zalo OA Broadcast',        'Zalo OA Broadcast',         '/mkt/zalo',                    'src/modules/fbautomation', 'QLHT_MKT', 10, 'fa-solid fa-comment-dots'),
    -- System administration
    ('QLHT_PERMISSION',     'Quản Lý Quyền API',        'API Permissions',           '/qtht/permissions',            'src/modules/qtht',      'QLHT_ROLE', 1, 'fa-solid fa-key'),
    ('QLHT_SECURITY',       'Bảo Mật Hệ Thống',         'Security',                  '/qtht/security',               'src/modules/qtht',      NULL, 90, 'fa-solid fa-lock'),
    ('QLHT_USAGE',          'Sử dụng hệ thống',         'Usage Analytics',           '/qtht/usage',                  'src/modules/qtht',      NULL, 90, 'fa-solid fa-chart-line'),
    ('QLHT_APILOG',         'Nhật Ký API',              'API Log',                   '/qtht/apilogs',                'src/modules/qtht',      NULL, 91, 'fa-solid fa-scroll'),
    ('QLHT_WEBSITE',        'Quản Lý Website',          'Website Management',        '/qtht/website',                'src/modules/qtht',      NULL, 92, 'fa-solid fa-globe'),
    ('QLHT_NEWS',           'Tin Tức Nội Bộ',           'Internal News',             '/qtht/tin-tuc',                'src/modules/qtht',      NULL, 93, 'fa-solid fa-newspaper'),
    -- Workflow Engine — quản lý quy trình duyệt chung cho mọi module
    ('QLHT_WORKFLOW',       'Quy Trình Duyệt',          'Approval Workflows',        '/qtht/workflows',              'src/modules/workflow',  NULL, 94, 'fa-solid fa-diagram-project')
) AS v(code, name, name_en, fe_url, folder_path, parent_code, order_index, icon)
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = v.code
);
