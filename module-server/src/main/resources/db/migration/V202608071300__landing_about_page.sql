-- Trang /about (Về chúng tôi) + khối câu chuyện trên trang chủ
ALTER TABLE landing_config
    ADD COLUMN IF NOT EXISTS about_title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS about_subtitle VARCHAR(500),
    ADD COLUMN IF NOT EXISTS about_image_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS story_title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS story_subtitle VARCHAR(500),
    ADD COLUMN IF NOT EXISTS contact_title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS contact_subtitle VARCHAR(500);
