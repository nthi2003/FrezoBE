-- FR-DOC-03: Guide CMS entity + permission seed (idempotent)
-- Flyway may be disabled locally (ddl-auto=update); keep IF NOT EXISTS / NOT EXISTS.

CREATE TABLE IF NOT EXISTS guide (
    id              varchar(36)  NOT NULL PRIMARY KEY,
    slug            varchar(120) NOT NULL,
    title           varchar(255) NOT NULL,
    body            text         NOT NULL,
    module          varchar(100),
    summary         varchar(500),
    sort_order      integer      DEFAULT 0,
    published       boolean      NOT NULL DEFAULT false,
    created_by      varchar(50),
    created_date    timestamp,
    updated_by      varchar(50),
    updated_date    timestamp,
    is_deleted      boolean      DEFAULT false,
    deleted_at      timestamp,
    deleted_by      varchar(50),
    CONSTRAINT uk_guide_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS idx_guide_published_sort
    ON guide (published, sort_order)
    WHERE is_deleted = false OR is_deleted IS NULL;

-- Permissions (leading slash — khớp @CheckPermission)
WITH guide_perms(code, name, api_method, api_path, action) AS (
    VALUES
        ('QTHT_GUIDES_VIEW',   'Guides - VIEW',   'GET',    '/qtht/guides', 'VIEW'),
        ('QTHT_GUIDES_CREATE', 'Guides - CREATE', 'POST',   '/qtht/guides', 'CREATE'),
        ('QTHT_GUIDES_UPDATE', 'Guides - UPDATE', 'PUT',    '/qtht/guides', 'UPDATE'),
        ('QTHT_GUIDES_DELETE', 'Guides - DELETE', 'DELETE', '/qtht/guides', 'DELETE')
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
FROM guide_perms s
WHERE NOT EXISTS (
    SELECT 1 FROM permission p WHERE p.code = s.code
);

-- ADMIN role → full guide perms
INSERT INTO role_permission (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT
    gen_random_uuid(),
    r.id,
    p.id,
    'QTHT',
    false,
    NOW(), 'system',
    NOW(), 'system'
FROM roles r
CROSS JOIN permission p
WHERE r.code = 'ADMIN'
  AND r.app_code = 'QTHT'
  AND (r.is_deleted = false OR r.is_deleted IS NULL)
  AND p.code IN ('QTHT_GUIDES_VIEW', 'QTHT_GUIDES_CREATE', 'QTHT_GUIDES_UPDATE', 'QTHT_GUIDES_DELETE')
  AND (p.is_deleted = false OR p.is_deleted IS NULL)
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
        AND (rp.is_deleted = false OR rp.is_deleted IS NULL)
  );

-- Menu: Quản lý hướng dẫn (Admin)
INSERT INTO menu (
    id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index,
    menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by
)
SELECT
    gen_random_uuid(),
    'QLHT_GUIDE',
    'Quản lý hướng dẫn',
    'Guide CMS',
    'QTHT',
    '/admin/guides',
    'src/modules/docs',
    'MENU_QTHT',
    18,
    1,
    'BookOpen',
    true,
    true,
    false,
    NOW(), 'system',
    NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM menu m WHERE m.app_code = 'QTHT' AND m.code = 'QLHT_GUIDE'
);

UPDATE menu
SET parent_code = 'MENU_QTHT',
    order_index = 18,
    fe_url = '/admin/guides',
    folder_path = 'src/modules/docs',
    is_deleted = false,
    status = true,
    updated_date = NOW(),
    updated_by = 'system'
WHERE app_code = 'QTHT' AND code = 'QLHT_GUIDE';
