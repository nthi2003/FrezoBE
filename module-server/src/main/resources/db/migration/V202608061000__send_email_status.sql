-- Ghi kết quả gửi thông báo vào send_emails để admin xem được cả lần thất bại (idempotent).
-- `type` để bảng dùng chung cho nhiều kênh (EMAIL hôm nay, SMS/ZALO sau này).
-- Flyway may be disabled locally (ddl-auto=update); keep IF NOT EXISTS.

ALTER TABLE send_emails ADD COLUMN IF NOT EXISTS type VARCHAR(20);
ALTER TABLE send_emails ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE send_emails ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000);

-- Row cũ chỉ được ghi khi gửi xong nên coi như thành công qua kênh email.
UPDATE send_emails SET type = 'EMAIL' WHERE type IS NULL;
UPDATE send_emails SET status = 'SUCCESS' WHERE status IS NULL;

CREATE INDEX IF NOT EXISTS idx_send_emails_created_date ON send_emails (created_date DESC);
CREATE INDEX IF NOT EXISTS idx_send_emails_status ON send_emails (status);
CREATE INDEX IF NOT EXISTS idx_send_emails_type ON send_emails (type);

-- reset_key giờ lưu hash SHA-256 kèm prefix stage ("OTP:"/"TOK:") thay vì OTP thô.
ALTER TABLE users ALTER COLUMN reset_key TYPE VARCHAR(128);
