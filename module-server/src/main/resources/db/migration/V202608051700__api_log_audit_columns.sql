-- Extend api_log for system-wide API audit (idempotent).
-- Flyway may be disabled locally (ddl-auto=update); keep IF NOT EXISTS.

ALTER TABLE api_log ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);
ALTER TABLE api_log ADD COLUMN IF NOT EXISTS query_string VARCHAR(2000);
ALTER TABLE api_log ADD COLUMN IF NOT EXISTS module VARCHAR(64);
ALTER TABLE api_log ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000);
ALTER TABLE api_log ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_api_log_created_date ON api_log (created_date DESC);
CREATE INDEX IF NOT EXISTS idx_api_log_username ON api_log (username);
CREATE INDEX IF NOT EXISTS idx_api_log_status_code ON api_log (status_code);
CREATE INDEX IF NOT EXISTS idx_api_log_uri ON api_log (uri);
CREATE INDEX IF NOT EXISTS idx_api_log_module ON api_log (module);
CREATE INDEX IF NOT EXISTS idx_api_log_eff_from ON api_log (eff_from DESC);
