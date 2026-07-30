-- QLNS P1: resignation_request — offboarding wizard
-- Idempotent — safe to re-run

CREATE TABLE IF NOT EXISTS resignation_request (
    id                  VARCHAR(36)  PRIMARY KEY,
    request_code        VARCHAR(50)  NOT NULL,
    person_id           VARCHAR(36)  NOT NULL,
    person_name         VARCHAR(500),
    expected_last_day   DATE,
    actual_last_day     DATE,
    reason              TEXT,
    status              VARCHAR(30)  NOT NULL DEFAULT 'REQUESTED',
    manager_approved_by VARCHAR(100),
    manager_approved_at TIMESTAMP,
    hr_confirmed_by     VARCHAR(100),
    hr_confirmed_at     TIMESTAMP,
    laptop_returned     BOOLEAN      DEFAULT false,
    badge_returned      BOOLEAN      DEFAULT false,
    docs_handed_over    BOOLEAN      DEFAULT false,
    handover_note       TEXT,
    handover_at         TIMESTAMP,
    payroll_settled_at  TIMESTAMP,
    user_revoked_at     TIMESTAMP,
    completed_at        TIMESTAMP,
    is_deleted          BOOLEAN      DEFAULT false,
    created_date        TIMESTAMP,
    created_by          VARCHAR(50),
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,
    deleted_by          VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_resignation_request_code
    ON resignation_request (request_code) WHERE COALESCE(is_deleted, false) = false;

CREATE INDEX IF NOT EXISTS idx_resignation_person
    ON resignation_request (person_id) WHERE COALESCE(is_deleted, false) = false;

CREATE INDEX IF NOT EXISTS idx_resignation_status
    ON resignation_request (status) WHERE COALESCE(is_deleted, false) = false;
