-- Module Hồ sơ nhân sự — job position, payroll component, work history
-- Idempotent — Flyway may be disabled locally (ddl-auto=update).

CREATE TABLE IF NOT EXISTS hr_job_position (
    id              VARCHAR(36) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    rank_code       VARCHAR(50) NOT NULL,
    title_code      VARCHAR(50) NOT NULL,
    activated       BOOLEAN NOT NULL DEFAULT TRUE,
    order_index     INTEGER,
    is_deleted      BOOLEAN DEFAULT FALSE,
    created_date    TIMESTAMP,
    created_by      VARCHAR(255),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_hr_job_pos_rank ON hr_job_position (rank_code);
CREATE INDEX IF NOT EXISTS idx_hr_job_pos_title ON hr_job_position (title_code);

CREATE TABLE IF NOT EXISTS hr_payroll_component (
    id              VARCHAR(36) PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    nature          VARCHAR(20) NOT NULL,
    taxable_type    VARCHAR(20),
    tax_deductible  BOOLEAN,
    quota_type      VARCHAR(20),
    quota_value     NUMERIC(18, 4),
    default_value   NUMERIC(18, 2),
    activated       BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN DEFAULT FALSE,
    created_date    TIMESTAMP,
    created_by      VARCHAR(255),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(255)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_hr_pay_comp_code ON hr_payroll_component (code);

CREATE TABLE IF NOT EXISTS hr_person_work_history (
    id              VARCHAR(36) PRIMARY KEY,
    person_id       VARCHAR(36) NOT NULL,
    from_date       DATE,
    to_date         DATE,
    department_name VARCHAR(255),
    position_name   VARCHAR(255),
    job_position_id VARCHAR(36),
    note            VARCHAR(2000),
    is_deleted      BOOLEAN DEFAULT FALSE,
    created_date    TIMESTAMP,
    created_by      VARCHAR(255),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_hr_work_hist_person ON hr_person_work_history (person_id);
