-- News module: articles extensions, banners pin flag, news_categories/mottos/pins tables
-- Idempotent — Flyway may be disabled locally (ddl-auto=update).

-- articles: category, content type, external link, display toggle
ALTER TABLE articles ADD COLUMN IF NOT EXISTS category_id VARCHAR(50);
ALTER TABLE articles ADD COLUMN IF NOT EXISTS content_type VARCHAR(20);
ALTER TABLE articles ADD COLUMN IF NOT EXISTS external_url VARCHAR(1000);
ALTER TABLE articles ADD COLUMN IF NOT EXISTS display_on_news BOOLEAN;

UPDATE articles SET display_on_news = true WHERE display_on_news IS NULL;
ALTER TABLE articles ALTER COLUMN display_on_news SET DEFAULT true;
UPDATE articles SET content_type = 'ARTICLE' WHERE content_type IS NULL;

-- banners: pin for /bai-viet carousel
ALTER TABLE banners ADD COLUMN IF NOT EXISTS pin_for_news_page BOOLEAN;
UPDATE banners SET pin_for_news_page = false WHERE pin_for_news_page IS NULL;
ALTER TABLE banners ALTER COLUMN pin_for_news_page SET DEFAULT false;

-- news categories
CREATE TABLE IF NOT EXISTS news_categories (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(20)  NOT NULL DEFAULT '#16a34a',
    organization_id VARCHAR(50),
    order_index     INTEGER      DEFAULT 0,
    created_by      VARCHAR(50),
    created_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    is_deleted      BOOLEAN      DEFAULT false,
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_news_categories_org
    ON news_categories (organization_id, order_index)
    WHERE is_deleted = false OR is_deleted IS NULL;

-- news mottos (châm ngôn)
CREATE TABLE IF NOT EXISTS news_mottos (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    content         TEXT         NOT NULL,
    author          VARCHAR(200),
    created_by      VARCHAR(50),
    created_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    is_deleted      BOOLEAN      DEFAULT false,
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

-- article pins (ghim tin theo đơn vị, max 5 enforced in service)
CREATE TABLE IF NOT EXISTS article_pins (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    article_id      VARCHAR(50)  NOT NULL,
    organization_id VARCHAR(50)  NOT NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_by      VARCHAR(50),
    created_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    is_deleted      BOOLEAN      DEFAULT false,
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50),
    CONSTRAINT uk_article_pin_org_article UNIQUE (organization_id, article_id)
);

CREATE INDEX IF NOT EXISTS idx_article_pins_org
    ON article_pins (organization_id, sort_order)
    WHERE is_deleted = false OR is_deleted IS NULL;
