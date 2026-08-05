-- QTHT: system_job + system_job_history — quản trị tác vụ nền (cron cấu hình trong DB)
-- Idempotent — safe to re-run

CREATE TABLE IF NOT EXISTS system_job (
    id               VARCHAR(36)  PRIMARY KEY,
    job_code         VARCHAR(50)  NOT NULL,
    job_name         VARCHAR(200) NOT NULL,
    description      TEXT,
    module_code      VARCHAR(50),
    cron_expression  VARCHAR(100) NOT NULL,
    enabled          BOOLEAN      DEFAULT true,
    last_run_at      TIMESTAMP,
    last_status      VARCHAR(20),
    last_duration_ms BIGINT,
    last_message     TEXT,
    next_run_at      TIMESTAMP,
    is_deleted       BOOLEAN      DEFAULT false,
    created_date     TIMESTAMP,
    created_by       VARCHAR(50),
    updated_date     TIMESTAMP,
    updated_by       VARCHAR(50),
    deleted_at       TIMESTAMP,
    deleted_by       VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_job_code
    ON system_job (job_code) WHERE COALESCE(is_deleted, false) = false;

CREATE TABLE IF NOT EXISTS system_job_history (
    id           VARCHAR(36)  PRIMARY KEY,
    job_code     VARCHAR(50)  NOT NULL,
    started_at   TIMESTAMP    NOT NULL,
    finished_at  TIMESTAMP,
    duration_ms  BIGINT,
    status       VARCHAR(20)  NOT NULL,
    message      TEXT,
    triggered_by VARCHAR(100),
    is_deleted   BOOLEAN      DEFAULT false,
    created_date TIMESTAMP,
    created_by   VARCHAR(50),
    updated_date TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted_at   TIMESTAMP,
    deleted_by   VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_system_job_history_code_started
    ON system_job_history (job_code, started_at DESC);

-- Seed 6 job hiện có. DynamicJobScheduler cũng tự seed khi khởi động,
-- nhưng giữ ở đây để môi trường mới có dữ liệu ngay sau migrate.
INSERT INTO system_job (id, job_code, job_name, description, module_code, cron_expression, enabled,
                        is_deleted, created_date, created_by, updated_date, updated_by)
SELECT gen_random_uuid(), v.job_code, v.job_name, v.description, v.module_code, v.cron_expression, true,
       false, NOW(), 'system', NOW(), 'system'
FROM (VALUES
    ('DB_BACKUP',           'Sao lưu cơ sở dữ liệu',        'Chạy pg_dump toàn bộ database rồi tải file backup lên Google Drive',                'QTHT',          '0 0 12 * * *'),
    ('WEEKLY_REPORT',       'Báo cáo tuần tự động',         'Tổng hợp số liệu tuần và gửi email cho quản trị viên',                               'QTHT',          '0 0 8 * * MON'),
    ('STOCK_ALERT_SCAN',    'Quét cảnh báo tồn kho',        'Quét quy tắc tồn kho tối thiểu và lô hàng cận hạn để sinh cảnh báo và gửi thông báo', 'WAREHOUSE',     '0 0 6 * * *'),
    ('ATTENDANCE_REMINDER', 'Nhắc chấm công',               'Nhắc nhân viên check-in trước giờ vào ca và check-out sau giờ tan ca',               'QLNS',          '0 */5 * * * MON-FRI'),
    ('EMAIL_SEQUENCE',      'Gửi email sequence đến hạn',   'Quét các bước email sequence tới hạn và gửi cho khách hàng',                         'CRM',           '0 0 * * * *'),
    ('SOCIAL_POST_PUBLISH', 'Đăng bài social đã hẹn giờ',   'Quét bài viết trạng thái SCHEDULED tới giờ đăng và publish lên kênh tương ứng',      'FB_AUTOMATION', '0 * * * * *')
) AS v(job_code, job_name, description, module_code, cron_expression)
WHERE NOT EXISTS (
    SELECT 1 FROM system_job sj WHERE sj.job_code = v.job_code
);
