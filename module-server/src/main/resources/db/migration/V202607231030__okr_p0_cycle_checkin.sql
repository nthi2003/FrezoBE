-- OKR P0: cycle/dept/org/level + check-in history
-- Flyway currently disabled locally (ddl-auto=update); keep idempotent for future enable.

ALTER TABLE okr ADD COLUMN IF NOT EXISTS cycle_id varchar(36);
ALTER TABLE okr ADD COLUMN IF NOT EXISTS department_id varchar(36);
ALTER TABLE okr ADD COLUMN IF NOT EXISTS org_id varchar(36);
ALTER TABLE okr ADD COLUMN IF NOT EXISTS parent_okr_id varchar(36);
ALTER TABLE okr ADD COLUMN IF NOT EXISTS level varchar(20);

CREATE INDEX IF NOT EXISTS idx_okr_cycle ON okr (cycle_id);
CREATE INDEX IF NOT EXISTS idx_okr_dept ON okr (department_id);

CREATE TABLE IF NOT EXISTS okr_check_in (
    id              varchar(36)  NOT NULL PRIMARY KEY,
    okr_id          varchar(36)  NOT NULL,
    key_result_id   varchar(36),
    previous_value  double precision,
    current_value   double precision,
    note            varchar(2000),
    checked_by      varchar(100),
    created_by      varchar(50),
    created_date    timestamp,
    updated_by      varchar(50),
    updated_date    timestamp,
    is_deleted      boolean DEFAULT false,
    deleted_at      timestamp,
    deleted_by      varchar(50)
);

CREATE INDEX IF NOT EXISTS idx_okr_checkin_okr ON okr_check_in (okr_id);
