-- Thư viện meme cá nhân — dùng trong bình luận ERP (tính năng phụ vui)
CREATE TABLE IF NOT EXISTS user_meme (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    title           VARCHAR(200),
    file_name       VARCHAR(500),
    file_url        VARCHAR(1000),
    file_size       BIGINT,
    content_type    VARCHAR(100),
    created_by      VARCHAR(50),
    created_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    is_deleted      BOOLEAN DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_user_meme_username ON user_meme (username);
CREATE INDEX IF NOT EXISTS idx_user_meme_username_created ON user_meme (username, created_date DESC);
