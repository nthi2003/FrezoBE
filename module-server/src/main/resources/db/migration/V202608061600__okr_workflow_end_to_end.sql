-- OKR end-to-end workflow: settings, timeline, feedback, plans and 1:1 check-ins.
-- Idempotent because Flyway is disabled in some local profiles.

ALTER TABLE okr ADD COLUMN IF NOT EXISTS objective_type varchar(20) DEFAULT 'COMMITTED';
ALTER TABLE okr ADD COLUMN IF NOT EXISTS scope_type varchar(20) DEFAULT 'PERSONAL';
ALTER TABLE okr ADD COLUMN IF NOT EXISTS cross_link_ids text;
ALTER TABLE okr ADD COLUMN IF NOT EXISTS published boolean NOT NULL DEFAULT false;
ALTER TABLE okr ADD COLUMN IF NOT EXISTS published_at timestamp;
ALTER TABLE okr ADD COLUMN IF NOT EXISTS published_by varchar(100);

CREATE TABLE IF NOT EXISTS okr_timeline_step (
    id varchar(36) NOT NULL PRIMARY KEY,
    step_name varchar(255) NOT NULL,
    department_name varchar(255),
    time_label varchar(255),
    detail varchar(2000),
    result varchar(2000),
    sort_order integer DEFAULT 0,
    created_by varchar(50), created_date timestamp, updated_by varchar(50), updated_date timestamp,
    is_deleted boolean DEFAULT false, deleted_at timestamp, deleted_by varchar(50)
);

CREATE TABLE IF NOT EXISTS okr_feedback_type (
    id varchar(36) NOT NULL PRIMARY KEY,
    name varchar(255) NOT NULL,
    created_by varchar(50), created_date timestamp, updated_by varchar(50), updated_date timestamp,
    is_deleted boolean DEFAULT false, deleted_at timestamp, deleted_by varchar(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_okr_feedback_type_name
    ON okr_feedback_type (lower(name)) WHERE is_deleted=false;

CREATE TABLE IF NOT EXISTS okr_feedback (
    id varchar(36) NOT NULL PRIMARY KEY,
    objective_id varchar(36),
    target_scope varchar(20) NOT NULL,
    target_department_id varchar(36),
    feedback_type_id varchar(36) NOT NULL,
    content varchar(4000) NOT NULL,
    sender_person_id varchar(36) NOT NULL,
    created_by varchar(50), created_date timestamp, updated_by varchar(50), updated_date timestamp,
    is_deleted boolean DEFAULT false, deleted_at timestamp, deleted_by varchar(50)
);
CREATE INDEX IF NOT EXISTS idx_okr_feedback_target ON okr_feedback (target_scope, target_department_id);

CREATE TABLE IF NOT EXISTS okr_action (
    id varchar(36) NOT NULL PRIMARY KEY,
    key_result_id varchar(36) NOT NULL,
    title varchar(500) NOT NULL,
    plan_url varchar(2000),
    start_date date,
    end_date date,
    result varchar(2000),
    status varchar(20) NOT NULL DEFAULT 'TODO',
    related_person_ids text,
    created_by varchar(50), created_date timestamp, updated_by varchar(50), updated_date timestamp,
    is_deleted boolean DEFAULT false, deleted_at timestamp, deleted_by varchar(50)
);
CREATE INDEX IF NOT EXISTS idx_okr_action_kr ON okr_action (key_result_id);

CREATE TABLE IF NOT EXISTS okr_checkin_session (
    id varchar(36) NOT NULL PRIMARY KEY,
    okr_id varchar(36) NOT NULL,
    employee_person_id varchar(36) NOT NULL,
    manager_person_id varchar(36) NOT NULL,
    progress varchar(4000),
    delayed_work varchar(4000),
    blockers varchar(4000),
    solutions varchar(4000),
    confidence_level integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    official_update varchar(4000),
    manager_feedback varchar(4000),
    next_check_in_date date,
    complete_okrs boolean NOT NULL DEFAULT false,
    confirmed_at timestamp,
    created_by varchar(50), created_date timestamp, updated_by varchar(50), updated_date timestamp,
    is_deleted boolean DEFAULT false, deleted_at timestamp, deleted_by varchar(50)
);
CREATE INDEX IF NOT EXISTS idx_okr_checkin_session_okr ON okr_checkin_session (okr_id, created_date);
CREATE INDEX IF NOT EXISTS idx_okr_checkin_manager ON okr_checkin_session (manager_person_id, status);

CREATE TABLE IF NOT EXISTS okr_checkin_feedback (
    id varchar(36) NOT NULL PRIMARY KEY,
    checkin_id varchar(36) NOT NULL,
    parent_feedback_id varchar(36),
    author_person_id varchar(36) NOT NULL,
    content varchar(4000) NOT NULL,
    created_by varchar(50), created_date timestamp, updated_by varchar(50), updated_date timestamp,
    is_deleted boolean DEFAULT false, deleted_at timestamp, deleted_by varchar(50)
);
CREATE INDEX IF NOT EXISTS idx_okr_checkin_feedback_session ON okr_checkin_feedback (checkin_id, created_date);

WITH perms(code, name, api_method, api_path, action) AS (
    VALUES
        ('QLNS_OKR_WORKFLOW_VIEW', 'OKR workflow - VIEW', 'GET', '/qlns/okr-workflow/**', 'VIEW'),
        ('QLNS_OKR_WORKFLOW_CREATE', 'OKR workflow - CREATE', 'POST', '/qlns/okr-workflow/**', 'CREATE'),
        ('QLNS_OKR_WORKFLOW_UPDATE', 'OKR workflow - UPDATE', 'PUT', '/qlns/okr-workflow/**', 'UPDATE'),
        ('QLNS_OKR_WORKFLOW_DELETE', 'OKR workflow - DELETE', 'DELETE', '/qlns/okr-workflow/**', 'DELETE')
)
INSERT INTO permission
    (id, code, name, api_method, api_path, action, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), p.code, p.name, p.api_method, p.api_path, p.action, 'QLNS', false,
       NOW(), 'system', NOW(), 'system'
FROM perms p
WHERE NOT EXISTS (SELECT 1 FROM permission x WHERE x.code=p.code);

INSERT INTO role_permission
    (id, role_id, permission_id, app_code, is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), r.id, p.id, 'QLNS', false, NOW(), 'system', NOW(), 'system'
FROM roles r CROSS JOIN permission p
WHERE r.code='ADMIN'
  AND (r.is_deleted=false OR r.is_deleted IS NULL)
  AND p.code IN ('QLNS_OKR_WORKFLOW_VIEW','QLNS_OKR_WORKFLOW_CREATE','QLNS_OKR_WORKFLOW_UPDATE','QLNS_OKR_WORKFLOW_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_id=r.id AND rp.permission_id=p.id AND (rp.is_deleted=false OR rp.is_deleted IS NULL)
  );

INSERT INTO menu (
    id, code, name, name_en, app_code, fe_url, folder_path, parent_code, order_index,
    menu_type, icon, is_public, status, is_deleted, created_date, created_by, updated_date, updated_by
)
SELECT gen_random_uuid(), 'QLNS_OKR_HUB', 'OKRs', 'OKRs', 'QLNS', '/qlns/performance?tab=okrs',
       'src/modules/qlns', 'MENU_QLNS', 35, 1, 'Target', true, true, false,
       NOW(), 'system', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE code='QLNS_OKR_HUB' AND app_code='QLNS');
