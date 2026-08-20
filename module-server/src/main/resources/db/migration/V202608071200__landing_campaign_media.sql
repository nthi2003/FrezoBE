-- Landing campaign / holiday media + loading screen
-- Idempotent — Flyway may be disabled locally (ddl-auto=update).

ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS hero_media_type VARCHAR(20);
ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS hero_video_url VARCHAR(1000);
ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS campaign_name VARCHAR(200);
ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS campaign_start_date DATE;
ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS campaign_end_date DATE;
ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS loading_title VARCHAR(200);
ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS loading_subtitle VARCHAR(500);
ALTER TABLE landing_config ADD COLUMN IF NOT EXISTS loading_image_url VARCHAR(1000);

UPDATE landing_config SET hero_media_type = 'SLIDER' WHERE hero_media_type IS NULL;
