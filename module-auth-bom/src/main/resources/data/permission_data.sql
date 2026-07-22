-- ============================================================
-- SCRIPT: Permission Catalog (~160 rows)
-- Description: Seed toàn bộ permission theo module × entity × action.
--   Convention:
--     code       = <MODULE>_<ENTITY>_<ACTION>   e.g. QTHT_DEPARTMENT_VIEW
--     api_path   = <module>/<entity>            e.g. qtht/department
--     action     = VIEW | CREATE | UPDATE | DELETE (đôi khi EXPORT/IMPORT/APPROVE)
--     api_method = GET | POST | PUT | DELETE
--   Aspect @CheckPermission match theo (api_path, action).
-- Created: 2026-07-16 (Batch I2)
-- IDEMPOTENT: WHERE NOT EXISTS trên code UNIQUE
-- ============================================================

WITH perm_matrix(module, entity, actions) AS (
    VALUES
        -- ========================= QTHT (Quản trị hệ thống) =========================
        ('qtht', 'user',            ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'role',            ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'menu',            ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'department',      ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'organization',    ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'permission',      ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'role-menu',       ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'category',        ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'setting',         ARRAY['VIEW','UPDATE']),
        ('qtht', 'session',         ARRAY['VIEW','DELETE']),
        ('qtht', 'ip-blacklist',    ARRAY['VIEW','CREATE','DELETE']),
        ('qtht', 'ip-whitelist',    ARRAY['VIEW','CREATE','DELETE']),
        ('qtht', 'ip-trust',        ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtht', 'api-log',         ARRAY['VIEW','DELETE']),
        ('qtht', 'audit-log',       ARRAY['VIEW']),
        ('qtht', 'notification',    ARRAY['VIEW','UPDATE']),
        ('qtht', 'websocket',       ARRAY['VIEW','CREATE']),
        ('qtht', 'dashboard',       ARRAY['VIEW','EXPORT']),
        ('qtht', 'system',          ARRAY['VIEW','CREATE']),
        -- ========================= QLNS (Nhân sự) =========================
        ('qlns', 'person',          ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qlns', 'contract',        ARRAY['VIEW','CREATE','UPDATE','DELETE','APPROVE']),
        ('qlns', 'contract-template', ARRAY['VIEW','CREATE','DELETE']),
        ('qlns', 'attendance',      ARRAY['VIEW','CREATE','UPDATE']),
        ('qlns', 'leave',           ARRAY['VIEW','CREATE','APPROVE']),
        ('qlns', 'leave-request',   ARRAY['VIEW','CREATE','APPROVE']),
        ('qlns', 'payroll',         ARRAY['VIEW','CREATE','UPDATE','APPROVE']),
        ('qlns', 'person-document', ARRAY['VIEW','CREATE','DELETE']),
        -- ========================= CUSTOMER (CRM) =========================
        ('customer', 'customer',    ARRAY['VIEW','CREATE','UPDATE','DELETE','EXPORT','IMPORT']),
        ('customer', 'ncc',         ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('customer', 'voucher',     ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        -- ========================= PRODUCT =========================
        ('product',  'product',     ARRAY['VIEW','CREATE','UPDATE','DELETE','IMPORT']),
        ('product',  'product-category', ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('product',  'order',       ARRAY['VIEW','CREATE','UPDATE','DELETE','APPROVE']),
        ('product',  'cart',        ARRAY['VIEW','CREATE','DELETE']),
        ('product',  'dashboard',   ARRAY['VIEW']),
        -- ========================= WAREHOUSE =========================
        ('warehouse', 'warehouse',   ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('warehouse', 'stock-balance', ARRAY['VIEW']),
        ('warehouse', 'grn',         ARRAY['VIEW','CREATE','UPDATE','DELETE','APPROVE']),
        ('warehouse', 'gin',         ARRAY['VIEW','CREATE','UPDATE','DELETE','APPROVE']),
        ('warehouse', 'stock-check', ARRAY['VIEW','CREATE','APPROVE']),
        -- ========================= QTBV (Quản trị bán vựng — CMS) =========================
        -- Legacy singular 'article' kept for idempotent reseed of old DBs;
        -- SA-ART-002 adds /qtbv/articles (plural + leading slash) in block below.
        ('qtbv', 'article',         ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtbv', 'banner',          ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('qtbv', 'landing-config',  ARRAY['VIEW','UPDATE']),
        ('qtbv', 'event',           ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        -- ========================= EMAIL =========================
        ('email', 'template',       ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('email', 'group',          ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('email', 'config',         ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('email', 'inbox',          ARRAY['VIEW','UPDATE']),
        ('email', 'send',           ARRAY['CREATE']),
        -- ========================= TASK =========================
        ('task', 'task',            ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('task', 'ticket',          ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('task', 'tag',             ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        -- ========================= FACEBOOK AUTOMATION =========================
        ('fb', 'account',           ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('fb', 'group',             ARRAY['VIEW','DELETE']),
        ('fb', 'lead',              ARRAY['VIEW','DELETE','IMPORT']),
        ('fb', 'automation',        ARRAY['VIEW','CREATE']),
        -- ========================= CMS =========================
        ('cms', 'voucher',          ARRAY['VIEW','CREATE','UPDATE']),
        ('cms', 'order',            ARRAY['VIEW','UPDATE']),
        ('cms', 'customer',         ARRAY['VIEW','EXPORT']),
        -- ========================= DMDC (Danh mục dùng chung) =========================
        ('dmdc', 'issuer',          ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('dmdc', 'signer',          ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('dmdc', 'title',           ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('dmdc', 'location',        ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        ('dmdc', 'industry',        ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        -- ========================= QLTS (Tài sản) =========================
        ('qlts', 'asset',           ARRAY['VIEW','CREATE','UPDATE','DELETE']),
        -- ========================= AI =========================
        ('ai',   'doc',             ARRAY['VIEW','CREATE'])
),
expanded AS (
    SELECT
        UPPER(REPLACE(module, '-', '_')) || '_' || UPPER(REPLACE(entity, '-', '_')) || '_' || act AS code,
        UPPER(REPLACE(entity, '-', ' ')) || ' - ' || act AS name,
        CASE act
            WHEN 'VIEW'    THEN 'GET'
            WHEN 'CREATE'  THEN 'POST'
            WHEN 'UPDATE'  THEN 'PUT'
            WHEN 'DELETE'  THEN 'DELETE'
            WHEN 'APPROVE' THEN 'PUT'
            WHEN 'EXPORT'  THEN 'GET'
            WHEN 'IMPORT'  THEN 'POST'
            ELSE 'GET'
        END AS api_method,
        module || '/' || entity AS api_path,
        act AS action,
        'QTHT' AS app_code
    FROM perm_matrix, UNNEST(actions) AS act
)
INSERT INTO permission (id, code, name, api_method, api_path, action, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    e.code,
    e.name,
    e.api_method,
    e.api_path,
    e.action,
    e.app_code,
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM expanded e
WHERE NOT EXISTS (
    SELECT 1 FROM permission p WHERE p.code = e.code
);

-- ============================================================
-- S1 2026-07-21: accounting / approval / workflow
-- api_path có leading slash — khớp @CheckPermission(api=...) trên controller
-- Codes: ACCOUNTING_*, APPROVALS_*, APPROVAL_FLOWS_*, WF_*, WORKFLOWS_*
-- BA: map role_permission theo menu FE/Mobile (SUPER_ADMIN bypass qua isAdmin)
-- ============================================================
WITH s1_perms(code, name, api_method, api_path, action) AS (
    VALUES
        -- Accounting
        ('ACCOUNTING_ACCOUNTS_VIEW',          'Accounts - VIEW',          'GET',    '/accounting/accounts',         'VIEW'),
        ('ACCOUNTING_ACCOUNTS_CREATE',        'Accounts - CREATE',        'POST',   '/accounting/accounts',         'CREATE'),
        ('ACCOUNTING_ACCOUNTS_UPDATE',        'Accounts - UPDATE',        'PUT',    '/accounting/accounts',         'UPDATE'),
        ('ACCOUNTING_ACCOUNTS_DELETE',        'Accounts - DELETE',        'DELETE', '/accounting/accounts',         'DELETE'),
        ('ACCOUNTING_JOURNALS_VIEW',          'Journals - VIEW',          'GET',    '/accounting/journals',         'VIEW'),
        ('ACCOUNTING_JOURNALS_CREATE',        'Journals - CREATE',        'POST',   '/accounting/journals',         'CREATE'),
        ('ACCOUNTING_JOURNALS_UPDATE',        'Journals - UPDATE',        'PUT',    '/accounting/journals',         'UPDATE'),
        ('ACCOUNTING_PERIODS_VIEW',           'Periods - VIEW',           'GET',    '/accounting/periods',          'VIEW'),
        ('ACCOUNTING_PERIODS_CREATE',         'Periods - CREATE',         'POST',   '/accounting/periods',          'CREATE'),
        ('ACCOUNTING_PERIODS_UPDATE',         'Periods - UPDATE',         'PUT',    '/accounting/periods',          'UPDATE'),
        ('ACCOUNTING_GL_VIEW',                'GL - VIEW',                'GET',    '/accounting/gl',               'VIEW'),
        ('ACCOUNTING_REPORTS_VIEW',           'Reports - VIEW',           'GET',    '/accounting/reports',          'VIEW'),
        ('ACCOUNTING_BANK_STATEMENTS_VIEW',   'Bank Statements - VIEW',   'GET',    '/accounting/bank-statements',  'VIEW'),
        ('ACCOUNTING_BANK_STATEMENTS_CREATE', 'Bank Statements - CREATE', 'POST',   '/accounting/bank-statements',  'CREATE'),
        ('ACCOUNTING_BANK_STATEMENTS_UPDATE', 'Bank Statements - UPDATE', 'PUT',    '/accounting/bank-statements',  'UPDATE'),
        ('ACCOUNTING_SETTING_VIEW',           'Setting - VIEW',           'GET',    '/accounting/setting',          'VIEW'),
        ('ACCOUNTING_SETTING_UPDATE',         'Setting - UPDATE',         'PUT',    '/accounting/setting',          'UPDATE'),
        ('ACCOUNTING_TAX_VIEW',               'Tax - VIEW',               'GET',    '/accounting/tax',              'VIEW'),
        -- Approvals
        ('APPROVALS_VIEW',                    'Approvals - VIEW',         'GET',    '/approvals',                   'VIEW'),
        ('APPROVALS_CREATE',                  'Approvals - CREATE',       'POST',   '/approvals',                   'CREATE'),
        ('APPROVALS_APPROVE',                 'Approvals - APPROVE',      'PUT',    '/approvals',                   'APPROVE'),
        ('APPROVAL_FLOWS_VIEW',               'Approval Flows - VIEW',    'GET',    '/approval-flows',              'VIEW'),
        ('APPROVAL_FLOWS_CREATE',             'Approval Flows - CREATE',  'POST',   '/approval-flows',              'CREATE'),
        ('APPROVAL_FLOWS_UPDATE',             'Approval Flows - UPDATE',  'PUT',    '/approval-flows',              'UPDATE'),
        -- Workflow engine (/wf)
        ('WF_DEFINITIONS_VIEW',               'WF Definitions - VIEW',    'GET',    '/wf/definitions',              'VIEW'),
        ('WF_DEFINITIONS_CREATE',             'WF Definitions - CREATE',  'POST',   '/wf/definitions',              'CREATE'),
        ('WF_DEFINITIONS_DELETE',             'WF Definitions - DELETE',  'DELETE', '/wf/definitions',              'DELETE'),
        ('WF_INSTANCES_VIEW',                 'WF Instances - VIEW',      'GET',    '/wf/instances',                'VIEW'),
        ('WF_INSTANCES_UPDATE',               'WF Instances - UPDATE',    'PUT',    '/wf/instances',                'UPDATE'),
        ('WF_TASKS_VIEW',                     'WF Tasks - VIEW',          'GET',    '/wf/tasks',                    'VIEW'),
        ('WF_TASKS_APPROVE',                  'WF Tasks - APPROVE',       'PUT',    '/wf/tasks',                    'APPROVE'),
        -- Visual workflow (/workflows)
        ('WORKFLOWS_TEMPLATES_VIEW',          'WF Templates - VIEW',      'GET',    '/workflows/templates',         'VIEW'),
        ('WORKFLOWS_TEMPLATES_CREATE',        'WF Templates - CREATE',    'POST',   '/workflows/templates',         'CREATE'),
        ('WORKFLOWS_DEFINITIONS_VIEW',        'WF Visual Def - VIEW',     'GET',    '/workflows/definitions',       'VIEW'),
        ('WORKFLOWS_DEFINITIONS_UPDATE',      'WF Visual Def - UPDATE',   'PUT',    '/workflows/definitions',       'UPDATE')
)
INSERT INTO permission (id, code, name, api_method, api_path, action, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    s.code,
    s.name,
    s.api_method,
    s.api_path,
    s.action,
    'QTHT',
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM s1_perms s
WHERE NOT EXISTS (
    SELECT 1 FROM permission p WHERE p.code = s.code
);

-- ============================================================
-- CYCLE-QTLV-ART / SA-ART-002: QTBV articles — align api_path với @CheckPermission
-- Codes FE: QTBV.ARTICLES.* → QTBV_ARTICLES_*
-- Soft-delete legacy singular qtbv/article (QTBV_ARTICLE_*)
-- ============================================================
UPDATE permission
SET is_deleted = true,
    updated_date = NOW(),
    updated_by = 'system'
WHERE code IN (
    'QTBV_ARTICLE_VIEW',
    'QTBV_ARTICLE_CREATE',
    'QTBV_ARTICLE_UPDATE',
    'QTBV_ARTICLE_DELETE'
)
  AND (is_deleted = false OR is_deleted IS NULL);

WITH qtbv_art_perms(code, name, api_method, api_path, action) AS (
    VALUES
        ('QTBV_ARTICLES_VIEW',              'Articles - VIEW',              'GET',    '/qtbv/articles',                  'VIEW'),
        ('QTBV_ARTICLES_CREATE',            'Articles - CREATE',            'POST',   '/qtbv/articles',                  'CREATE'),
        ('QTBV_ARTICLES_UPDATE',            'Articles - UPDATE',            'PUT',    '/qtbv/articles',                  'UPDATE'),
        ('QTBV_ARTICLES_DELETE',            'Articles - DELETE',            'DELETE', '/qtbv/articles',                  'DELETE'),
        ('QTBV_ARTICLES_FILTER_VIEW',       'Articles Filter - VIEW',       'GET',    '/qtbv/articles/filter',           'VIEW'),
        ('QTBV_ARTICLES_SUBMIT_UPDATE',     'Articles Submit - UPDATE',     'PUT',    '/qtbv/articles/submit',           'UPDATE'),
        ('QTBV_ARTICLES_PUBLISH_UPDATE',    'Articles Publish - UPDATE',    'PUT',    '/qtbv/articles/publish',          'UPDATE'),
        ('QTBV_ARTICLES_REVIEW_UPDATE',     'Articles Review - UPDATE',     'PUT',    '/qtbv/articles/review',           'UPDATE'),
        ('QTBV_ARTICLES_MY_DRAFTS_VIEW',    'Articles My Drafts - VIEW',    'GET',    '/qtbv/articles/my-drafts',        'VIEW'),
        ('QTBV_ARTICLES_PENDING_VIEW',      'Articles Pending - VIEW',      'GET',    '/qtbv/articles/pending-approval', 'VIEW'),
        ('QTBV_ARTICLES_PUBLISHED_VIEW',    'Articles Published - VIEW',    'GET',    '/qtbv/articles/published',        'VIEW'),
        ('QTBV_MANAGERS_VIEW',              'QTBV Managers - VIEW',         'GET',    '/qtbv/managers',                  'VIEW'),
        ('QTBV_ORGANIZATIONS_VIEW',         'QTBV Organizations - VIEW',    'GET',    '/qtbv/organizations',             'VIEW')
)
INSERT INTO permission (id, code, name, api_method, api_path, action, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    s.code,
    s.name,
    s.api_method,
    s.api_path,
    s.action,
    'QTHT',
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM qtbv_art_perms s
WHERE NOT EXISTS (
    SELECT 1 FROM permission p WHERE p.code = s.code
);
