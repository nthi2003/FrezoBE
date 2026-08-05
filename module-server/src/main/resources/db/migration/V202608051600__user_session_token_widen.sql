-- Widen JWT columns: access/refresh token với nhiều claims có thể > 500 chars.
-- Flyway may be disabled locally (ddl-auto=update); keep IF EXISTS / idempotent.

ALTER TABLE user_session
    ALTER COLUMN token TYPE VARCHAR(2000);

ALTER TABLE user_session
    ALTER COLUMN refresh_token TYPE VARCHAR(2000);
